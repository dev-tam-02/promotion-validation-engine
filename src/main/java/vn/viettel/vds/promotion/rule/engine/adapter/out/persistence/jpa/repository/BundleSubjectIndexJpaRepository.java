package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleSubjectIndexEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface BundleSubjectIndexJpaRepository extends JpaRepository<BundleSubjectIndexEntity, String> {

    @Query("SELECT b FROM BundleSubjectIndexEntity b WHERE b.tenantId = :tenantId " +
            "AND b.subject.type = :subjectType AND b.subject.key = :subjectKey")
    Optional<BundleSubjectIndexEntity> findByTenantIdAndSubject(
            @Param("tenantId") String tenantId,
            @Param("subjectType") String subjectType,
            @Param("subjectKey") String subjectKey);

    List<BundleSubjectIndexEntity> findByTenantIdAndRuleIdAndRuleVersion(
            String tenantId, String ruleId, Integer ruleVersion);

    List<BundleSubjectIndexEntity> findByBundleHash(String bundleHash);

    void deleteByTenantIdAndRuleId(String tenantId, String ruleId);
}