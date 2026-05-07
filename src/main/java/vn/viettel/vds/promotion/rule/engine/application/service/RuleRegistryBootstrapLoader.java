package vn.viettel.vds.promotion.rule.engine.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.in.RegisterDrlUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.AssignmentRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleRegistryPort;

import java.util.*;

/**
 * Repopulates the in-memory rule registry from {@code assignments} + {@code bundles}
 * at service startup.
 *
 * <p>{@link vn.viettel.vds.promotion.rule.engine.adapter.out.registry.InMemoryRuleRegistryAdapter}
 * holds compiled DRL bindings in a {@link java.util.concurrent.ConcurrentHashMap}, which is
 * lost on every restart. The compile/deploy saga ({@code pp-validation} → POST
 * {@code /v1/compiler/compile}) only persists the bundle row; it never calls
 * {@code POST /v1/rules}, so a freshly-restarted engine evaluates every {@code ruleId}
 * as {@code RULE_NOT_REGISTERED} until the operator manually re-registers each one.
 *
 * <p>This bootstrap closes the gap by walking every active assignment, looking up the
 * referenced bundle's DRL content, and pushing the {@code (ruleId, drl)} pair through
 * {@link RegisterDrlUseCase#register} — the same path POST {@code /v1/rules} takes,
 * which means the incremental KieBase update fires for each rule and Drools is ready
 * to evaluate immediately after startup.
 *
 * <p>Failures on individual rules are logged and skipped; one bad bundle should not
 * prevent the rest of the registry from loading.
 *
 * <p>Runs after Spring's {@code DataSourceInitializerInvoker} (Liquibase) — JPA repos are
 * already wired by the time {@link ApplicationRunner} fires.
 */
@Component
@Order(100) // run after Liquibase / DataSource init
public class RuleRegistryBootstrapLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RuleRegistryBootstrapLoader.class);

    private final AssignmentRepositoryPort assignmentRepo;
    private final BundleRepositoryPort bundleRepo;
    private final RegisterDrlUseCase registerDrlUseCase;
    private final RuleRegistryPort ruleRegistry;

    public RuleRegistryBootstrapLoader(AssignmentRepositoryPort assignmentRepo,
                                       BundleRepositoryPort bundleRepo,
                                       RegisterDrlUseCase registerDrlUseCase,
                                       RuleRegistryPort ruleRegistry) {
        this.assignmentRepo = assignmentRepo;
        this.bundleRepo = bundleRepo;
        this.registerDrlUseCase = registerDrlUseCase;
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        long started = System.currentTimeMillis();
        List<AssignmentEntity> assignments = assignmentRepo.findAllActive();
        log.info("[RegistryBootstrap] Found {} active assignments — rebuilding rule registry", assignments.size());

        // Cache DRL by bundleHash so we don't hit the bundles table once per ruleId
        // when multiple assignments share the same bundle.
        Map<String, String> drlByHash = new HashMap<>();
        Set<String> registered = new HashSet<>();
        int ok = 0;
        int skipped = 0;
        int failed = 0;

        BootstrapCounters counters = new BootstrapCounters();
        for (AssignmentEntity a : assignments) {
            registerAssignment(a, drlByHash, registered, counters);
        }
        ok = counters.ok;
        skipped = counters.skipped;
        failed = counters.failed;

        log.info("[RegistryBootstrap] Done in {} ms — registered={}, skipped={}, failed={}",
                System.currentTimeMillis() - started, ok, skipped, failed);
    }

    private void registerAssignment(AssignmentEntity a, Map<String, String> drlByHash,
                                    Set<String> registered, BootstrapCounters counters) {
        String ruleId = a.getRuleId();
        String bundleHash = a.getBundleHash();

        if (ruleId == null || bundleHash == null) {
            counters.skipped++;
            return;
        }
        if (!registered.add(ruleId)) {
            return;
        }
        if (ruleRegistry.findById(ruleId).isPresent()) {
            return;
        }

        String drl = drlByHash.computeIfAbsent(bundleHash, this::loadDrl);
        if (drl == null) {
            log.warn("[RegistryBootstrap] Skipping ruleId={} — bundle {} has no DRL content",
                    ruleId, bundleHash);
            counters.skipped++;
            return;
        }

        try {
            registerDrlUseCase.register(ruleId, drl);
            counters.ok++;
        } catch (Exception e) {
            log.warn("[RegistryBootstrap] Failed to register ruleId={} (bundle={}): {}",
                    ruleId, bundleHash, e.getMessage());
            counters.failed++;
        }
    }

    private String loadDrl(String bundleHash) {
        Optional<BundleEntity> bundle = bundleRepo.findById(bundleHash);
        return bundle.map(BundleEntity::getDrlContent).orElse(null);
    }

    private static final class BootstrapCounters {
        int ok;
        int skipped;
        int failed;
    }
}
