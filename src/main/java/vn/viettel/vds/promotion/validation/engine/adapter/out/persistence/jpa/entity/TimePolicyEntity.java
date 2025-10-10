package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_policies")
@Getter
@Setter
public class TimePolicyEntity {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id", nullable = false)
    private List<TimeWindowEntity> timeWindows = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
