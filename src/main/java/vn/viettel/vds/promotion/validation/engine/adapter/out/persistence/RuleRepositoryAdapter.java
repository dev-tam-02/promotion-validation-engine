package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.RuleMongoEntity;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.mapper.RuleMapper;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.repository.RuleMongoRepository;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleRepositoryPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.Rule;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RuleRepositoryAdapter implements RuleRepositoryPort {

    private final RuleMongoRepository ruleMongoRepository;
    private final RuleMapper ruleMapper;

    public RuleRepositoryAdapter(RuleMongoRepository ruleMongoRepository, RuleMapper ruleMapper) {
        this.ruleMongoRepository = ruleMongoRepository;
        this.ruleMapper = ruleMapper;
    }

    @Override
    public List<Rule> findEnabledRules() {
        return ruleMongoRepository.findByEnabledTrue()
                .stream()
                .map(ruleMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Rule findById(String id) {
        return ruleMongoRepository.findById(id)
                .map(ruleMapper::toDomain)
                .orElse(null);
    }

    @Override
    public Rule save(Rule rule) {
        RuleMongoEntity entity = ruleMapper.toEntity(rule);
        RuleMongoEntity savedEntity = ruleMongoRepository.save(entity);
        return ruleMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(String id) {
        ruleMongoRepository.deleteById(id);
    }

    @Override
    public List<Rule> findAll() {
        return ruleMongoRepository.findAll()
                .stream()
                .map(ruleMapper::toDomain)
                .collect(Collectors.toList());
    }
}