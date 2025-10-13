package vn.viettel.vds.promotion.validation.engine.adapter.out.rules;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleRepositoryPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RuleEngineAdapter implements RuleEnginePort {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngineAdapter.class);
    private static final String DEFAULT_BUNDLE = "default";

    private final RuleRepositoryPort ruleRepositoryPort;
    // Cache for KieBase by bundleHash
    private final Cache<String, KieBase> kieBaseCache;
    private final ConcurrentHashMap<String, KieContainer> kieContainerCache = new ConcurrentHashMap<>();

    public RuleEngineAdapter(RuleRepositoryPort ruleRepositoryPort) {
        this.ruleRepositoryPort = ruleRepositoryPort;
        this.kieBaseCache = Caffeine.newBuilder()
                .maximumSize(100)
                .recordStats()
                .build();
    }

    private static String sanitizeFileName(String input) {
        return (input == null || input.isBlank()) ? "unnamed" : input.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    @PostConstruct
    public void init() {
        rebuildInternal();
    }

    @Override
    public ValidationResult executeRules(Customer customer, Order order, Candidate candidate, String bundleHash) {
        try {
            KieBase kieBase = kieBaseCache.getIfPresent(bundleHash);

            if (kieBase == null) {
                logger.warn("KieBase not found for bundleHash: {}, trying to load it", bundleHash);
                // Try to use default KieBase if specific bundle not found
                kieBase = kieBaseCache.getIfPresent(DEFAULT_BUNDLE);
                if (kieBase == null) {
                    return new ValidationResult(false, "No rules available for validation");
                }
            }

            StatelessKieSession session = kieBase.newStatelessKieSession();
            ValidationResult result = new ValidationResult();

            // Set candidate context for rules
            session.setGlobal("candidate", candidate);
            session.execute(Arrays.asList(customer, order, result));

            return result;
        } catch (Exception e) {
            logger.error("Error executing rules for bundleHash: {}", bundleHash, e);
            return new ValidationResult(false, "Rule execution error: " + e.getMessage());
        }
    }

    @Override
    public List<ValidationResult> executeBulkRules(Customer customer, Order order, List<Candidate> candidates) {
        List<ValidationResult> results = new ArrayList<>();

        // Group candidates by their bundle hash for optimized execution
        Map<String, List<Candidate>> candidatesByBundle = new HashMap<>();

        for (Candidate candidate : candidates) {
            String bundleHash = determineBundleHash(candidate);
            candidatesByBundle.computeIfAbsent(bundleHash, k -> new ArrayList<>()).add(candidate);
        }

        // Execute rules for each bundle group
        for (Map.Entry<String, List<Candidate>> entry : candidatesByBundle.entrySet()) {
            String bundleHash = entry.getKey();
            List<Candidate> bundleCandidates = entry.getValue();

            List<ValidationResult> bundleResults = executeBulkRulesForBundle(
                    customer, order, bundleCandidates, bundleHash);
            results.addAll(bundleResults);
        }

        return results;
    }

    private List<ValidationResult> executeBulkRulesForBundle(Customer customer, Order order,
                                                             List<Candidate> candidates, String bundleHash) {
        List<ValidationResult> results = new ArrayList<>();

        try {
            KieBase kieBase = kieBaseCache.getIfPresent(bundleHash);
            if (kieBase == null) {
                logger.warn("KieBase not found for bundleHash: {}, falling back to default", bundleHash);
                kieBase = kieBaseCache.getIfPresent(DEFAULT_BUNDLE);
                if (kieBase == null) {
                    // Return failed results for all candidates
                    for (int i = 0; i < candidates.size(); i++) {
                        results.add(new ValidationResult(false, "No rules available for validation"));
                    }
                    return results;
                }
            }

            StatelessKieSession session = kieBase.newStatelessKieSession();

            // Create a result for each candidate
            Map<String, ValidationResult> candidateResults = new HashMap<>();
            for (Candidate candidate : candidates) {
                ValidationResult result = new ValidationResult();
                result.setCandidateId(candidate.getCode() != null ? candidate.getCode() : candidate.getId());
                candidateResults.put(result.getCandidateId(), result);
                results.add(result);
            }

            // Prepare all facts for batch execution
            List<Object> facts = new ArrayList<>();
            facts.add(customer);
            facts.add(order);
            facts.addAll(candidates);
            facts.addAll(candidateResults.values());

            // Set global context
            session.setGlobal("candidateResults", candidateResults);

            // Execute all rules in one batch
            session.execute(facts);

        } catch (Exception e) {
            logger.error("Error executing bulk rules for bundleHash: {}", bundleHash, e);
            // Return error results for all candidates
            for (int i = 0; i < candidates.size(); i++) {
                results.add(new ValidationResult(false, "Rule execution error: " + e.getMessage()));
            }
        }

        return results;
    }

    private String determineBundleHash(Candidate candidate) {
        // In a real implementation, this would look up the bundle hash
        // For now, group by candidate type
        return candidate.getType() + "-bundle";
    }

    @Override
    public void loadRuleBundle(String bundleHash, byte[] kieModuleBytes) {
        try {
            KieServices ks = KieServices.Factory.get();

            // Create a new KieFileSystem for this bundle
            KieFileSystem kfs = ks.newKieFileSystem();

            // Create module model
            KieModuleModel kModule = ks.newKieModuleModel();
            KieBaseModel kBase = kModule.newKieBaseModel(bundleHash + "KBase")
                    .setDefault(false);
            kBase.newKieSessionModel(bundleHash + "KSession")
                    .setType(KieSessionModel.KieSessionType.STATELESS)
                    .setDefault(false);

            kfs.writeKModuleXML(kModule.toXML());

            // If kieModuleBytes contains DRL content (as String), write it as DRL file
            String drlContent = new String(kieModuleBytes);
            if (drlContent.contains("package") && drlContent.contains("rule")) {
                String sourcePath = "rules/" + bundleHash + ".drl";
                kfs.write(sourcePath,
                        ks.getResources()
                                .newByteArrayResource(kieModuleBytes)
                                .setSourcePath(sourcePath));
            } else {
                // Try to load as actual KieModule bytes
                throw new IllegalArgumentException("Expected DRL content but received binary data");
            }

            // Build the rules
            KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();

            if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                String errorMessage = "Error building rule bundle: " + kieBuilder.getResults();
                logger.error("Error building rule bundle {}: {}", bundleHash, kieBuilder.getResults());
                throw new RuleBundleException(errorMessage);
            }

            // Create container with custom release ID
            ReleaseId releaseId = ks.newReleaseId("rules", bundleHash, "1.0.0");
            KieContainer container = ks.newKieContainer(releaseId);

            kieContainerCache.put(bundleHash, container);
            kieBaseCache.put(bundleHash, container.getKieBase(bundleHash + "KBase"));

            logger.info("Loaded rule bundle with hash: {}", bundleHash);
        } catch (RuleBundleException e) {
            // Re-throw RuleBundleException with original context
            throw e;
        } catch (Exception e) {
            throw new RuleBundleException(
                    String.format("Failed to load rule bundle with hash '%s'", bundleHash), e);
        }
    }

    @Override
    public boolean isRuleBundleLoaded(String bundleHash) {
        return kieBaseCache.getIfPresent(bundleHash) != null;
    }

    private void rebuildInternal() {
        KieServices ks = KieServices.Factory.get();

        // 1) Declare module model
        KieModuleModel kModule = ks.newKieModuleModel();

        // 2) Declare KieBase and KieSession
        KieBaseModel kBase = kModule.newKieBaseModel("rulesKBase")
                .setDefault(true);

        kBase.newKieSessionModel("statelessKSession")
                .setType(KieSessionModel.KieSessionType.STATELESS)
                .setDefault(true);

        // 3) Build KJAR in-memory
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(kModule.toXML());

        // Load default rules from database
        try {
            List<Rule> dbRules = ruleRepositoryPort.findEnabledRules();
            for (Rule r : dbRules) {
                if (r.getDrlText() == null || r.getDrlText().isBlank()) continue;
                String sourcePath = "rules/db/" + sanitizeFileName(r.getName()) + "-" + sanitizeFileName(r.getVersion()) + ".drl";
                kfs.write(
                        sourcePath,
                        ks.getResources()
                                .newByteArrayResource(r.getDrlText().getBytes())
                                .setSourcePath(sourcePath)
                );
            }
        } catch (Exception e) {
            logger.warn("Failed to load rules from database, using default rules", e);
            // Load default sample rules
            loadDefaultRules(ks, kfs);
        }

        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();

        if (kieBuilder.getResults() != null &&
                kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            logger.error("Error building rules: {}", kieBuilder.getResults());
            throw new IllegalStateException("Error building rules: " + kieBuilder.getResults());
        }

        // 4) Create container from default ReleaseId
        ReleaseId releaseId = ks.getRepository().getDefaultReleaseId();
        KieContainer defaultKieContainer = ks.newKieContainer(releaseId);

        // Cache the default KieBase
        kieBaseCache.put(DEFAULT_BUNDLE, defaultKieContainer.getKieBase());
        kieContainerCache.put(DEFAULT_BUNDLE, defaultKieContainer);

        logger.info("Default rule engine initialized successfully");
    }

    private void loadDefaultRules(KieServices ks, KieFileSystem kfs) {
        String defaultRule = """
                package vn.viettel.validation.rules
                
                import vn.viettel.vds.promotion.validation.engine.domain.model.*
                
                global Candidate candidate
                
                rule "Default Validation Rule"
                when
                    $customer : Customer()
                    $order : Order()
                    $result : ValidationResult()
                then
                    $result.setMatched(true);
                    $result.setMessage("Default validation passed");
                end
                """;

        kfs.write("rules/default.drl",
                ks.getResources()
                        .newByteArrayResource(defaultRule.getBytes())
                        .setSourcePath("rules/default.drl"));
    }

    // Additional methods required by RuleEnginePort interface
    @Override
    public CompileResult compile(CompileInput input) {
        // This adapter focuses on execution, not compilation
        throw new UnsupportedOperationException("Compilation not supported by this adapter. Use DroolsRuleEngineAdapter instead.");
    }

    @Override
    public ExecuteResponse execute(ExecuteInput input) {
        // This is a wrapper that adapts the legacy validation method to the new interface
        // In a real implementation, you'd want to refactor to use this method directly
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("NOT_IMPLEMENTED"));
        response.setExplain(List.of(new ExecuteResponse.ExplainEntry("legacy", "not_implemented", false)));

        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("drools-legacy");
        engine.setLatencyMs(0);
        engine.setCacheHit(false);
        response.setEngine(engine);

        return response;
    }

    @Override
    public List<ExecuteResponse> executeBatch(List<ExecuteInput> inputs) {
        return inputs.stream()
                .map(this::execute)
                .toList();
    }

    @Override
    public void warmupBundle(String bundleHash, byte[] artifactBytes) {
        // Use the existing loadRuleBundle method for warming up
        loadRuleBundle(bundleHash, artifactBytes);
    }

    @Override
    public boolean isBundleCached(String bundleHash) {
        return isRuleBundleLoaded(bundleHash);
    }
}