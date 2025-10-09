package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.FastCheckConfigEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface FastCheckConfigJpaRepository extends JpaRepository<FastCheckConfigEntity, Long> {

    Optional<FastCheckConfigEntity> findByCampaignId(String campaignId);

    List<FastCheckConfigEntity> findByTenantId(String tenantId);

    List<FastCheckConfigEntity> findByTenantIdAndEnabled(String tenantId, boolean enabled);

    boolean existsByCampaignId(String campaignId);

    void deleteByCampaignId(String campaignId);
}