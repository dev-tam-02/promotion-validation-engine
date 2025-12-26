package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.TimePolicyEntity;

import java.util.Optional;

@Repository
public interface TimePolicyJpaRepository extends JpaRepository<TimePolicyEntity, String> {

    @Query("SELECT t FROM TimePolicyEntity t LEFT JOIN FETCH t.timeWindows WHERE t.id = :policyId")
    Optional<TimePolicyEntity> findByIdWithWindows(@Param("policyId") String policyId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TimePolicyEntity t WHERE t.id = :policyId AND t.active = true")
    boolean existsByIdAndActiveTrue(@Param("policyId") String policyId);
}
