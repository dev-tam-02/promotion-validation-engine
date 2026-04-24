package vn.viettel.vds.promotion.rule.engine.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.out.QuotaEventPort;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaCounterService unit tests")
class QuotaCounterServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private QuotaEventPort quotaEventPort;

    @Mock
    private RAtomicLong atomicLong;

    private QuotaCounterService sut;

    @BeforeEach
    void setUp() {
        sut = new QuotaCounterService(redissonClient, quotaEventPort);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomicLong);
    }

    @Nested
    @DisplayName("incrementWithCheck()")
    class IncrementTests {

        @Test
        @DisplayName("Should return true (ALLOW) when counter is below limit after INCR")
        void shouldAllow_whenBelowLimit() {
            // Given: counter becomes 2, limit = 3
            when(atomicLong.incrementAndGet()).thenReturn(2L);

            LocalDateTime ws = LocalDateTime.of(2026, 4, 1, 0, 0);
            LocalDateTime we = LocalDateTime.of(2026, 4, 2, 0, 0);

            // When
            boolean allowed = sut.incrementWithCheck("rule-1", "redeem-1", "cust-1",
                    "cust-1", ws, we, 3);

            // Then
            assertThat(allowed).isTrue();

            // Verify quota_event persisted with INCR type
            ArgumentCaptor<QuotaEventEntity> captor = ArgumentCaptor.forClass(QuotaEventEntity.class);
            verify(quotaEventPort).save(captor.capture());
            QuotaEventEntity saved = captor.getValue();
            assertThat(saved.getEventType()).isEqualTo(QuotaEventEntity.EventType.INCR);
            assertThat(saved.getCountDelta()).isEqualTo(1);
            assertThat(saved.getRuleId()).isEqualTo("rule-1");
            assertThat(saved.getRedemptionId()).isEqualTo("redeem-1");
        }

        @Test
        @DisplayName("Should return false (DENY) when counter exceeds limit after INCR")
        void shouldDeny_whenAtLimit() {
            // Given: counter becomes 4, limit = 3 → over limit
            when(atomicLong.incrementAndGet()).thenReturn(4L);

            // When
            boolean allowed = sut.incrementWithCheck("rule-1", "redeem-1", "cust-1",
                    "cust-1", null, null, 3);

            // Then: over limit → DENY, counter is rolled back via DECR
            assertThat(allowed).isFalse();
            verify(atomicLong).decrementAndGet();
            // No quota_event persisted for a denied attempt
            org.mockito.Mockito.verify(quotaEventPort, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("Should set TTL on first INCR (counter == 1)")
        void shouldSetTtl_onFirstIncr() {
            when(atomicLong.incrementAndGet()).thenReturn(1L);
            LocalDateTime ws = LocalDateTime.of(2026, 4, 1, 0, 0);
            LocalDateTime we = LocalDateTime.of(2026, 4, 2, 0, 0); // 86400 seconds window

            sut.incrementWithCheck("rule-x", "redeem-x", null, "bucket-x", ws, we, 5);

            verify(atomicLong).expire(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("decrement()")
    class DecrementTests {

        @Test
        @DisplayName("Should DECR counter and persist ROLLBACK event")
        void shouldDecrAndPersistRollback() {
            when(atomicLong.decrementAndGet()).thenReturn(1L);

            sut.decrement("rule-1", "redeem-1", "cust-1", "cust-1", null, null);

            verify(atomicLong).decrementAndGet();

            ArgumentCaptor<QuotaEventEntity> captor = ArgumentCaptor.forClass(QuotaEventEntity.class);
            verify(quotaEventPort).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(QuotaEventEntity.EventType.ROLLBACK);
            assertThat(captor.getValue().getCountDelta()).isEqualTo(-1);
        }

        @Test
        @DisplayName("Should clamp counter to 0 when DECR would go negative")
        void shouldClamp_whenNegative() {
            when(atomicLong.decrementAndGet()).thenReturn(-1L);

            sut.decrement("rule-1", "redeem-1", null, null, null, null);

            verify(atomicLong).set(0L);
        }
    }

    @Nested
    @DisplayName("overwrite()")
    class OverwriteTests {

        @Test
        @DisplayName("Should SET counter value and TTL")
        void shouldSetValueAndTtl() {
            LocalDateTime ws = LocalDateTime.of(2026, 4, 1, 0, 0);
            LocalDateTime we = LocalDateTime.of(2026, 4, 2, 0, 0);

            sut.overwrite("rule-1", "bucket-1", ws, we, 42L);

            verify(atomicLong).set(42L);
            verify(atomicLong).expire(anyLong(), any());
        }
    }
}
