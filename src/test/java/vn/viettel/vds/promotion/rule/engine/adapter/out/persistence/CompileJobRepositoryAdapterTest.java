package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.viettel.vds.promotion.rule.engine.TestFixtures;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.CompileJobJpaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompileJobRepositoryAdapter Tests")
class CompileJobRepositoryAdapterTest {

    @Mock
    private CompileJobJpaRepository compileJobJpaRepository;

    @InjectMocks
    private CompileJobRepositoryAdapter sut;

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("Should delegate save to JPA repository")
        void shouldDelegateSaveToJpaRepository() {
            // Given
            var job = TestFixtures.compileJob("job-1", "rule-1", 1, CompileJobEntity.JobStatus.RUNNING);
            when(compileJobJpaRepository.save(job)).thenReturn(job);

            // When
            var result = sut.save(job);

            // Then
            assertThat(result.getId()).isEqualTo("job-1");
            verify(compileJobJpaRepository).save(job);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return job when found")
        void shouldReturnJob_whenFound() {
            // Given
            var job = TestFixtures.compileJob("job-1", "rule-1", 1, CompileJobEntity.JobStatus.SUCCESS);
            when(compileJobJpaRepository.findById("job-1")).thenReturn(Optional.of(job));

            // When
            var result = sut.findById("job-1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getRuleId()).isEqualTo("rule-1");
        }

        @Test
        @DisplayName("Should return empty when not found")
        void shouldReturnEmpty_whenNotFound() {
            // Given
            when(compileJobJpaRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When
            var result = sut.findById("nonexistent");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRuleIdAndTargetVersion()")
    class FindByRuleIdAndTargetVersionTests {

        @Test
        @DisplayName("Should return job for specific rule and version")
        void shouldReturnJob_forSpecificRuleAndVersion() {
            // Given
            var job = TestFixtures.compileJob("job-1", "rule-1", 2, CompileJobEntity.JobStatus.SUCCESS);
            when(compileJobJpaRepository.findByRuleIdAndTargetVersion("rule-1", 2))
                    .thenReturn(Optional.of(job));

            // When
            var result = sut.findByRuleIdAndTargetVersion("rule-1", 2);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTargetVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Should delegate to JPA repository with pageable")
        void shouldDelegateToJpaRepository() {
            // Given
            var pageable = PageRequest.of(0, 10);
            var job = TestFixtures.compileJob("job-1", "rule-1", 1, CompileJobEntity.JobStatus.SUCCESS);
            var page = new PageImpl<>(List.of(job), pageable, 1);
            when(compileJobJpaRepository.findAll(pageable)).thenReturn(page);

            // When
            var result = sut.findAll(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }
}
