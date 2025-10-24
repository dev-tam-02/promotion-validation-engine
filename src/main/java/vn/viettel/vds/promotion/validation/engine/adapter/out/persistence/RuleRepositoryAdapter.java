package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.RuleEntity;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository.RuleJpaRepository;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleRepositoryPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.Rule;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleRepositoryAdapter implements RuleRepositoryPort {

    private final RuleJpaRepository ruleJpaRepository;

    @Override
    public List<Rule> findEnabledRules() {
        return ruleJpaRepository.findAllEnabledRules()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Rule findById(String id) {
        try {
            Long longId = Long.parseLong(id);
            return ruleJpaRepository.findById(longId)
                    .map(this::toDomain)
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Rule save(Rule rule) {
        RuleEntity entity = toEntity(rule);
        RuleEntity savedEntity = ruleJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public void deleteById(String id) {
        try {
            Long longId = Long.parseLong(id);
            ruleJpaRepository.deleteById(longId);
        } catch (NumberFormatException e) {
            // Ignore invalid ID format
        }
    }

    @Override
    public List<Rule> findAll() {
        return ruleJpaRepository.findAll()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Rule toDomain(RuleEntity entity) {
        Rule rule = new Rule();
        rule.setId(entity.getId() != null ? entity.getId().toString() : null);
        rule.setName(entity.getName());
        rule.setVersion(entity.getVersion());
        rule.setDrlText(entity.getDrlText());
        rule.setEnabled(entity.isEnabled());
        return rule;
    }

    private RuleEntity toEntity(Rule rule) {
        RuleEntity entity = new RuleEntity();
        if (rule.getId() != null) {
            try {
                entity.setId(Long.parseLong(rule.getId()));
            } catch (NumberFormatException e) {
                // New entity, ID will be generated
            }
        }
        entity.setName(rule.getName());
        entity.setVersion(rule.getVersion());
        entity.setDrlText(rule.getDrlText());
        entity.setEnabled(rule.isEnabled());
        return entity;
    }
}