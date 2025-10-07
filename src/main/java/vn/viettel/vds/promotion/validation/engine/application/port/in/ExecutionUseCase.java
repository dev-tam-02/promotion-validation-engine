package vn.viettel.vds.promotion.validation.engine.application.port.in;

import vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteRequest;
import vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse;

import java.util.List;
import java.util.Map;

public interface ExecutionUseCase {

    ExecuteResponse execute(ExecuteRequest request);

    BatchExecuteResponse executeBatch(BatchExecuteRequest request);

    public static class BatchExecuteRequest {
        private String tenantId;
        private ExecuteRequest.Bundle bundle;
        private List<TestCase> cases;

        public BatchExecuteRequest() {
        }

        public BatchExecuteRequest(String tenantId, ExecuteRequest.Bundle bundle, List<TestCase> cases) {
            this.tenantId = tenantId;
            this.bundle = bundle;
            this.cases = cases;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public ExecuteRequest.Bundle getBundle() {
            return bundle;
        }

        public void setBundle(ExecuteRequest.Bundle bundle) {
            this.bundle = bundle;
        }

        public List<TestCase> getCases() {
            return cases;
        }

        public void setCases(List<TestCase> cases) {
            this.cases = cases;
        }

        public static class TestCase {
            private String name;
            private Map<String, Object> context;

            public TestCase() {
            }

            public TestCase(String name, Map<String, Object> context) {
                this.name = name;
                this.context = context;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public Map<String, Object> getContext() {
                return context;
            }

            public void setContext(Map<String, Object> context) {
                this.context = context;
            }
        }
    }

    public static class BatchExecuteResponse {
        private List<TestResult> results;
        private Stats stats;

        public BatchExecuteResponse() {
        }

        public BatchExecuteResponse(List<TestResult> results, Stats stats) {
            this.results = results;
            this.stats = stats;
        }

        public List<TestResult> getResults() {
            return results;
        }

        public void setResults(List<TestResult> results) {
            this.results = results;
        }

        public Stats getStats() {
            return stats;
        }

        public void setStats(Stats stats) {
            this.stats = stats;
        }

        public static class TestResult {
            private String name;
            private ExecuteResponse response;

            public TestResult() {
            }

            public TestResult(String name, ExecuteResponse response) {
                this.name = name;
                this.response = response;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public ExecuteResponse getResponse() {
                return response;
            }

            public void setResponse(ExecuteResponse response) {
                this.response = response;
            }
        }

        public static class Stats {
            private Integer pass;
            private Integer fail;
            private Long totalTimeMs;

            public Stats() {
            }

            public Stats(Integer pass, Integer fail, Long totalTimeMs) {
                this.pass = pass;
                this.fail = fail;
                this.totalTimeMs = totalTimeMs;
            }

            public Integer getPass() {
                return pass;
            }

            public void setPass(Integer pass) {
                this.pass = pass;
            }

            public Integer getFail() {
                return fail;
            }

            public void setFail(Integer fail) {
                this.fail = fail;
            }

            public Long getTotalTimeMs() {
                return totalTimeMs;
            }

            public void setTotalTimeMs(Long totalTimeMs) {
                this.totalTimeMs = totalTimeMs;
            }
        }
    }
}