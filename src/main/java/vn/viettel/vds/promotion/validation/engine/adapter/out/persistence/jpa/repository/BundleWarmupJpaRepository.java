package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.BundleWarmupEntity;

import java.util.List;

@Repository
public interface BundleWarmupJpaRepository extends JpaRepository<BundleWarmupEntity, String> {

    List<BundleWarmupEntity> findByTenantIdAndStateOrderByCreatedAt(
            String tenantId, BundleWarmupEntity.WarmupState state);

    List<BundleWarmupEntity> findByStateOrderByCreatedAt(BundleWarmupEntity.WarmupState state);

    List<BundleWarmupEntity> findByBundleHash(String bundleHash);

    void deleteByBundleHash(String bundleHash);
}