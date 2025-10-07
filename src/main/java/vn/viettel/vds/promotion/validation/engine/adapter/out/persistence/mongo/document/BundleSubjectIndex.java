package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "bundle_subject_index")
@CompoundIndexes({
        @CompoundIndex(name = "bySubject", def = "{'tenantId': 1, 'subject.type': 1, 'subject.key': 1}", unique = true),
        @CompoundIndex(name = "byRuleVer", def = "{'tenantId': 1, 'ruleId': 1, 'ruleVersion': -1}")
})
public class BundleSubjectIndex {

    @Id
    private String id; // tenantId|type|key format

    private String tenantId;

    private Subject subject;

    private String ruleId;

    private Integer ruleVersion;

    private Integer assignmentVersion;

    private String bundleHash;

    private Instant updatedAt;

    public BundleSubjectIndex() {
    }

    public BundleSubjectIndex(String id, String tenantId, Subject subject, String ruleId,
                              Integer ruleVersion, Integer assignmentVersion, String bundleHash,
                              Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.subject = subject;
        this.ruleId = ruleId;
        this.ruleVersion = ruleVersion;
        this.assignmentVersion = assignmentVersion;
        this.bundleHash = bundleHash;
        this.updatedAt = updatedAt;
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

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
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

    public Integer getAssignmentVersion() {
        return assignmentVersion;
    }

    public void setAssignmentVersion(Integer assignmentVersion) {
        this.assignmentVersion = assignmentVersion;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Nested class
    public static class Subject {
        private String type;
        private String key;

        public Subject() {
        }

        public Subject(String type, String key) {
            this.type = type;
            this.key = key;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}