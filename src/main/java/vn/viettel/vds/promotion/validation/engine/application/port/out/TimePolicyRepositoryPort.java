package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.domain.model.TimePolicy;

import java.util.Optional;

/**
 * Port for accessing time policy data
 */
public interface TimePolicyRepositoryPort {

    /**
     * Find a time policy by ID
     *
     * @param policyId The policy identifier
     * @return Optional containing the policy if found
     */
    Optional<TimePolicy> findById(String policyId);

    /**
     * Check if a policy exists and is active
     *
     * @param policyId The policy identifier
     * @return true if policy exists and is active
     */
    boolean isActive(String policyId);
}
