package vn.viettel.vds.promotion.rule.engine.infrastructure.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.application.port.out.QuotaEventPort;
import vn.viettel.vds.promotion.rule.engine.application.service.QuotaCounterService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RebuildQuotaCountersJob — rebuild + drift metric tests")
class RebuildQuotaCountersJobTest {

    @Mock
    private QuotaEventPort quotaEventPort;

    @Mock
    private QuotaCounterService quotaCounterService;

    private MeterRegistry meterRegistry;
    private RebuildQuotaCountersJob sut;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        sut = new RebuildQuotaCountersJob(quotaEventPort, quotaCounterService, meterRegistry);
    }

    @Test
    @DisplayName("Should overwrite Redis counters for each active-window aggregate from DB")
    void shouldRebuildCounters_fromDbAggregates() {
        // Given: DB aggregate returns one bucket
        LocalDateTime ws = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime we = LocalDateTime.of(2026, 4, 2, 0, 0);
        long wsEpoch = ws.toEpochSecond(ZoneOffset.UTC);
        long weEpoch = we.toEpochSecond(ZoneOffset.UTC);

        String aggregateKey = "rule-1:bucket-1:" + wsEpoch + ":wend:" + weEpoch;
        when(quotaEventPort.aggregateActiveWindowCounters(any())).thenReturn(Map.of(aggregateKey, 5L));
        when(quotaCounterService.computeDriftRatio(any())).thenReturn(0.0);

        // When
        Map<String, Object> result = sut.triggerManual();

        // Then: overwrite called with correct args
        verify(quotaCounterService).overwrite(
                "rule-1",
                "bucket-1",
                ws,
                we,
                5L
        );
        assertThat((Integer) result.get("rebuilt")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should register layer1_counter_drift gauge in meter registry")
    void shouldRegisterDriftGauge() {
        assertThat(meterRegistry.find("layer1_counter_drift").gauge()).isNotNull();
    }

    @Test
    @DisplayName("Should report drift ratio from QuotaCounterService in manual trigger response")
    void shouldReportDrift_inManualTriggerResponse() {
        when(quotaEventPort.aggregateActiveWindowCounters(any())).thenReturn(Map.of());
        when(quotaCounterService.computeDriftRatio(any())).thenReturn(0.002); // 0.2% drift

        Map<String, Object> result = sut.triggerManual();

        assertThat((Double) result.get("driftRatio")).isEqualTo(0.002);
    }
}
