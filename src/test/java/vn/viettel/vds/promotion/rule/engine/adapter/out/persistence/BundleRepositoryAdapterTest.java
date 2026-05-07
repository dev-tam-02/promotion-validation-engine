package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.TestFixtures;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.BundleJpaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BundleRepositoryAdapter Tests")
class BundleRepositoryAdapterTest {

    @Mock
    private BundleJpaRepository bundleJpaRepository;

    @InjectMocks
    private BundleRepositoryAdapter sut;

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("Should delegate save to JPA repository")
        void shouldDelegateSaveToJpaRepository() {
            // Given
            var entity = TestFixtures.bundleEntity("sha256:abc", "rule-1", 1);
            when(bundleJpaRepository.save(entity)).thenReturn(entity);

            // When
            var result = sut.save(entity);

            // Then
            assertThat(result.getId()).isEqualTo("sha256:abc");
            verify(bundleJpaRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return bundle when found")
        void shouldReturnBundle_whenFound() {
            // Given
            var entity = TestFixtures.bundleEntity("sha256:abc", "rule-1", 1);
            when(bundleJpaRepository.findById("sha256:abc")).thenReturn(Optional.of(entity));

            // When
            var result = sut.findById("sha256:abc");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getRuleId()).isEqualTo("rule-1");
        }

        @Test
        @DisplayName("Should return empty when not found")
        void shouldReturnEmpty_whenNotFound() {
            // Given
            when(bundleJpaRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When
            var result = sut.findById("nonexistent");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRuleIdAndRuleVersion()")
    class FindByRuleIdAndVersionTests {

        @Test
        @DisplayName("Should return bundle for specific rule and version")
        void shouldReturnBundle_forSpecificRuleAndVersion() {
            // Given
            var entity = TestFixtures.bundleEntity("sha256:abc", "rule-1", 2);
            when(bundleJpaRepository.findByRuleIdAndRuleVersion("rule-1", 2))
                    .thenReturn(Optional.of(entity));

            // When
            var result = sut.findByRuleIdAndRuleVersion("rule-1", 2);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getRuleVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("existsById()")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when bundle exists")
        void shouldReturnTrue_whenBundleExists() {
            // Given
            when(bundleJpaRepository.existsById("sha256:abc")).thenReturn(true);

            // When
            var result = sut.existsById("sha256:abc");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when bundle does not exist")
        void shouldReturnFalse_whenBundleDoesNotExist() {
            // Given
            when(bundleJpaRepository.existsById("nonexistent")).thenReturn(false);

            // When
            var result = sut.existsById("nonexistent");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findActiveBundles()")
    class FindActiveBundlesTests {

        @Test
        @DisplayName("Should return enabled bundles")
        void shouldReturnEnabledBundles() {
            // Given
            var entity1 = TestFixtures.bundleEntity("sha256:a", "rule-1", 1);
            var entity2 = TestFixtures.bundleEntity("sha256:b", "rule-2", 1);
            when(bundleJpaRepository.findByEnabledTrue()).thenReturn(List.of(entity1, entity2));

            // When
            var result = sut.findActiveBundles();

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findBundlesByStatus()")
    class FindBundlesByStatusTests {

        @Test
        @DisplayName("Should find active bundles when status is ACTIVE")
        void shouldFindActiveBundles_whenStatusIsActive() {
            // Given
            when(bundleJpaRepository.findByEnabled(true)).thenReturn(List.of(
                    TestFixtures.bundleEntity("sha256:a", "rule-1", 1)
            ));

            // When
            var result = sut.findBundlesByStatus("ACTIVE");

            // Then
            assertThat(result).hasSize(1);
            verify(bundleJpaRepository).findByEnabled(true);
        }

        @Test
        @DisplayName("Should find inactive bundles when status is INACTIVE")
        void shouldFindInactiveBundles_whenStatusIsInactive() {
            // Given
            when(bundleJpaRepository.findByEnabled(false)).thenReturn(List.of());

            // When
            var result = sut.findBundlesByStatus("INACTIVE");

            // Then
            assertThat(result).isEmpty();
            verify(bundleJpaRepository).findByEnabled(false);
        }

        @Test
        @DisplayName("Should handle case-insensitive status")
        void shouldHandleCaseInsensitiveStatus() {
            // Given
            when(bundleJpaRepository.findByEnabled(true)).thenReturn(List.of());

            // When
            sut.findBundlesByStatus("active");

            // Then
            verify(bundleJpaRepository).findByEnabled(true);
        }
    }
}
