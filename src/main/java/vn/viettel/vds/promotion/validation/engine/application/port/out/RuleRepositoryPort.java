package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.domain.model.Rule;

import java.util.List;

public interface RuleRepositoryPort {

    List<Rule> findEnabledRules();

    Rule findById(String id);

    Rule save(Rule rule);

    void deleteById(String id);

    List<Rule> findAll();
}