package vn.viettel.vds.promotion.rule.engine.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;

import java.time.Instant;
import java.util.Optional;

public interface CompileJobRepositoryPort {

    CompileJobEntity save(CompileJobEntity compileJob);

    Optional<CompileJobEntity> findById(String jobId);

    Optional<CompileJobEntity> findByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion);

    Page<CompileJobEntity> findByTenantIdAndRuleIdAndStatusAndRequestedAtBetween(
            String tenantId, String ruleId, CompileJobEntity.JobStatus status,
            Instant from, Instant to, Pageable pageable);

    Page<CompileJobEntity> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable);

    Page<CompileJobEntity> findByTenantId(String tenantId, Pageable pageable);
}