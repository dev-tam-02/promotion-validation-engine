package vn.viettel.vds.promotion.validation.engine.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.CompileJob;

import java.time.Instant;
import java.util.Optional;

public interface CompileJobRepositoryPort {

    CompileJob save(CompileJob compileJob);

    Optional<CompileJob> findById(String jobId);

    Optional<CompileJob> findByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion);

    Page<CompileJob> findByTenantIdAndRuleIdAndStatusAndRequestedAtBetween(
            String tenantId, String ruleId, CompileJob.JobStatus status,
            Instant from, Instant to, Pageable pageable);

    Page<CompileJob> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable);

    Page<CompileJob> findByTenantId(String tenantId, Pageable pageable);
}