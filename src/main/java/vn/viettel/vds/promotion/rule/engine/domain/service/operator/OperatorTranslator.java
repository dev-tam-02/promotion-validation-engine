package vn.viettel.vds.promotion.rule.engine.domain.service.operator;

import java.util.Map;

public interface OperatorTranslator {

    String translate(String nodeId, Map<String, Object> params, String reasonCode);

    String getOperatorName();

    Integer getVersion();

    boolean supports(String operatorName, Integer version);

    /**
     * Returns {@code true} if this translator emits a QuotaPolicy via {@code result.addPolicy(...)}
     * in the Drools {@code then} clause instead of a {@code when} clause condition.
     *
     * <p>Counter translators are handled specially by {@code RuleTranslationService}:
     * <ul>
     *   <li>LHS: {@link #getCounterFactPattern()} is used as the fact binding in {@code when}.</li>
     *   <li>RHS: {@link #translate} output goes into the {@code then} clause.</li>
     * </ul>
     */
    default boolean isCounterPolicy() {
        return false;
    }

    /**
     * Returns the Drools fact pattern (LHS binding) for counter-policy translators.
     * Only called when {@link #isCounterPolicy()} returns {@code true}.
     * Example: {@code "$c: Customer()"}
     */
    default String getCounterFactPattern() {
        return null;
    }
}