package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompileJobJpaRepository extends JpaRepository<CompileJobEntity, String> {

    Optional<CompileJobEntity> findByRuleIdAndTargetVersion(String ruleId, Integer targetVersion);

    List<CompileJobEntity> findByStatus(CompileJobEntity.JobStatus status);

    @Query("SELECT c FROM CompileJobEntity c WHERE c.status = :status ORDER BY c.requestedAt DESC")
    List<CompileJobEntity> findByStatusOrderByRequestedAtDesc(
            @Param("status") CompileJobEntity.JobStatus status);

    List<CompileJobEntity> findByRuleId(String ruleId);

    @Query("SELECT c FROM CompileJobEntity c WHERE c.ruleId = :ruleId ORDER BY c.targetVersion DESC")
    List<CompileJobEntity> findLatestByRuleId(@Param("ruleId") String ruleId);
}