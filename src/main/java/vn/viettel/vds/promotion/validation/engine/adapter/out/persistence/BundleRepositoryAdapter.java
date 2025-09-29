package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.Bundle;
import vn.viettel.vds.promotion.validation.engine.application.port.out.BundleRepositoryPort;

import java.util.Optional;

@Repository
public interface BundleRepositoryAdapter extends MongoRepository<Bundle, String>, BundleRepositoryPort {

    @Override
    Optional<Bundle> findByTenantIdAndRuleIdAndRuleVersion(String tenantId, String ruleId, Integer ruleVersion);
}