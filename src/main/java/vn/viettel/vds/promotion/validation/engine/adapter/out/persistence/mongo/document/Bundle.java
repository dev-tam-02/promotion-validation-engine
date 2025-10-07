package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "bundles")
@CompoundIndexes({
        @CompoundIndex(name = "byRuleVersion", def = "{'tenantId': 1, 'ruleId': 1, 'ruleVersion': -1}"),
        @CompoundIndex(name = "byHash", def = "{'_id': 1}", unique = true)
})
public class Bundle {

    @Id
    private String id; // bundleHash (sha256:xxx)

    @Indexed
    private String tenantId;

    private String ruleId;

    private Integer ruleVersion;

    private String operatorsFingerprint;

    private Engine engine;

    private List<TimeLink> timeLinks;

    private Limits limits;

    private Artifact artifact;

    private Instant createdAt;

    private Source source;

    public Bundle() {
    }

    public Bundle(String id, String tenantId, String ruleId, Integer ruleVersion,
                  String operatorsFingerprint, Engine engine, List<TimeLink> timeLinks,
                  Limits limits, Artifact artifact, Instant createdAt, Source source) {
        this.id = id;
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.ruleVersion = ruleVersion;
        this.operatorsFingerprint = operatorsFingerprint;
        this.engine = engine;
        this.timeLinks = timeLinks;
        this.limits = limits;
        this.artifact = artifact;
        this.createdAt = createdAt;
        this.source = source;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getOperatorsFingerprint() {
        return operatorsFingerprint;
    }

    public void setOperatorsFingerprint(String operatorsFingerprint) {
        this.operatorsFingerprint = operatorsFingerprint;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public List<TimeLink> getTimeLinks() {
        return timeLinks;
    }

    public void setTimeLinks(List<TimeLink> timeLinks) {
        this.timeLinks = timeLinks;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    // Nested classes
    public static class Engine {
        private String type;
        private String compilerId;
        private String droolsVersion;

        public Engine() {
        }

        public Engine(String type, String compilerId, String droolsVersion) {
            this.type = type;
            this.compilerId = compilerId;
            this.droolsVersion = droolsVersion;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCompilerId() {
            return compilerId;
        }

        public void setCompilerId(String compilerId) {
            this.compilerId = compilerId;
        }

        public String getDroolsVersion() {
            return droolsVersion;
        }

        public void setDroolsVersion(String droolsVersion) {
            this.droolsVersion = droolsVersion;
        }
    }

    public static class TimeLink {
        private String policyId;
        private String mode;

        public TimeLink() {
        }

        public TimeLink(String policyId, String mode) {
            this.policyId = policyId;
            this.mode = mode;
        }

        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Limits {
        private Integer perCustomer;
        private Integer perDay;

        public Limits() {
        }

        public Limits(Integer perCustomer, Integer perDay) {
            this.perCustomer = perCustomer;
            this.perDay = perDay;
        }

        public Integer getPerCustomer() {
            return perCustomer;
        }

        public void setPerCustomer(Integer perCustomer) {
            this.perCustomer = perCustomer;
        }

        public Integer getPerDay() {
            return perDay;
        }

        public void setPerDay(Integer perDay) {
            this.perDay = perDay;
        }
    }

    public static class Artifact {
        private String store;
        private String key;
        private Long size;

        public Artifact() {
        }

        public Artifact(String store, String key, Long size) {
            this.store = store;
            this.key = key;
            this.size = size;
        }

        public String getStore() {
            return store;
        }

        public void setStore(String store) {
            this.store = store;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }
    }

    public static class Source {
        private String validationRuleVersionId;
        private String snapshotHash;

        public Source() {
        }

        public Source(String validationRuleVersionId, String snapshotHash) {
            this.validationRuleVersionId = validationRuleVersionId;
            this.snapshotHash = snapshotHash;
        }

        public String getValidationRuleVersionId() {
            return validationRuleVersionId;
        }

        public void setValidationRuleVersionId(String validationRuleVersionId) {
            this.validationRuleVersionId = validationRuleVersionId;
        }

        public String getSnapshotHash() {
            return snapshotHash;
        }

        public void setSnapshotHash(String snapshotHash) {
            this.snapshotHash = snapshotHash;
        }
    }
}