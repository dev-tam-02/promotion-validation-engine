package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.AssignmentEntity;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepositoryPort {

    AssignmentEntity save(AssignmentEntity assignment);

    Optional<AssignmentEntity> findById(String id);

    Optional<AssignmentEntity> findBySubjectTypeAndSubjectKeyAndRuleId(
            String subjectType, String subjectKey, String ruleId);

    List<AssignmentEntity> findActiveBySubjectOrderByPriority(
            String subjectType, String subjectKey);

    List<AssignmentEntity> findByRuleId(String ruleId);

    List<AssignmentEntity> findAllActive();

    List<AssignmentEntity> findActiveMissingBundle();

    void deleteBySubjectTypeAndSubjectKeyAndRuleId(
            String subjectType, String subjectKey, String ruleId);
}
