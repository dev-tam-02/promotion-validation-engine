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

    Optional<BundleEntity> findByRuleIdAndRuleVersion(String ruleId, Integer ruleVersion);

    List<BundleEntity> findByRuleId(String ruleId);

    @Query("SELECT b FROM BundleEntity b WHERE b.ruleId = :ruleId ORDER BY b.ruleVersion DESC")
    List<BundleEntity> findLatestByRuleId(@Param("ruleId") String ruleId);

    @Query("SELECT b FROM BundleEntity b ORDER BY b.createdAt DESC")
    List<BundleEntity> findLatest();

    @Query("SELECT COUNT(b) FROM BundleEntity b WHERE b.ruleId = :ruleId")
    long countByRuleId(@Param("ruleId") String ruleId);

    List<BundleEntity> findByEnabledTrue();

    List<BundleEntity> findByEnabled(boolean enabled);
}