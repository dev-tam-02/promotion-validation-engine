package vn.viettel.vds.promotion.rule.engine.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.out.QuotaEventPort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaRollbackService — rollback endpoint unit tests")
class QuotaRollbackServiceTest {

    @Mock
    private QuotaEventPort quotaEventPort;

    @Mock
    private QuotaCounterService quotaCounterService;

    private QuotaRollbackService sut;

    @BeforeEach
    void setUp() {
        sut = new QuotaRollbackService(quotaEventPort, quotaCounterService);
    }

    @Test
    @DisplayName("Should decrement counter and return reverted count for each uncompensated event")
    void shouldRollback_uncompensatedEvents() {
        // Given: two uncompensated INCR events for the same redemption
        QuotaEventEntity e1 = buildEvent("rule-1", "redeem-1", "cust-A", "cust-A");
        QuotaEventEntity e2 = buildEvent("rule-2", "redeem-1", "cust-A", "cust-A");
        when(quotaEventPort.findUncompensated("redeem-1")).thenReturn(List.of(e1, e2));

        // When
        int count = sut.rollback("redeem-1");

        // Then: both counters decremented
        assertThat(count).isEqualTo(2);
        verify(quotaCounterService).decrement(eq("rule-1"), eq("redeem-1"), any(), any(), any(), any());
        verify(quotaCounterService).decrement(eq("rule-2"), eq("redeem-1"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return 0 (NO_OP) when no uncompensated events exist")
    void shouldReturnZero_whenAlreadyRolledBack() {
        when(quotaEventPort.findUncompensated("redeem-99")).thenReturn(List.of());

        int count = sut.rollback("redeem-99");

        assertThat(count).isZero();
        verifyNoInteractions(quotaCounterService);
    }

    private QuotaEventEntity buildEvent(String ruleId, String redemptionId,
                                        String customerId, String bucketKey) {
        QuotaEventEntity e = new QuotaEventEntity();
        e.setRuleId(ruleId);
        e.setRedemptionId(redemptionId);
        e.setCustomerId(customerId);
        e.setBucketKey(bucketKey);
        e.setWindowStart(LocalDateTime.of(2026, 4, 1, 0, 0));
        e.setWindowEnd(LocalDateTime.of(2026, 4, 2, 0, 0));
        e.setEventType(QuotaEventEntity.EventType.INCR);
        e.setCountDelta(1);
        return e;
    }
}
