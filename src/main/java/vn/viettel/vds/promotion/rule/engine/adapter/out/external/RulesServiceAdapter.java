package vn.viettel.vds.promotion.rule.engine.adapter.out.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RulesServicePort;
import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class RulesServiceAdapter implements RulesServicePort {

    private static final Logger logger = LoggerFactory.getLogger(RulesServiceAdapter.class);
    private static final String DEFAULT_VERSION = "1.0.0";

    @Override
    @Cacheable(value = "rule-bundles", key = "#candidate.type + '_' + #candidate.code")
    public RuleBundle getRuleBundle(Candidate candidate) {
        try {
            logger.info("Fetching rule bundle for candidate: {} {}", candidate.getType(), candidate.getCode());

            // For now, return a mock rule bundle since external service might not be available
            String mockBundleHash = generateMockBundleHash(candidate);
            byte[] mockKieModuleBytes = createMockKieModule(candidate);

            return new RuleBundle(
                    mockBundleHash,
                    mockKieModuleBytes,
                    DEFAULT_VERSION,
                    DEFAULT_VERSION,
                    DEFAULT_VERSION
            );

        } catch (Exception e) {
            logger.error("Failed to fetch rule bundle for candidate: {} {}", candidate.getType(), candidate.getCode(), e);
            return null;
        }
    }

    private String generateMockBundleHash(Candidate candidate) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = candidate.getType() + ":" + candidate.getCode();
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "mock-hash-" + candidate.getType() + "-" + candidate.getCode();
        }
    }

    private byte[] createMockKieModule(Candidate candidate) {
        String mockDrlContent = String.format("""
                        package vn.viettel.validation.rules.%s
                        
                        import vn.viettel.vds.promotion.rule.engine.domain.model.*
                        
                        global Candidate candidate
                        
                        rule "Validate %s %s"
                        when
                            $customer : Customer()
                            $order : Order(total >= 100000)
                            $result : ValidationResult()
                        then
                            $result.setMatched(true);
                            $result.setMessage("Validation passed for %s %s");
                        end
                        
                        rule "Reject Low Order Value for %s %s"
                        when
                            $customer : Customer()
                            $order : Order(total < 100000)
                            $result : ValidationResult()
                        then
                            $result.setMatched(false);
                            $result.setMessage("Order total too low for %s %s");
                        end
                        """,
                candidate.getType(),
                candidate.getType(), candidate.getCode(),
                candidate.getType(), candidate.getCode(),
                candidate.getType(), candidate.getCode(),
                candidate.getType(), candidate.getCode()
        );

        return mockDrlContent.getBytes();
    }

}