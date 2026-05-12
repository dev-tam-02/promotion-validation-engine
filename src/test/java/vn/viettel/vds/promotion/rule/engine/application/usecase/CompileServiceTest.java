package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.promix.platform.outbox.spi.OutboxService;
import vn.viettel.vds.promotion.engine.event.BundlePublishedEvent;
import vn.viettel.vds.promotion.rule.engine.TestFixtures;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.out.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompileService Tests")
class CompileServiceTest {

    @Mock
    private BundleRepositoryPort bundleRepository;

    @Mock
    private CompileJobRepositoryPort compileJobRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private RuleEnginePort ruleEnginePort;

    @Captor
    private ArgumentCaptor<CompileJobEntity> compileJobCaptor;

    @Captor
    private ArgumentCaptor<BundleEntity> bundleCaptor;

    private CompileService sut;

    @BeforeEach
    void setUp() {
        sut = new CompileService(bundleRepository, compileJobRepository,
                outboxService, objectStoragePort, eventPublisherPort, ruleEnginePort);
    }

    @Nested
    @DisplayName("compile()")
    class CompileTests {

        @Test
        @DisplayName("Should compile successfully and create bundle")
        void shouldCompileSuccessfully() {
            // Given
            var request = TestFixtures.compileRequest("rule-1", 1);
            var compileResult = new RuleEnginePort.CompileResult(
                    "sha256:abc123", new byte[]{1, 2, 3}, 1024L,
                    List.of("Compiled successfully"), "10.1.0", "package rules;\n..."
            );

            when(compileJobRepository.findByRuleIdAndTargetVersion("rule-1", 1))
                    .thenReturn(Optional.empty());
            when(compileJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ruleEnginePort.compile(any(RuleEnginePort.CompileInput.class))).thenReturn(compileResult);
            when(bundleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            var result = sut.compile(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBundleHash()).isEqualTo("sha256:abc123");
            assertThat(result.getSize()).isEqualTo(1024L);

            verify(objectStoragePort).store(anyString(), any(byte[].class));
            verify(bundleRepository).save(any(BundleEntity.class));
            verify(outboxService).createEvent(
                    eq("Bundle"),
                    eq("sha256:abc123"),
                    eq("BundlePublished"),
                    any(BundlePublishedEvent.class),
                    eq("promotion_bundle_published"),
                    isNull(),
                    eq(BundlePublishedEvent.class)
            );
            verify(eventPublisherPort).publishCacheInvalidation(any());
        }

        @Test
        @DisplayName("Should return existing result when job already succeeded")
        void shouldReturnExistingResult_whenJobAlreadySucceeded() {
            // Given
            var request = TestFixtures.compileRequest("rule-1", 1);
            var existingJob = TestFixtures.compileJob("pj_rule-1_v1", "rule-1", 1,
                    CompileJobEntity.JobStatus.SUCCESS);
            existingJob.setBundleHash("sha256:existing");

            var bundleEntity = TestFixtures.bundleEntity("sha256:existing", "rule-1", 1);

            when(compileJobRepository.findByRuleIdAndTargetVersion("rule-1", 1))
                    .thenReturn(Optional.of(existingJob));
            when(bundleRepository.findById("sha256:existing"))
                    .thenReturn(Optional.of(bundleEntity));

            // When
            var result = sut.compile(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBundleHash()).isEqualTo("sha256:existing");

            verify(ruleEnginePort, never()).compile(any());
        }

        @Test
        @DisplayName("Should throw when compilation already in progress")
        void shouldThrow_whenCompilationInProgress() {
            // Given
            var request = TestFixtures.compileRequest("rule-1", 1);
            var runningJob = TestFixtures.compileJob("pj_rule-1_v1", "rule-1", 1,
                    CompileJobEntity.JobStatus.RUNNING);

            when(compileJobRepository.findByRuleIdAndTargetVersion("rule-1", 1))
                    .thenReturn(Optional.of(runningJob));

            // When & Then
            assertThatThrownBy(() -> sut.compile(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already in progress");
        }

        @Test
        @DisplayName("Should retry when previous job failed")
        void shouldRetry_whenPreviousJobFailed() {
            // Given
            var request = TestFixtures.compileRequest("rule-1", 1);
            var failedJob = TestFixtures.compileJob("pj_rule-1_v1", "rule-1", 1,
                    CompileJobEntity.JobStatus.FAILED);

            var compileResult = new RuleEnginePort.CompileResult(
                    "sha256:new123", new byte[]{4, 5, 6}, 2048L,
                    List.of("Recompiled"), "10.1.0", "package rules;\n..."
            );

            when(compileJobRepository.findByRuleIdAndTargetVersion("rule-1", 1))
                    .thenReturn(Optional.of(failedJob));
            when(compileJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ruleEnginePort.compile(any(RuleEnginePort.CompileInput.class))).thenReturn(compileResult);
            when(bundleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            var result = sut.compile(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBundleHash()).isEqualTo("sha256:new123");
        }

        @Test
        @DisplayName("Should handle compilation failure and update job status")
        void shouldHandleCompilationFailure() {
            // Given
            var request = TestFixtures.compileRequest("rule-1", 1);

            when(compileJobRepository.findByRuleIdAndTargetVersion("rule-1", 1))
                    .thenReturn(Optional.empty());
            when(compileJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ruleEnginePort.compile(any())).thenThrow(new RuntimeException("DRL syntax error"));

            // When & Then
            assertThatThrownBy(() -> sut.compile(request))
                    .isInstanceOf(CompileService.CompilationFailedException.class)
                    .hasMessageContaining("Compilation failed");

            // Verify job was saved twice (create + failure update)
            verify(compileJobRepository, times(2)).save(compileJobCaptor.capture());
            var failedJob = compileJobCaptor.getAllValues().get(1);
            assertThat(failedJob.getStatus()).isEqualTo(CompileJobEntity.JobStatus.FAILED);
        }

        @Test
        @DisplayName("Should create bundle with limits when present")
        void shouldCreateBundleWithLimits() {
            // Given
            var request = TestFixtures.compileRequest("rule-1", 1);
            request.setLimits(new CompileRequest.Limits(5, 100));

            var compileResult = new RuleEnginePort.CompileResult(
                    "sha256:withlimits", new byte[]{1}, 512L,
                    List.of("OK"), "10.1.0", "drl"
            );

            when(compileJobRepository.findByRuleIdAndTargetVersion("rule-1", 1))
                    .thenReturn(Optional.empty());
            when(compileJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ruleEnginePort.compile(any())).thenReturn(compileResult);
            when(bundleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            sut.compile(request);

            // Then
            verify(bundleRepository).save(bundleCaptor.capture());
            var savedBundle = bundleCaptor.getValue();
            assertThat(savedBundle.getLimits()).isNotNull();
            assertThat(savedBundle.getLimits().getPerCustomer()).isEqualTo(5);
            assertThat(savedBundle.getLimits().getPerDay()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("getCompileJob()")
    class GetCompileJobTests {

        @Test
        @DisplayName("Should return compile job when found")
        void shouldReturnCompileJob_whenFound() {
            // Given
            var job = TestFixtures.compileJob("pj_rule-1_v1", "rule-1", 1,
                    CompileJobEntity.JobStatus.SUCCESS);
            job.setBundleHash("sha256:abc");

            when(compileJobRepository.findById("pj_rule-1_v1")).thenReturn(Optional.of(job));

            // When
            var result = sut.getCompileJob("pj_rule-1_v1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("pj_rule-1_v1");
            assertThat(result.ruleId()).isEqualTo("rule-1");
            assertThat(result.status()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("Should throw when compile job not found")
        void shouldThrow_whenCompileJobNotFound() {
            // Given
            when(compileJobRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.getCompileJob("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Compile job not found");
        }
    }
}
