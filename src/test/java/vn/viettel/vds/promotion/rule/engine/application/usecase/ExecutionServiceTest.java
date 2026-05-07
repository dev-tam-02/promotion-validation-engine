package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.TestFixtures;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.ExecutionUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.EngineConfigRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionService Tests")
class ExecutionServiceTest {

    @Mock
    private BundleRepositoryPort bundleRepository;

    @Mock
    private EngineConfigRepositoryPort engineConfigRepository;

    @Mock
    private RuleEnginePort ruleEnginePort;

    @Mock
    private ObjectStoragePort objectStoragePort;

    private ExecutionService sut;

    @BeforeEach
    void setUp() {
        sut = new ExecutionService(bundleRepository, engineConfigRepository, ruleEnginePort, objectStoragePort);
    }

    @Nested
    @DisplayName("execute()")
    class ExecuteTests {

        @Test
        @DisplayName("Should execute rule successfully when bundle exists and is cached")
        void shouldExecuteSuccessfully_whenBundleExistsAndCached() {
            // Given
            String bundleHash = "sha256:abc123";
            var request = TestFixtures.executeRequest(bundleHash, TestFixtures.validContext());
            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);
            var expectedResponse = TestFixtures.executeResponse(true, "ALLOW");

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(true);
            when(ruleEnginePort.execute(any(RuleEnginePort.ExecuteInput.class))).thenReturn(expectedResponse);

            // When
            var result = sut.execute(request);

            // Then
            assertThat(result.getOk()).isTrue();
            assertThat(result.getDecision()).isEqualTo("ALLOW");
            verify(ruleEnginePort).execute(any(RuleEnginePort.ExecuteInput.class));
        }

        @Test
        @DisplayName("Should warm up bundle when not cached")
        void shouldWarmUpBundle_whenNotCached() {
            // Given
            String bundleHash = "sha256:abc123";
            var request = TestFixtures.executeRequest(bundleHash, TestFixtures.validContext());
            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);
            var expectedResponse = TestFixtures.executeResponse(true, "ALLOW");
            byte[] artifactBytes = new byte[]{1, 2, 3};

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(false);
            when(objectStoragePort.retrieve(any())).thenReturn(Optional.of(artifactBytes));
            when(ruleEnginePort.execute(any(RuleEnginePort.ExecuteInput.class))).thenReturn(expectedResponse);

            // When
            var result = sut.execute(request);

            // Then
            assertThat(result.getOk()).isTrue();
            verify(ruleEnginePort).warmupBundle(eq(bundleHash), eq(artifactBytes));
        }

        @Test
        @DisplayName("Should throw when bundle not found")
        void shouldThrow_whenBundleNotFound() {
            // Given
            String bundleHash = "sha256:nonexistent";
            var request = TestFixtures.executeRequest(bundleHash, TestFixtures.validContext());

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.execute(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bundle not found");
        }

        @Test
        @DisplayName("Should throw when artifact not found in storage")
        void shouldThrow_whenArtifactNotFoundInStorage() {
            // Given
            String bundleHash = "sha256:abc123";
            var request = TestFixtures.executeRequest(bundleHash, TestFixtures.validContext());
            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(false);
            when(objectStoragePort.retrieve(any())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.execute(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Bundle artifact not found");
        }

        @Test
        @DisplayName("Should merge options with config defaults")
        void shouldMergeOptionsWithConfigDefaults() {
            // Given
            String bundleHash = "sha256:abc123";
            var options = new ExecuteRequest.ExecuteOptions("FULL", null, null);
            var request = new ExecuteRequest(
                    new ExecuteRequest.Bundle(bundleHash, 1, 1),
                    TestFixtures.validContext(),
                    options
            );
            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);
            var config = TestFixtures.engineConfig(bundleHash, 100, 200);
            var expectedResponse = TestFixtures.executeResponse(true, "ALLOW");

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.of(config));
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(true);
            when(ruleEnginePort.execute(any(RuleEnginePort.ExecuteInput.class))).thenReturn(expectedResponse);

            // When
            sut.execute(request);

            // Then
            verify(ruleEnginePort).execute(argThat(input ->
                    input.getOptions().getTimeoutMs() == 100 &&
                            input.getOptions().getMaxRulesFired() == 200
            ));
        }

        @Test
        @DisplayName("Should apply system defaults when no config and no request options")
        void shouldApplySystemDefaults_whenNoConfigAndNoOptions() {
            // Given
            String bundleHash = "sha256:abc123";
            var request = TestFixtures.executeRequest(bundleHash, TestFixtures.validContext());
            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);
            var expectedResponse = TestFixtures.executeResponse(true, "ALLOW");

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(true);
            when(ruleEnginePort.execute(any(RuleEnginePort.ExecuteInput.class))).thenReturn(expectedResponse);

            // When
            sut.execute(request);

            // Then
            verify(ruleEnginePort).execute(argThat(input ->
                    input.getOptions().getTimeoutMs() == 40 &&
                            input.getOptions().getMaxRulesFired() == 500 &&
                            "NONE".equals(input.getOptions().getExplain())
            ));
        }

        @Test
        @DisplayName("Should wrap and rethrow rule execution exception")
        void shouldWrapAndRethrow_whenRuleExecutionFails() {
            // Given
            String bundleHash = "sha256:abc123";
            var request = TestFixtures.executeRequest(bundleHash, TestFixtures.validContext());
            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(true);
            when(ruleEnginePort.execute(any())).thenThrow(new RuntimeException("Drools error"));

            // When & Then
            assertThatThrownBy(() -> sut.execute(request))
                    .isInstanceOf(RuleExecutionException.class)
                    .hasMessageContaining("Rule execution failed");
        }
    }

    @Nested
    @DisplayName("executeBatch()")
    class ExecuteBatchTests {

        @Test
        @DisplayName("Should execute batch successfully")
        void shouldExecuteBatchSuccessfully() {
            // Given
            String bundleHash = "sha256:abc123";
            var bundle = new ExecuteRequest.Bundle(bundleHash, 1, 1);
            var context1 = TestFixtures.validContext();
            var context2 = TestFixtures.validContext();
            var cases = List.of(
                    new ExecutionUseCase.BatchExecuteRequest.TestCase("test-1", context1),
                    new ExecutionUseCase.BatchExecuteRequest.TestCase("test-2", context2)
            );
            var batchRequest = new ExecutionUseCase.BatchExecuteRequest(bundle, cases);

            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);
            var response1 = TestFixtures.executeResponse(true, "ALLOW");
            var response2 = TestFixtures.executeResponse(false, "DENY");

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(true);
            when(ruleEnginePort.executeBatch(any())).thenReturn(List.of(response1, response2));

            // When
            var result = sut.executeBatch(batchRequest);

            // Then
            assertThat(result.getResults()).hasSize(2);
            assertThat(result.getStats().getPass()).isEqualTo(1);
            assertThat(result.getStats().getFail()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw when bundle not found for batch")
        void shouldThrow_whenBundleNotFoundForBatch() {
            // Given
            String bundleHash = "sha256:nonexistent";
            var bundle = new ExecuteRequest.Bundle(bundleHash, 1, 1);
            var batchRequest = new ExecutionUseCase.BatchExecuteRequest(bundle, List.of());

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sut.executeBatch(batchRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bundle not found");
        }

        @Test
        @DisplayName("Should throw when context is null")
        void shouldThrow_whenContextIsNull() {
            // Given
            String bundleHash = "sha256:abc123";
            var bundle = new ExecuteRequest.Bundle(bundleHash, 1, 1);
            var cases = List.of(
                    new ExecutionUseCase.BatchExecuteRequest.TestCase("test-null", null)
            );
            var batchRequest = new ExecutionUseCase.BatchExecuteRequest(bundle, cases);

            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> sut.executeBatch(batchRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Context cannot be null");
        }

        @Test
        @DisplayName("Should throw when context missing 'now' field")
        void shouldThrow_whenContextMissingNowField() {
            // Given
            String bundleHash = "sha256:abc123";
            var bundle = new ExecuteRequest.Bundle(bundleHash, 1, 1);
            Map<String, Object> invalidContext = Map.of("customer", Map.of("id", "c1"));
            var cases = List.of(
                    new ExecutionUseCase.BatchExecuteRequest.TestCase("test-no-now", invalidContext)
            );
            var batchRequest = new ExecutionUseCase.BatchExecuteRequest(bundle, cases);

            var bundleEntity = TestFixtures.bundleEntity(bundleHash, "rule-1", 1);

            when(bundleRepository.findById(bundleHash)).thenReturn(Optional.of(bundleEntity));
            when(engineConfigRepository.findById(bundleHash)).thenReturn(Optional.empty());
            when(ruleEnginePort.isBundleCached(bundleHash)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> sut.executeBatch(batchRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("now");
        }
    }
}
