package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.time.Instant;

public class Rule {
    private String id;
    private String name;
    private String version;
    private String drlText;
    private boolean enabled;
    private Instant updatedAt;

    public Rule() {
    }

    public Rule(String name, String version, String drlText) {
        this.name = name;
        this.version = version;
        this.drlText = drlText;
        this.enabled = true;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDrlText() {
        return drlText;
    }

    public void setDrlText(String drlText) {
        this.drlText = drlText;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}