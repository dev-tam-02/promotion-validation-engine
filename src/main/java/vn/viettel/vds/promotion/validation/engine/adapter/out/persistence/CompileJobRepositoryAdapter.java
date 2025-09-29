package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.CompileJob;
import vn.viettel.vds.promotion.validation.engine.application.port.out.CompileJobRepositoryPort;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CompileJobRepositoryAdapter extends MongoRepository<CompileJob, String>, CompileJobRepositoryPort {

    @Override
    Optional<CompileJob> findByTenantIdAndRuleIdAndTargetVersion(String tenantId, String ruleId, Integer targetVersion);

    @Override
    Page<CompileJob> findByTenantIdAndRuleIdAndStatusAndRequestedAtBetween(
            String tenantId, String ruleId, CompileJob.JobStatus status,
            Instant from, Instant to, Pageable pageable);

    @Override
    Page<CompileJob> findByTenantIdAndRuleId(String tenantId, String ruleId, Pageable pageable);

    @Override
    Page<CompileJob> findByTenantId(String tenantId, Pageable pageable);
}