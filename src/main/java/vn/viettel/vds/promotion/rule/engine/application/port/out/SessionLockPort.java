package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;

public interface SessionLockPort {

    /**
     * Attempts to acquire a validation session lock for a candidate and customer
     *
     * @param candidate  the candidate being validated
     * @param customerId the customer ID
     * @param ttlSeconds time-to-live for the lock in seconds
     * @return true if lock was acquired, false if already locked
     */
    boolean acquireValidationLock(Candidate candidate, String customerId, int ttlSeconds);

    /**
     * Releases a validation session lock
     *
     * @param candidate  the candidate
     * @param customerId the customer ID
     * @return true if lock was released, false if lock didn't exist
     */
    boolean releaseValidationLock(Candidate candidate, String customerId);

    /**
     * Checks if a validation session lock exists
     *
     * @param candidate  the candidate
     * @param customerId the customer ID
     * @return true if lock exists, false otherwise
     */
    boolean isValidationLocked(Candidate candidate, String customerId);

    /**
     * Extends the TTL of an existing lock
     *
     * @param candidate  the candidate
     * @param customerId the customer ID
     * @param ttlSeconds new TTL in seconds
     * @return true if lock was extended, false if lock doesn't exist
     */
    boolean extendValidationLock(Candidate candidate, String customerId, int ttlSeconds);
}