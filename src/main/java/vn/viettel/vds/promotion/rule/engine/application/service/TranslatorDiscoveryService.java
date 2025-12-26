package vn.viettel.vds.promotion.rule.engine.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.SupportedOperatorResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOperatorsRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOperatorsResponse;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslatorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TranslatorDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(TranslatorDiscoveryService.class);

    private final OperatorTranslatorRegistry translatorRegistry;
    private final List<OperatorTranslator> allTranslators;

    public TranslatorDiscoveryService(OperatorTranslatorRegistry translatorRegistry,
                                      List<OperatorTranslator> allTranslators) {
        this.translatorRegistry = translatorRegistry;
        this.allTranslators = allTranslators;
    }

    /**
     * Get all supported operators with their metadata
     */
    public List<SupportedOperatorResponse> getSupportedOperators() {
        logger.info("Discovering all supported operators");

        List<SupportedOperatorResponse> supportedOperators = new ArrayList<>();

        for (OperatorTranslator translator : allTranslators) {
            String operatorName = translator.getOperatorName();
            Integer version = translator.getVersion();
            String context = extractContext(operatorName);
            String description = generateDescription(operatorName);
            String translatorClass = translator.getClass().getSimpleName();

            SupportedOperatorResponse response = new SupportedOperatorResponse(
                    operatorName, version, description, context, translatorClass
            );
            supportedOperators.add(response);
        }

        logger.info("Found {} supported operators", supportedOperators.size());
        return supportedOperators;
    }

    /**
     * Validate if given operators are supported
     */
    public ValidateOperatorsResponse validateOperators(ValidateOperatorsRequest request) {
        logger.info("Validating {} operators", request.getOperators().size());

        List<String> supportedOperators = new ArrayList<>();
        List<ValidateOperatorsResponse.UnsupportedOperator> unsupportedOperators = new ArrayList<>();

        for (ValidateOperatorsRequest.OperatorValidationItem item : request.getOperators()) {
            String operatorName = item.getOperatorName();
            Integer version = item.getVersion();

            try {
                Optional<OperatorTranslator> translator = translatorRegistry.findTranslator(operatorName, version);

                if (translator.isPresent()) {
                    supportedOperators.add(operatorName + (version != null ? "@" + version : ""));
                    logger.debug("Operator {} version {} is supported", operatorName, version);
                } else {
                    String reason = "No translator found for operator: " + operatorName +
                            (version != null ? " version: " + version : "");
                    unsupportedOperators.add(new ValidateOperatorsResponse.UnsupportedOperator(
                            operatorName, version, reason));
                    logger.warn("Operator {} version {} is not supported", operatorName, version);
                }
            } catch (Exception e) {
                String reason = "Error validating operator: " + e.getMessage();
                unsupportedOperators.add(new ValidateOperatorsResponse.UnsupportedOperator(
                        operatorName, version, reason));
                logger.error("Error validating operator {} version {}: {}", operatorName, version, e.getMessage());
            }
        }

        boolean valid = unsupportedOperators.isEmpty();
        String message = valid ?
                "All operators are supported" :
                String.format("%d out of %d operators are not supported",
                        unsupportedOperators.size(), request.getOperators().size());

        logger.info("Validation result: {} supported, {} unsupported",
                supportedOperators.size(), unsupportedOperators.size());

        return new ValidateOperatorsResponse(valid, supportedOperators, unsupportedOperators, message);
    }

    /**
     * Check if a specific operator is supported
     */
    public boolean isOperatorSupported(String operatorName, Integer version) {
        return translatorRegistry.findTranslator(operatorName, version).isPresent();
    }

    /**
     * Get all supported operator names (for quick reference)
     */
    public List<String> getSupportedOperatorNames() {
        return allTranslators.stream()
                .map(OperatorTranslator::getOperatorName)
                .distinct()
                .sorted()
                .toList();
    }

    private String extractContext(String operatorName) {
        if (operatorName.startsWith("customer.")) {
            return "customer";
        } else if (operatorName.startsWith("order.")) {
            return "order";
        } else if (operatorName.startsWith("time.")) {
            return "time";
        } else {
            return "unknown";
        }
    }

    private String generateDescription(String operatorName) {
        switch (operatorName) {
            case "customer.segment.in":
                return "Check if customer segment is in specified list";
            case "customer.tier.equals":
                return "Check if customer tier equals specified value";
            case "customer.loyalty.points.gte":
                return "Check if customer loyalty points >= specified threshold";
            case "customer.usage.count.lt":
                return "Check if customer usage count < specified limit for voucher";
            case "order.amount.gte":
                return "Check if order amount >= specified threshold";
            case "order.total.gte":
                return "Check if order total >= specified threshold";
            case "order.items.count.gte":
                return "Check if order items count >= specified threshold";
            case "order.channel.in":
                return "Check if order channel is in specified list";
            case "order.item.category.in":
                return "Check if any order item category is in specified list";
            case "time.window.active":
                return "Check if current time is within specified time window policy";
            default:
                return "Custom operator: " + operatorName;
        }
    }
}