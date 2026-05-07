package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.FastCheckConfigEntity;

import java.util.Optional;

@Repository
public interface FastCheckConfigJpaRepository extends JpaRepository<FastCheckConfigEntity, Long> {

    Optional<FastCheckConfigEntity> findBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);

    void deleteBySubjectTypeAndSubjectKey(String subjectType, String subjectKey);
}
