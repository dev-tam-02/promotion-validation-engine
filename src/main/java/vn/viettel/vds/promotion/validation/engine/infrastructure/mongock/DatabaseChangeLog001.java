package vn.viettel.vds.promotion.validation.engine.infrastructure.mongock;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * Initial database setup for validation-engine module
 */
@ChangeUnit(id = "validation-engine-initial-setup", order = "001", author = "system")
public class DatabaseChangeLog001 {

    @Execution
    public void createIndexes(MongoTemplate mongoTemplate) {
        // Create indexes for rules collection
        IndexOperations rulesIndexOps = mongoTemplate.indexOps("rules");
        rulesIndexOps.ensureIndex(new Index().on("tenantId", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_tenant_id"));
        rulesIndexOps.ensureIndex(new Index().on("ruleId", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_rule_id"));
        rulesIndexOps.ensureIndex(new Index().on("version", org.springframework.data.domain.Sort.Direction.DESC)
                .named("idx_version"));
        rulesIndexOps.ensureIndex(new Index().on("active", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_active"));

        // Create compound index for efficient rule lookup
        rulesIndexOps.ensureIndex(new Index()
                .on("tenantId", org.springframework.data.domain.Sort.Direction.ASC)
                .on("ruleId", org.springframework.data.domain.Sort.Direction.ASC)
                .on("version", org.springframework.data.domain.Sort.Direction.DESC)
                .named("idx_tenant_rule_version"));

        // Create indexes for bundles collection
        IndexOperations bundlesIndexOps = mongoTemplate.indexOps("bundles");
        bundlesIndexOps.ensureIndex(new Index().on("bundleId", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_bundle_id"));
        bundlesIndexOps.ensureIndex(new Index().on("tenantId", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_bundle_tenant"));
        bundlesIndexOps.ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_bundle_status"));
        bundlesIndexOps.ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC)
                .named("idx_bundle_created"));

        // Create indexes for bundle_subject_index collection
        IndexOperations bundleSubjectIndexOps = mongoTemplate.indexOps("bundle_subject_index");
        bundleSubjectIndexOps.ensureIndex(new Index().on("bundleId", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_bsi_bundle_id"));
        bundleSubjectIndexOps.ensureIndex(new Index().on("subject", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_bsi_subject"));

        // Create indexes for compile_jobs collection
        IndexOperations compileJobsIndexOps = mongoTemplate.indexOps("compile_jobs");
        compileJobsIndexOps.ensureIndex(new Index().on("jobId", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_job_id"));
        compileJobsIndexOps.ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_job_status"));
        compileJobsIndexOps.ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.DESC)
                .named("idx_job_created"));

        // Create indexes for engine_config collection
        IndexOperations engineConfigIndexOps = mongoTemplate.indexOps("engine_config");
        engineConfigIndexOps.ensureIndex(new Index().on("configKey", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_config_key"));
        engineConfigIndexOps.ensureIndex(new Index().on("tenantId", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_config_tenant"));

        // Create indexes for outbox_events collection
        IndexOperations outboxEventsIndexOps = mongoTemplate.indexOps("outbox_events");
        outboxEventsIndexOps.ensureIndex(new Index().on("processed", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_outbox_processed"));
        outboxEventsIndexOps.ensureIndex(new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_outbox_created"));
        outboxEventsIndexOps.ensureIndex(new Index().on("eventType", org.springframework.data.domain.Sort.Direction.ASC)
                .named("idx_outbox_type"));
    }

    @RollbackExecution
    public void rollbackIndexes(MongoTemplate mongoTemplate) {
        // Drop all custom indexes if needed during rollback
        mongoTemplate.indexOps("rules").dropIndex("idx_tenant_id");
        mongoTemplate.indexOps("rules").dropIndex("idx_rule_id");
        mongoTemplate.indexOps("rules").dropIndex("idx_version");
        mongoTemplate.indexOps("rules").dropIndex("idx_active");
        mongoTemplate.indexOps("rules").dropIndex("idx_tenant_rule_version");

        mongoTemplate.indexOps("bundles").dropIndex("idx_bundle_id");
        mongoTemplate.indexOps("bundles").dropIndex("idx_bundle_tenant");
        mongoTemplate.indexOps("bundles").dropIndex("idx_bundle_status");
        mongoTemplate.indexOps("bundles").dropIndex("idx_bundle_created");

        mongoTemplate.indexOps("bundle_subject_index").dropIndex("idx_bsi_bundle_id");
        mongoTemplate.indexOps("bundle_subject_index").dropIndex("idx_bsi_subject");

        mongoTemplate.indexOps("compile_jobs").dropIndex("idx_job_id");
        mongoTemplate.indexOps("compile_jobs").dropIndex("idx_job_status");
        mongoTemplate.indexOps("compile_jobs").dropIndex("idx_job_created");

        mongoTemplate.indexOps("engine_config").dropIndex("idx_config_key");
        mongoTemplate.indexOps("engine_config").dropIndex("idx_config_tenant");

        mongoTemplate.indexOps("outbox_events").dropIndex("idx_outbox_processed");
        mongoTemplate.indexOps("outbox_events").dropIndex("idx_outbox_created");
        mongoTemplate.indexOps("outbox_events").dropIndex("idx_outbox_type");
    }
}