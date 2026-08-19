package vn.viettel.vds.promotion.validation.engine.domain.service.operator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
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
        log.info("Getting translator for operator: {} version: {}", operatorName, version);
        log.info("Available translators: {}", translators);
        return findTranslator(operatorName, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("No translator found for operator: %s version: %s", operatorName, version)));
    }
}