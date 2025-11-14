package vn.viettel.vds.promotion.validation.engine.application.dto;

import java.util.List;

public class CompileResponse {

    private String bundleHash;
    private Engine engine;
    private Long size;
    private List<String> logs;
    private String drlContent;

    public CompileResponse() {
    }

    public CompileResponse(String bundleHash, Engine engine, Long size, List<String> logs) {
        this.bundleHash = bundleHash;
        this.engine = engine;
        this.size = size;
        this.logs = logs;
    }

    // Getters and Setters
    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public String getDrlContent() {
        return drlContent;
    }

    public void setDrlContent(String drlContent) {
        this.drlContent = drlContent;
    }

    // Nested class
    public static class Engine {
        private String type;
        private String droolsVersion;

        public Engine() {
        }

        public Engine(String type, String droolsVersion) {
            this.type = type;
            this.droolsVersion = droolsVersion;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDroolsVersion() {
            return droolsVersion;
        }

        public void setDroolsVersion(String droolsVersion) {
            this.droolsVersion = droolsVersion;
        }
    }
}