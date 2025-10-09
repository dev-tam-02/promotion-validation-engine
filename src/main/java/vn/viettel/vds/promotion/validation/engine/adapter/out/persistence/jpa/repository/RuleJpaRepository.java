package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.RuleEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleJpaRepository extends JpaRepository<RuleEntity, Long> {

    Optional<RuleEntity> findByName(String name);

    List<RuleEntity> findByEnabled(boolean enabled);

    @Query("SELECT r FROM RuleEntity r WHERE r.enabled = true ORDER BY r.name")
    List<RuleEntity> findAllEnabledRules();

    @Query("SELECT r FROM RuleEntity r WHERE r.name LIKE %:keyword% " +
            "OR r.drlText LIKE %:keyword%")
    List<RuleEntity> searchByKeyword(@Param("keyword") String keyword);

    boolean existsByName(String name);

    void deleteByName(String name);

    @Query("SELECT COUNT(r) FROM RuleEntity r WHERE r.enabled = true")
    long countEnabledRules();
}