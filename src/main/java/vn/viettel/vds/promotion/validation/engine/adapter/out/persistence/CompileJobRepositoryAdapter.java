package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository.CompileJobJpaRepository;
import vn.viettel.vds.promotion.validation.engine.application.port.out.CompileJobRepositoryPort;

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
    public Optional<CompileJobEntity> findByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion) {
        return compileJobJpaRepository.findByTenantIdAndRuleIdAndTargetVersion(tenantId, ruleId, targetVersion);
    }

    @Override
    public Page<CompileJobEntity> findByTenantIdAndRuleIdAndStatusAndRequestedAtBetween(
            String tenantId, String ruleId, CompileJobEntity.JobStatus status,
            Instant from, Instant to, Pageable pageable) {
        // Need to implement this method in JpaRepository or use @Query
        return Page.empty(pageable);
    }

    @Override
    public Page<CompileJobEntity> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable) {
        // Need to implement this method in JpaRepository
        return Page.empty(pageable);
    }

    @Override
    public Page<CompileJobEntity> findByTenantId(String tenantId, Pageable pageable) {
        // Need to implement this method in JpaRepository
        return Page.empty(pageable);
    }
}