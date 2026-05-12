package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;

import java.util.Collection;
import java.util.Optional;

/**
 * Output port for persisting and retrieving registered DRL rules.
 *
 * <p>MVP implementation: {@link vn.viettel.vds.promotion.rule.engine.adapter.out.registry.InMemoryRuleRegistryAdapter}
 * (ConcurrentHashMap). Task 09 V2 will provide a JPA-backed adapter.
 */
public interface RuleRegistryPort {

    /**
     * Find a registered rule by its id.
     *
     * @param ruleId the rule identifier
     * @return the rule if it exists
     */
    Optional<RegisteredRule> findById(String ruleId);

    /**
     * Return all currently registered rules.
     * Used by the incremental KieContainer update (Task 09 V5) to rebuild the
     * full DRL snapshot when applying a new rule version.
     *
     * @return unmodifiable snapshot of all registered rules
     */
    Collection<RegisteredRule> findAll();

    /**
     * Persist (insert or replace) a registered rule.
     *
     * @param rule the rule to save
     */
    void save(RegisteredRule rule);

    /**
     * Remove a registered rule by its id. No-op if not found.
     *
     * @param ruleId the rule identifier
     */
    void deleteById(String ruleId);
}
