package vn.viettel.vds.promotion.rule.engine.adapter.out.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleRegistryPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link RuleRegistryPort} backed by a {@link ConcurrentHashMap}.
 *
 * <p>This is the MVP (Task 07b) implementation. All state is lost on service restart.
 * Task 09 V2 will introduce a JPA-backed adapter persisting to a {@code rule_registry} table,
 * at which point this bean will be replaced (or kept as a fallback for tests).
 *
 * <p>Thread-safety: ConcurrentHashMap provides safe concurrent reads and writes.
 * The {@code save} operation is atomic at the map level (put). For the idempotency check
 * (read-then-write in {@link vn.viettel.vds.promotion.rule.engine.application.usecase.DrlRegistrationService}),
 * the application layer is responsible for holding a higher-level lock if strict linearizability
 * is required in a multi-threaded burst. For the MVP single-node deployment, the current approach
 * is acceptable.
 */
@Component
public class InMemoryRuleRegistryAdapter implements RuleRegistryPort {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryRuleRegistryAdapter.class);

    private final ConcurrentHashMap<String, RegisteredRule> registry = new ConcurrentHashMap<>();

    @Override
    public Optional<RegisteredRule> findById(String ruleId) {
        return Optional.ofNullable(registry.get(ruleId));
    }

    @Override
    public Collection<RegisteredRule> findAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    @Override
    public void save(RegisteredRule rule) {
        registry.put(rule.getRuleId(), rule);
        logger.debug("Saved rule to registry: ruleId={}, bundleHash={}, registrySize={}",
                rule.getRuleId(), rule.getBundleHash(), registry.size());
    }

    @Override
    public void deleteById(String ruleId) {
        RegisteredRule removed = registry.remove(ruleId);
        if (removed != null) {
            logger.debug("Removed rule from registry: ruleId={}, registrySize={}", ruleId, registry.size());
        }
    }

    /** For observability / health checks. Not part of the port contract. */
    public int size() {
        return registry.size();
    }
}
