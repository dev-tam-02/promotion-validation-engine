package vn.viettel.vds.promotion.rule.engine.application.service;

/**
 * Result of a {@link QuotaCounterService#incrementWithCheck} call.
 *
 * <p>{@code ok = true}  — increment accepted; current value is within limit.
 * <p>{@code ok = false} — limit exceeded; increment was NOT applied (atomic rollback).
 */
public final class CounterResult {

    private final boolean ok;
    private final long current;

    private CounterResult(boolean ok, long current) {
        this.ok = ok;
        this.current = current;
    }

    public static CounterResult ok(long current) {
        return new CounterResult(true, current);
    }

    public static CounterResult exceeded(long current) {
        return new CounterResult(false, current);
    }

    /** {@code true} if the increment was accepted (not exceeded). */
    public boolean isOk() {
        return ok;
    }

    /** Current counter value after the attempted increment. */
    public long getCurrent() {
        return current;
    }

    @Override
    public String toString() {
        return "CounterResult{ok=" + ok + ", current=" + current + '}';
    }
}
