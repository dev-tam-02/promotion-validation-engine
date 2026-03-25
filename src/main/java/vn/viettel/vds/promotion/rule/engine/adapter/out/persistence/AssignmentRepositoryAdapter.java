package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.AssignmentJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.AssignmentRepositoryPort;

import java.util.List;
import java.util.Optional;

@Repository
public class AssignmentRepositoryAdapter implements AssignmentRepositoryPort {

    private final AssignmentJpaRepository assignmentJpaRepository;

    public AssignmentRepositoryAdapter(AssignmentJpaRepository assignmentJpaRepository) {
        this.assignmentJpaRepository = assignmentJpaRepository;
    }

    @Override
    public AssignmentEntity save(AssignmentEntity assignment) {
        return assignmentJpaRepository.save(assignment);
    }

    @Override
    public Optional<AssignmentEntity> findById(String id) {
        return assignmentJpaRepository.findById(id);
    }

    @Override
    public Optional<AssignmentEntity> findBySubjectTypeAndSubjectKeyAndRuleId(
            String subjectType, String subjectKey, String ruleId) {
        return assignmentJpaRepository.findBySubjectTypeAndSubjectKeyAndRuleId(
                subjectType, subjectKey, ruleId);
    }

    @Override
    public List<AssignmentEntity> findActiveBySubjectOrderByPriority(
            String subjectType, String subjectKey) {
        return assignmentJpaRepository.findActiveBySubjectOrderByPriority(subjectType, subjectKey);
    }

    @Override
    public List<AssignmentEntity> findByRuleId(String ruleId) {
        return assignmentJpaRepository.findByRuleId(ruleId);
    }

    @Override
    public List<AssignmentEntity> findAllActive() {
        return assignmentJpaRepository.findByActiveTrue();
    }

    @Override
    public List<AssignmentEntity> findActiveMissingBundle() {
        return assignmentJpaRepository.findActiveMissingBundle();
    }

    @Override
    public void deleteBySubjectTypeAndSubjectKeyAndRuleId(
            String subjectType, String subjectKey, String ruleId) {
        assignmentJpaRepository.deleteBySubjectTypeAndSubjectKeyAndRuleId(
                subjectType, subjectKey, ruleId);
    }
}
