package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.TimePolicyEntity;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.mapper.TimePolicyMapper;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository.TimePolicyJpaRepository;
import vn.viettel.vds.promotion.validation.engine.application.port.out.TimePolicyRepositoryPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.TimePolicy;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TimePolicyRepositoryAdapter implements TimePolicyRepositoryPort {

    private final TimePolicyJpaRepository timePolicyJpaRepository;
    private final TimePolicyMapper timePolicyMapper;

    @Override
    @Cacheable(value = "time-policies", key = "#policyId")
    public Optional<TimePolicy> findById(String policyId) {
        return timePolicyJpaRepository.findByIdWithWindows(policyId)
                .map(timePolicyMapper::toDomain);
    }

    @Override
    @Cacheable(value = "time-policy-active", key = "#policyId")
    public boolean isActive(String policyId) {
        return timePolicyJpaRepository.existsByIdAndActiveTrue(policyId);
    }
}
