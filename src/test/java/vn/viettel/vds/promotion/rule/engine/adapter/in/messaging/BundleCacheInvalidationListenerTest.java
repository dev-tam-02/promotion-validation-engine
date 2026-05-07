package vn.viettel.vds.promotion.rule.engine.adapter.in.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;
import vn.viettel.vds.promotion.rule.engine.adapter.in.messaging.config.BroadcastKafkaConfig;
import vn.viettel.vds.promotion.rule.engine.domain.service.bundle.BundlePreloadService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BundleCacheInvalidationListener Tests")
class BundleCacheInvalidationListenerTest {

    @Mock
    private KieSessionManager kieSessionManager;

    @Mock
    private BundlePreloadService bundlePreloadService;

    @Mock
    private BroadcastKafkaConfig broadcastKafkaConfig;

    private BundleCacheInvalidationListener sut;

    @BeforeEach
    void setUp() {
        sut = new BundleCacheInvalidationListener(kieSessionManager, bundlePreloadService, broadcastKafkaConfig);
    }

    private BundleCacheInvalidationEvent createEvent(String bundleHash, String ruleId, Integer ruleVersion,
                                                     BundleCacheInvalidationEvent.InvalidationType type,
                                                     String sourceInstanceId) {
        return new BundleCacheInvalidationEvent(bundleHash, ruleId, ruleVersion, type, sourceInstanceId);
    }

    @Nested
    @DisplayName("onCacheInvalidation()")
    class OnCacheInvalidationTests {

        @Test
        @DisplayName("Should skip event from self instance")
        void shouldSkipEvent_fromSelfInstance() {
            // Given
            when(broadcastKafkaConfig.getInstanceId()).thenReturn("instance-1");

            var event = createEvent("sha256:abc", "rule-1", 1,
                    BundleCacheInvalidationEvent.InvalidationType.BUNDLE_UPDATED, "instance-1");

            // When
            sut.onCacheInvalidation(event);

            // Then
            verify(kieSessionManager, never()).evictContainer(any());
            verify(bundlePreloadService, never()).preloadSpecificRules(any());
        }

        @Test
        @DisplayName("Should handle BUNDLE_UPDATED by evicting and preloading")
        void shouldHandleBundleUpdated() {
            // Given
            when(broadcastKafkaConfig.getInstanceId()).thenReturn("instance-1");
            when(bundlePreloadService.preloadSpecificRules(any()))
                    .thenReturn(new BundlePreloadService.PreloadResult(1, 0, 0, 100L));

            var event = createEvent("sha256:abc", "rule-1", 1,
                    BundleCacheInvalidationEvent.InvalidationType.BUNDLE_UPDATED, "instance-2");

            // When
            sut.onCacheInvalidation(event);

            // Then
            verify(kieSessionManager).evictContainer("sha256:abc");
            verify(bundlePreloadService).preloadSpecificRules(List.of("rule-1"));
        }

        @Test
        @DisplayName("Should handle BUNDLE_DELETED by evicting only")
        void shouldHandleBundleDeleted() {
            // Given
            when(broadcastKafkaConfig.getInstanceId()).thenReturn("instance-1");

            var event = createEvent("sha256:abc", "rule-1", 1,
                    BundleCacheInvalidationEvent.InvalidationType.BUNDLE_DELETED, "instance-2");

            // When
            sut.onCacheInvalidation(event);

            // Then
            verify(kieSessionManager).evictContainer("sha256:abc");
            verify(bundlePreloadService, never()).preloadSpecificRules(any());
        }

        @Test
        @DisplayName("Should handle FULL_INVALIDATION by clearing cache and preloading all")
        void shouldHandleFullInvalidation() {
            // Given
            when(broadcastKafkaConfig.getInstanceId()).thenReturn("instance-1");

            var event = createEvent(null, null, null,
                    BundleCacheInvalidationEvent.InvalidationType.FULL_INVALIDATION, "instance-2");

            // When
            sut.onCacheInvalidation(event);

            // Then
            verify(kieSessionManager).clearCache();
            verify(bundlePreloadService).preloadActiveRules();
        }

        @Test
        @DisplayName("Should handle BUNDLE_UPDATED without ruleId gracefully")
        void shouldHandleBundleUpdated_withoutRuleId() {
            // Given
            when(broadcastKafkaConfig.getInstanceId()).thenReturn("instance-1");

            var event = createEvent("sha256:abc", null, 1,
                    BundleCacheInvalidationEvent.InvalidationType.BUNDLE_UPDATED, "instance-2");

            // When
            sut.onCacheInvalidation(event);

            // Then
            verify(kieSessionManager).evictContainer("sha256:abc");
            verify(bundlePreloadService, never()).preloadSpecificRules(any());
        }

        @Test
        @DisplayName("Should not propagate exception from preload failure")
        void shouldNotPropagateException_fromPreloadFailure() {
            // Given
            when(broadcastKafkaConfig.getInstanceId()).thenReturn("instance-1");
            when(bundlePreloadService.preloadSpecificRules(any()))
                    .thenThrow(new RuntimeException("Storage unavailable"));

            var event = createEvent("sha256:abc", "rule-1", 1,
                    BundleCacheInvalidationEvent.InvalidationType.BUNDLE_UPDATED, "instance-2");

            // When - should not throw
            sut.onCacheInvalidation(event);

            // Then
            verify(kieSessionManager).evictContainer("sha256:abc");
        }
    }
}
