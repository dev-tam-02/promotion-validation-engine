package vn.viettel.vds.promotion.validation.engine.domain.service.execution;

import org.kie.api.event.rule.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ExecutionTracingService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionTracingService.class);

    public TracingAgendaEventListener createTracingListener(String executionId) {
        return new TracingAgendaEventListener(executionId);
    }

    public static class TracingAgendaEventListener implements AgendaEventListener {

        private final String executionId;
        private final List<ExecuteResponse.ExplainEntry> explainEntries;
        private final ConcurrentMap<String, Long> ruleExecutionTimes;
        private final ConcurrentMap<String, Integer> ruleFireCounts;

        public TracingAgendaEventListener(String executionId) {
            this.executionId = executionId;
            this.explainEntries = new ArrayList<>();
            this.ruleExecutionTimes = new ConcurrentHashMap<>();
            this.ruleFireCounts = new ConcurrentHashMap<>();
        }

        @Override
        public void matchCreated(MatchCreatedEvent event) {
            String ruleName = event.getMatch().getRule().getName();
            logger.debug("Rule match created: {} for execution: {}", ruleName, executionId);

            explainEntries.add(new ExecuteResponse.ExplainEntry(
                ruleName,
                "match_created",
                true
            ));
        }

        @Override
        public void matchCancelled(MatchCancelledEvent event) {
            String ruleName = event.getMatch().getRule().getName();
            logger.debug("Rule match cancelled: {} for execution: {}", ruleName, executionId);

            explainEntries.add(new ExecuteResponse.ExplainEntry(
                ruleName,
                "match_cancelled",
                false
            ));
        }

        @Override
        public void beforeMatchFired(BeforeMatchFiredEvent event) {
            String ruleName = event.getMatch().getRule().getName();
            logger.debug("Before rule fired: {} for execution: {}", ruleName, executionId);

            // Track when rule execution starts
            ruleExecutionTimes.put(ruleName + "_start", System.currentTimeMillis());
        }

        @Override
        public void afterMatchFired(AfterMatchFiredEvent event) {
            String ruleName = event.getMatch().getRule().getName();
            logger.debug("After rule fired: {} for execution: {}", ruleName, executionId);

            // Calculate execution time
            Long startTime = ruleExecutionTimes.get(ruleName + "_start");
            if (startTime != null) {
                long executionTime = System.currentTimeMillis() - startTime;
                ruleExecutionTimes.put(ruleName + "_duration", executionTime);
                logger.debug("Rule {} executed in {}ms", ruleName, executionTime);
            }

            // Track fire count
            ruleFireCounts.merge(ruleName, 1, Integer::sum);

            explainEntries.add(new ExecuteResponse.ExplainEntry(
                ruleName,
                "rule_fired",
                true
            ));
        }

        @Override
        public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
            logger.debug("Agenda group popped: {} for execution: {}",
                        event.getAgendaGroup().getName(), executionId);
        }

        @Override
        public void agendaGroupPushed(AgendaGroupPushedEvent event) {
            logger.debug("Agenda group pushed: {} for execution: {}",
                        event.getAgendaGroup().getName(), executionId);
        }

        @Override
        public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
            logger.debug("Before rule flow group activated: {} for execution: {}",
                        event.getRuleFlowGroup().getName(), executionId);
        }

        @Override
        public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
            logger.debug("After rule flow group activated: {} for execution: {}",
                        event.getRuleFlowGroup().getName(), executionId);
        }

        @Override
        public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
            logger.debug("Before rule flow group deactivated: {} for execution: {}",
                        event.getRuleFlowGroup().getName(), executionId);
        }

        @Override
        public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
            logger.debug("After rule flow group deactivated: {} for execution: {}",
                        event.getRuleFlowGroup().getName(), executionId);
        }

        public List<ExecuteResponse.ExplainEntry> getExplainEntries() {
            return new ArrayList<>(explainEntries);
        }

        public ConcurrentMap<String, Integer> getRuleFireCounts() {
            return new ConcurrentHashMap<>(ruleFireCounts);
        }

        public ConcurrentMap<String, Long> getRuleExecutionTimes() {
            return new ConcurrentHashMap<>(ruleExecutionTimes);
        }

        public int getTotalRulesFired() {
            return ruleFireCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        public long getTotalExecutionTime() {
            return ruleExecutionTimes.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("_duration"))
                .mapToLong(entry -> entry.getValue())
                .sum();
        }
    }

    public static class ExecutionMetrics {
        private final String executionId;
        private final List<ExecuteResponse.ExplainEntry> explainEntries;
        private final ConcurrentMap<String, Integer> ruleFireCounts;
        private final ConcurrentMap<String, Long> ruleExecutionTimes;
        private final int totalRulesFired;
        private final long totalExecutionTime;

        public ExecutionMetrics(TracingAgendaEventListener listener) {
            this.executionId = listener.executionId;
            this.explainEntries = listener.getExplainEntries();
            this.ruleFireCounts = listener.getRuleFireCounts();
            this.ruleExecutionTimes = listener.getRuleExecutionTimes();
            this.totalRulesFired = listener.getTotalRulesFired();
            this.totalExecutionTime = listener.getTotalExecutionTime();
        }

        public String getExecutionId() { return executionId; }
        public List<ExecuteResponse.ExplainEntry> getExplainEntries() { return explainEntries; }
        public ConcurrentMap<String, Integer> getRuleFireCounts() { return ruleFireCounts; }
        public ConcurrentMap<String, Long> getRuleExecutionTimes() { return ruleExecutionTimes; }
        public int getTotalRulesFired() { return totalRulesFired; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
    }
}