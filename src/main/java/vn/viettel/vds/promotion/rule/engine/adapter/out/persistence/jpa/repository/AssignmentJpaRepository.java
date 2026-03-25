package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.AssignmentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity, String> {

    Optional<AssignmentEntity> findBySubjectTypeAndSubjectKeyAndRuleId(
            String subjectType, String subjectKey, String ruleId);

    List<AssignmentEntity> findBySubjectTypeAndSubjectKeyAndActiveTrue(
            String subjectType, String subjectKey);

    @Query("SELECT a FROM AssignmentEntity a WHERE a.subjectType = :subjectType " +
            "AND a.subjectKey = :subjectKey AND a.active = true ORDER BY a.priority DESC")
    List<AssignmentEntity> findActiveBySubjectOrderByPriority(
            @Param("subjectType") String subjectType,
            @Param("subjectKey") String subjectKey);

    List<AssignmentEntity> findByRuleId(String ruleId);

    List<AssignmentEntity> findByActiveTrue();

    List<AssignmentEntity> findByBundleHash(String bundleHash);

    @Query("SELECT a FROM AssignmentEntity a WHERE a.bundleHash IS NULL AND a.active = true")
    List<AssignmentEntity> findActiveMissingBundle();

    void deleteBySubjectTypeAndSubjectKeyAndRuleId(String subjectType, String subjectKey, String ruleId);
}
