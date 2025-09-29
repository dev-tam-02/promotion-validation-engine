package vn.viettel.vds.promotion.validation.engine.domain.service.operator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OperatorTranslatorRegistry {

    private final List<OperatorTranslator> translators;

    public OperatorTranslatorRegistry(List<OperatorTranslator> translators) {
        this.translators = translators;
    }

    public Optional<OperatorTranslator> findTranslator(String operatorName, Integer version) {
        return translators.stream()
            .filter(translator -> translator.supports(operatorName, version))
            .findFirst();
    }

    public OperatorTranslator getTranslator(String operatorName, Integer version) {
        return findTranslator(operatorName, version)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("No translator found for operator: %s version: %s", operatorName, version)));
    }
}