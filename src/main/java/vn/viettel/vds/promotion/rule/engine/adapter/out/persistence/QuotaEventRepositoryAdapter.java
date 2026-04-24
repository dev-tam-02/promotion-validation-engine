package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import com.promix.platform.core.util.IdGenerator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.QuotaEventJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.QuotaEventPort;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Transactional
public class QuotaEventRepositoryAdapter implements QuotaEventPort {

    private final QuotaEventJpaRepository repository;

    public QuotaEventRepositoryAdapter(QuotaEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(QuotaEventEntity event) {
        if (event.getId() == null) {
            event.setId(IdGenerator.generateId());
        }
        repository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuotaEventEntity> findUncompensated(String redemptionId) {
        return repository.findUncompensatedByRedemptionId(redemptionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> aggregateActiveWindowCounters(LocalDateTime threshold) {
        List<Object[]> rows = repository.aggregateActiveWindowCounters(threshold);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String ruleId      = (String) row[0];
            String bucketKey   = (String) row[1];
            LocalDateTime ws   = (LocalDateTime) row[2];
            LocalDateTime we   = (LocalDateTime) row[3];
            Long netDelta      = ((Number) row[4]).longValue();

            long epochSec = ws != null
                    ? ws.toEpochSecond(java.time.ZoneOffset.UTC) : 0L;
            String key = ruleId + ":" + (bucketKey != null ? bucketKey : "") + ":" + epochSec
                    + ":wend:" + (we != null ? we.toEpochSecond(java.time.ZoneOffset.UTC) : 0L);
            result.put(key, netDelta);
        }
        return result;
    }
}
