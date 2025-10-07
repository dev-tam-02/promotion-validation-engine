package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompileJobJpaRepository extends JpaRepository<CompileJobEntity, String> {

    Optional<CompileJobEntity> findByTenantIdAndRuleIdAndTargetVersion(
            String tenantId, String ruleId, Integer targetVersion);

    List<CompileJobEntity> findByTenantIdAndStatus(
            String tenantId, CompileJobEntity.JobStatus status);

    @Query("SELECT c FROM CompileJobEntity c WHERE c.tenantId = :tenantId " +
            "AND c.status = :status ORDER BY c.requestedAt DESC")
    List<CompileJobEntity> findByTenantIdAndStatusOrderByRequestedAtDesc(
            @Param("tenantId") String tenantId,
            @Param("status") CompileJobEntity.JobStatus status);

    List<CompileJobEntity> findByTenantIdAndRuleId(String tenantId, String ruleId);

    @Query("SELECT c FROM CompileJobEntity c WHERE c.tenantId = :tenantId " +
            "AND c.ruleId = :ruleId ORDER BY c.targetVersion DESC")
    List<CompileJobEntity> findLatestByTenantIdAndRuleId(
            @Param("tenantId") String tenantId,
            @Param("ruleId") String ruleId);
}