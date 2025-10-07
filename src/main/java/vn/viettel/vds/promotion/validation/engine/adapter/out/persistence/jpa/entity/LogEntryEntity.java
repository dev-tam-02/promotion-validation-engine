package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "compile_job_logs")
@Getter
@Setter
public class LogEntryEntity extends BaseEntity {

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "msg", columnDefinition = "TEXT")
    private String msg;

    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "compile_job_id", length = 100)
    private String compileJobId;
}