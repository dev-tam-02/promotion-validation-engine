package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface BundleJpaRepository extends JpaRepository<BundleEntity, String> {

    Optional<BundleEntity> findByTenantIdAndRuleIdAndRuleVersion(
            String tenantId, String ruleId, Integer ruleVersion);

    List<BundleEntity> findByTenantId(String tenantId);

    List<BundleEntity> findByTenantIdAndRuleId(String tenantId, String ruleId);

    @Query("SELECT b FROM BundleEntity b WHERE b.tenantId = :tenantId " +
            "AND b.ruleId = :ruleId ORDER BY b.ruleVersion DESC")
    List<BundleEntity> findLatestByTenantIdAndRuleId(
            @Param("tenantId") String tenantId,
            @Param("ruleId") String ruleId);

    @Query("SELECT b FROM BundleEntity b WHERE b.tenantId = :tenantId " +
            "ORDER BY b.createdAt DESC")
    List<BundleEntity> findLatestByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(b) FROM BundleEntity b WHERE b.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
    
    List<BundleEntity> findByEnabledTrue();
    
    List<BundleEntity> findByEnabled(boolean enabled);
}