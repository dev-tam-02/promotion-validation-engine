package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.EvaluateRuleResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.TraceEntry;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleRegistryPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
import vn.viettel.vds.promotion.rule.engine.domain.service.DroolsCompilationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.FactPreparationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Evaluates registered DRL rules against a provided fact map.
 *
 * <p>In SIMULATE mode a {@link SimulateAgendaEventListener} is attached to the stateless
 * KieSession. After execution:
 * <ul>
 *   <li>{@code matchedNodes} — Drools declaration IDs bound in rules that fired
 *       (each bound variable = one "matched node").</li>
 *   <li>{@code unmatchedNodes} — rule names whose matches were cancelled before firing.</li>
 * </ul>
 *
 * <p>Verdict:
 * <ul>
 *   <li>ALLOW — the Drools {@code result} global has {@code decision="ALLOW"} after execution.</li>
 *   <li>DENY  — any other outcome (default, explicit deny, or execution error).</li>
 * </ul>
 */
@Service
public class SimulateEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(SimulateEvaluationService.class);

    private final RuleRegistryPort ruleRegistry;
    private final DroolsCompilationService compilationService;
    private final FactPreparationService factPreparationService;
    private final KieSessionManager sessionManager;

    public SimulateEvaluationService(RuleRegistryPort ruleRegistry,
                                     DroolsCompilationService compilationService,
                                     FactPreparationService factPreparationService,
                                     KieSessionManager sessionManager) {
        this.ruleRegistry = ruleRegistry;
        this.compilationService = compilationService;
        this.factPreparationService = factPreparationService;
        this.sessionManager = sessionManager;
    }

    /**
     * Evaluate a list of rules against the provided facts.
     *
     * @param ruleIds      IDs of registered rules to evaluate
     * @param facts        flat fact map keyed by fact-type (order, customer, candidate, …)
     * @param simulateMode when true, attach tracing listener and populate trace + matched/unmatched
     * @return evaluation response
     */
    public EvaluateRuleResponse evaluate(List<String> ruleIds,
                                         Map<String, Object> facts,
                                         boolean simulateMode) {

        if (ruleIds == null || ruleIds.isEmpty()) {
            EvaluateRuleResponse resp = new EvaluateRuleResponse();
            resp.setVerdict("DENY");
            resp.setReasonCodes(List.of("NO_RULE_IDS"));
            resp.setTrace(List.of());
            resp.setMatchedNodes(List.of());
            resp.setUnmatchedNodes(List.of());
            return resp;
        }

        List<TraceEntry> allTrace = new ArrayList<>();
        List<String> allMatched = new ArrayList<>();
        List<String> allUnmatched = new ArrayList<>();
        List<String> allReasonCodes = new ArrayList<>();
        String verdict = "ALLOW";

        for (String ruleId : ruleIds) {
            Optional<RegisteredRule> registered = ruleRegistry.findById(ruleId);
            if (registered.isEmpty()) {
                log.warn("evaluate: ruleId={} not found in registry — marking DENY", ruleId);
                allUnmatched.add(ruleId);
                verdict = "DENY";
                allReasonCodes.add("RULE_NOT_REGISTERED");
                continue;
            }

            RegisteredRule rule = registered.get();
            log.info("evaluate: ruleId={}, bundleHash={}, simulate={}", ruleId, rule.getBundleHash(), simulateMode);

            try {
                SingleEvalResult result = executeSingle(rule, facts, simulateMode);

                if (simulateMode) {
                    allTrace.addAll(result.trace());
                    allMatched.addAll(result.matchedNodes());
                    allUnmatched.addAll(result.unmatchedNodes());
                }
                allReasonCodes.addAll(result.reasonCodes());

                if (!"ALLOW".equals(result.verdict())) {
                    verdict = "DENY";
                }

            } catch (Exception ex) {
                log.error("evaluate: execution failed for ruleId={}: {}", ruleId, ex.getMessage(), ex);
                verdict = "DENY";
                allReasonCodes.add("EXECUTION_ERROR");
                if (simulateMode) {
                    allUnmatched.add(ruleId);
                }
            }
        }

        EvaluateRuleResponse response = new EvaluateRuleResponse();
        response.setVerdict(verdict);
        response.setReasonCodes(allReasonCodes);
        response.setTrace(simulateMode ? allTrace : List.of());
        response.setMatchedNodes(simulateMode ? allMatched : List.of());
        response.setUnmatchedNodes(simulateMode ? allUnmatched : List.of());

        return response;
    }

    // ------------------------------------------------------------------
    // Private: execute a single rule bundle against the provided facts
    // ------------------------------------------------------------------

    private SingleEvalResult executeSingle(RegisteredRule rule,
                                           Map<String, Object> facts,
                                           boolean simulateMode) {

        KieContainer container = resolveContainer(rule);
        StatelessKieSession session = sessionManager.createStatelessSession(container);

        SimulateAgendaEventListener listener = null;
        if (simulateMode) {
            listener = new SimulateAgendaEventListener();
            session.addEventListener(listener);
        }

        List<Object> preparedFacts = factPreparationService.prepareFacts(facts);
        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);
        session.execute(preparedFacts);

        String verdict = "ALLOW".equalsIgnoreCase(result.getDecision()) ? "ALLOW" : "DENY";

        if (!simulateMode) {
            return new SingleEvalResult(verdict, List.of(), List.of(), List.of(), reasonCodes);
        }

        List<TraceEntry> trace = listener.buildTrace();
        List<String> matched = new ArrayList<>(listener.getMatchedNodes());
        List<String> unmatched = new ArrayList<>(listener.getUnmatchedNodes());

        return new SingleEvalResult(verdict, trace, matched, unmatched, reasonCodes);
    }

    /**
     * Resolve a KieContainer for the given registered rule.
     *
     * <p>Checks the session-manager cache first. On a cache miss, recompiles the stored DRL
     * to recreate the container — this can happen after a container TTL eviction.
     */
    private KieContainer resolveContainer(RegisteredRule rule) {
        String bundleHash = rule.getBundleHash();

        KieContainer cached = sessionManager.getCachedContainer(bundleHash);
        if (cached != null) {
            log.debug("resolveContainer: cache hit bundleHash={}", bundleHash);
            return cached;
        }

        // Cache miss — recompile from stored DRL and re-cache the container
        log.info("resolveContainer: cache miss bundleHash={}, recompiling from stored DRL", bundleHash);
        DroolsCompilationService.CompilationResult compiled =
                compilationService.compileDrl(rule.getRuleId(), 1, rule.getDrl());

        KieContainer container = compilationService.createKieContainer(compiled.getArtifactBytes());
        sessionManager.cacheContainer(bundleHash, container);
        return container;
    }

    // ------------------------------------------------------------------
    // Inner: Simulation-specific AgendaEventListener
    // ------------------------------------------------------------------

    /**
     * Captures Drools agenda events during SIMULATE execution.
     *
     * <ul>
     *   <li>{@code afterMatchFired}: each bound declaration variable in the match is a "matched
     *       node" → one {@link TraceEntry} per declaration (type=COND, result=true).</li>
     *   <li>{@code matchCancelled}: the rule name is recorded as an unmatched node
     *       → one TraceEntry with result=false.</li>
     * </ul>
     *
     * <p>One rule with two LHS patterns (e.g., {@code $order: OrderFact(…)} and
     * {@code $customer: CustomerFact(…)}) fires with two bound declarations, producing
     * two matched nodes: "$order" and "$customer".
     */
    static final class SimulateAgendaEventListener extends DefaultAgendaEventListener {

        private static final Logger llog = LoggerFactory.getLogger(SimulateAgendaEventListener.class);

        private final List<TraceEntry> rawEntries = new ArrayList<>();
        private final Set<String> matchedNodes = new LinkedHashSet<>();
        private final Set<String> unmatchedNodes = new LinkedHashSet<>();

        @Override
        public void afterMatchFired(AfterMatchFiredEvent event) {
            String ruleName = event.getMatch().getRule().getName();
            llog.debug("afterMatchFired: rule={}", ruleName);

            Iterable<String> declarations = event.getMatch().getDeclarationIds();
            boolean hadDeclarations = false;

            for (String decl : declarations) {
                hadDeclarations = true;
                String nodeId = decl;
                rawEntries.add(new TraceEntry(nodeId, "COND", stripDollar(decl), true, null));
                matchedNodes.add(nodeId);
                llog.debug("  matched declaration={}", nodeId);
            }

            if (!hadDeclarations) {
                // Rule with no bound variables — record the rule itself as one node
                rawEntries.add(new TraceEntry(ruleName, "GROUP", ruleName, true, null));
                matchedNodes.add(ruleName);
            }
        }

        @Override
        public void matchCancelled(MatchCancelledEvent event) {
            String ruleName = event.getMatch().getRule().getName();
            llog.debug("matchCancelled: rule={}", ruleName);
            rawEntries.add(new TraceEntry(ruleName, "COND", ruleName, false, null));
            unmatchedNodes.add(ruleName);
        }

        @Override
        public void matchCreated(MatchCreatedEvent event) {
            // handled via afterMatchFired / matchCancelled
        }

        List<TraceEntry> buildTrace() {
            return new ArrayList<>(rawEntries);
        }

        Set<String> getMatchedNodes() {
            return matchedNodes;
        }

        Set<String> getUnmatchedNodes() {
            return unmatchedNodes;
        }

        private String stripDollar(String declaration) {
            return (declaration != null && declaration.startsWith("$"))
                    ? declaration.substring(1)
                    : declaration;
        }
    }

    // ------------------------------------------------------------------
    // Inner: value holder
    // ------------------------------------------------------------------

    private record SingleEvalResult(String verdict,
                                    List<TraceEntry> trace,
                                    List<String> matchedNodes,
                                    List<String> unmatchedNodes,
                                    List<String> reasonCodes) {
    }
}
