package vn.viettel.vds.promotion.rule.engine.application.port.in;

import java.util.List;

/**
 * Input port for DRL rule registration (POST/PUT/DELETE /v1/rules).
 *
 * <p>This is the contract used by pp-validation's RuleEngineClient to push compiled DRL
 * directly to pp-rule-engine without going through the structured node tree pipeline.
 *
 * <p>Idempotency rule for POST:
 * <ul>
 *   <li>Same (id, drl) → returns existing bundleHash — treated as idempotent success (no-op).</li>
 *   <li>Same id, different drl → {@link DrlConflictException} (409). Caller must use PUT to update.</li>
 * </ul>
 */
public interface RegisterDrlUseCase {

    /**
     * Register a new DRL rule.
     *
     * @param ruleId unique rule identifier (e.g. "promo-rule-abc123")
     * @param drl    raw Drools DRL content
     * @return registration result containing ruleId and deterministic bundleHash
     * @throws DrlCompileException   if the DRL has syntax errors (→ 400)
     * @throws DrlConflictException  if ruleId already exists with different DRL (→ 409)
     */
    RegisterRuleResult register(String ruleId, String drl);

    /**
     * Update an existing DRL rule. Replaces the stored DRL and recompiles.
     *
     * @param ruleId id of the rule to update
     * @param drl    new DRL content
     * @return updated result with new bundleHash
     * @throws RuleNotFoundException if ruleId does not exist (→ 404)
     * @throws DrlCompileException   if the new DRL has syntax errors (→ 400)
     */
    RegisterRuleResult update(String ruleId, String drl);

    /**
     * Remove a registered rule.
     *
     * @param ruleId id of the rule to delete
     * @throws RuleNotFoundException if ruleId does not exist (→ 404)
     */
    void delete(String ruleId);

    // --- Result ---

    record RegisterRuleResult(String ruleId, String bundleHash) {
    }

    // --- Exceptions ---

    /** Thrown when DRL content fails Drools compilation (syntax error). */
    class DrlCompileException extends RuntimeException {
        private final List<String> compileLogs;

        public DrlCompileException(String message, List<String> compileLogs) {
            super(message);
            this.compileLogs = compileLogs != null ? compileLogs : List.of();
        }

        public List<String> getCompileLogs() {
            return compileLogs;
        }
    }

    /** Thrown when POST is called with an id that already exists but has different DRL. */
    class DrlConflictException extends RuntimeException {
        public DrlConflictException(String message) {
            super(message);
        }
    }

    /** Thrown when PUT or DELETE targets an id that does not exist in the registry. */
    class RuleNotFoundException extends RuntimeException {
        public RuleNotFoundException(String message) {
            super(message);
        }
    }
}
