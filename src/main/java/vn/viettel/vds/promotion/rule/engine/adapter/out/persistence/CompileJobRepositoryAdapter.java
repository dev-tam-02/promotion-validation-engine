package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.CompileJobJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.CompileJobRepositoryPort;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompileJobRepositoryAdapter implements CompileJobRepositoryPort {

    private final CompileJobJpaRepository compileJobJpaRepository;

    @Override
    public CompileJobEntity save(CompileJobEntity compileJob) {
        return compileJobJpaRepository.save(compileJob);
    }

    @Override
    public Optional<CompileJobEntity> findById(String jobId) {
        return compileJobJpaRepository.findById(jobId);
    }

    @Override
    public Optional<CompileJobEntity> findByRuleIdAndTargetVersion(String ruleId, Integer targetVersion) {
        return compileJobJpaRepository.findByRuleIdAndTargetVersion(ruleId, targetVersion);
    }

    @Override
    public Page<CompileJobEntity> findByRuleIdAndStatusAndRequestedAtBetween(
            String ruleId, CompileJobEntity.JobStatus status,
            Instant from, Instant to, Pageable pageable) {
        // Need to implement this method in JpaRepository or use @Query
        return Page.empty(pageable);
    }

    @Override
    public Page<CompileJobEntity> findByRuleId(String ruleId, Pageable pageable) {
        // Need to implement this method in JpaRepository
        return Page.empty(pageable);
    }

    @Override
    public Page<CompileJobEntity> findAll(Pageable pageable) {
        return compileJobJpaRepository.findAll(pageable);
    }
}