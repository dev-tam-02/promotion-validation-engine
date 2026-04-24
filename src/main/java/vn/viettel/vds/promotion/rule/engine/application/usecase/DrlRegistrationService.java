package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.application.port.in.RegisterDrlUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleRegistryPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.IncrementalKieContainerService;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service for DRL rule CRUD operations exposed at POST/PUT/DELETE /v1/rules.
 *
 * <h3>Task 09 V5 — incremental KieBase update</h3>
 * <p>Each successful register/update call delegates to
 * {@link IncrementalKieContainerService#registerOrUpdate} which:
 * <ol>
 *   <li>Compiles all currently-registered rules <em>plus</em> the new/updated DRL into a
 *       fresh versioned {@link org.kie.api.builder.ReleaseId}.</li>
 *   <li>Calls {@link org.kie.api.runtime.KieContainer#updateToVersion(org.kie.api.builder.ReleaseId)}
 *       for an atomic, in-place KieBase swap with zero downtime.</li>
 * </ol>
 * Concurrent evaluations that started before the swap continue on the previous KieBase;
 * new sessions transparently use the updated one.
 *
 * <h3>POST idempotency</h3>
 * Same (ruleId, drl) → return existing bundleHash without recompiling.
 * Same ruleId, different drl → {@link RegisterDrlUseCase.DrlConflictException} (409).
 *
 * <h3>Persistence</h3>
 * Delegates to {@link RuleRegistryPort}. MVP uses an in-memory ConcurrentHashMap;
 * Task 09 V2 introduced a JPA-backed adapter.
 */
@Service
public class DrlRegistrationService implements RegisterDrlUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DrlRegistrationService.class);

    private final RuleRegistryPort ruleRegistry;
    private final IncrementalKieContainerService incrementalKieSvc;

    public DrlRegistrationService(RuleRegistryPort ruleRegistry,
                                   IncrementalKieContainerService incrementalKieSvc) {
        this.ruleRegistry = ruleRegistry;
        this.incrementalKieSvc = incrementalKieSvc;
    }

    @Override
    public RegisterRuleResult register(String ruleId, String drl) {
        logger.info("Registering DRL rule: ruleId={}", ruleId);

        Optional<RegisteredRule> existing = ruleRegistry.findById(ruleId);
        if (existing.isPresent()) {
            RegisteredRule existingRule = existing.get();
            if (existingRule.hasSameDrl(drl)) {
                logger.info("Idempotent POST: rule already registered with identical DRL, ruleId={}, bundleHash={}",
                        ruleId, existingRule.getBundleHash());
                return new RegisterRuleResult(ruleId, existingRule.getBundleHash());
            }
            throw new DrlConflictException(
                    "Rule '" + ruleId + "' already exists with different DRL content. Use PUT /v1/rules/{id} to update.");
        }

        // Build snapshot of all currently registered DRLs (new rule not yet in registry)
        Map<String, String> currentDrls = buildCurrentDrlsMap();

        String bundleHash = applyIncrementalUpdate(ruleId, drl, currentDrls);

        RegisteredRule rule = new RegisteredRule(ruleId, drl, bundleHash, Instant.now(), Instant.now());
        ruleRegistry.save(rule);

        logger.info("Rule registered (incremental KieBase): ruleId={}, bundleHash={}", ruleId, bundleHash);
        return new RegisterRuleResult(ruleId, bundleHash);
    }

    @Override
    public RegisterRuleResult update(String ruleId, String drl) {
        logger.info("Updating DRL rule: ruleId={}", ruleId);

        RegisteredRule existing = ruleRegistry.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + ruleId));

        // Snapshot includes the old DRL for ruleId — registerOrUpdate will overwrite it
        Map<String, String> currentDrls = buildCurrentDrlsMap();

        String bundleHash = applyIncrementalUpdate(ruleId, drl, currentDrls);

        RegisteredRule updated = new RegisteredRule(ruleId, drl, bundleHash,
                existing.getRegisteredAt(), Instant.now());
        ruleRegistry.save(updated);

        logger.info("Rule updated (incremental KieBase): ruleId={}, bundleHash={}", ruleId, bundleHash);
        return new RegisterRuleResult(ruleId, bundleHash);
    }

    @Override
    public void delete(String ruleId) {
        logger.info("Deleting DRL rule: ruleId={}", ruleId);

        if (ruleRegistry.findById(ruleId).isEmpty()) {
            throw new RuleNotFoundException("Rule not found: " + ruleId);
        }

        ruleRegistry.deleteById(ruleId);
        logger.info("Rule deleted: ruleId={}", ruleId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Snapshot of ruleId→drl for all rules currently in the registry.
     * Called <em>before</em> saving the new/updated rule so the incremental service
     * receives the pre-change state (it will overwrite the target ruleId itself).
     */
    private Map<String, String> buildCurrentDrlsMap() {
        return ruleRegistry.findAll().stream()
                .collect(Collectors.toMap(RegisteredRule::getRuleId, RegisteredRule::getDrl));
    }

    /**
     * Delegate compilation + atomic KieBase swap to the incremental service.
     * Translates domain {@link IncrementalKieContainerService.IncrementalCompileException}
     * into application-layer {@link DrlCompileException}.
     */
    private String applyIncrementalUpdate(String ruleId, String drl, Map<String, String> currentDrls) {
        try {
            return incrementalKieSvc.registerOrUpdate(ruleId, drl, currentDrls);
        } catch (IncrementalKieContainerService.IncrementalCompileException e) {
            String message = "DRL compilation failed for rule '" + ruleId + "': " + e.getMessage();
            logger.warn(message);
            throw new DrlCompileException(message, e.getCompileLogs());
        }
    }
}
