package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "time_windows")
@Getter
@Setter
public class TimeWindowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "policy_id", nullable = false, length = 100, insertable = false, updatable = false)
    private String policyId;

    @Column(name = "days_of_week", length = 100)
    private String daysOfWeek; // Comma-separated: "MONDAY,TUESDAY,FRIDAY"

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "spans_midnight", nullable = false)
    private boolean spansMidnight = false;
}
