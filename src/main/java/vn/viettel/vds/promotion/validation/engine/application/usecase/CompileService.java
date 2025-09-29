package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.Bundle;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.CompileJob;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.OutboxEvent;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileJobResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.CompileUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompileService implements CompileUseCase {

    @Autowired
    private BundleRepositoryPort bundleRepository;

    @Autowired
    private CompileJobRepositoryPort compileJobRepository;

    @Autowired
    private OutboxEventRepositoryPort outboxEventRepository;

    @Autowired
    private ObjectStoragePort objectStoragePort;

    @Autowired
    private EventPublisherPort eventPublisherPort;

    @Autowired
    @Qualifier("droolsRuleEngineAdapter")
    private RuleEnginePort ruleEnginePort;

    @Override
    public CompileResponse compile(CompileRequest request) {
        // Check for existing job (idempotency)
        String jobId = generateJobId(request.getTenantId(), request.getRuleId(), request.getVersion());
        Optional<CompileJob> existingJob = compileJobRepository.findByTenantIdAndRuleIdAndTargetVersion(
                request.getTenantId(), request.getRuleId(), request.getVersion());

        if (existingJob.isPresent()) {
            CompileJob job = existingJob.get();
            if (job.getStatus() == CompileJob.JobStatus.SUCCESS && job.getBundleHash() != null) {
                // Return existing successful compilation
                Optional<Bundle> bundle = bundleRepository.findById(job.getBundleHash());
                if (bundle.isPresent()) {
                    List<String> logMessages = job.getLogs().stream()
                            .map(CompileJob.LogEntry::getMsg)
                            .collect(Collectors.toList());
                    return mapToCompileResponse(bundle.get(), logMessages);
                }
            } else if (job.getStatus() == CompileJob.JobStatus.RUNNING) {
                throw new IllegalStateException("Compilation already in progress for " + jobId);
            }
        }

        // Create new compile job
        CompileJob compileJob = createCompileJob(jobId, request);
        compileJobRepository.save(compileJob);

        try {
            // Execute compilation
            RuleEnginePort.CompileInput compileInput = new RuleEnginePort.CompileInput(
                    request.getTenantId(),
                    request.getRuleId(),
                    request.getVersion(),
                    request.getNodes(),
                    request.getOperatorsFingerprint(),
                    request.getCompilerId()
            );

            RuleEnginePort.CompileResult result = ruleEnginePort.compile(compileInput);

            // Store artifact in object storage
            String artifactKey = generateArtifactKey(result.getBundleHash());
            objectStoragePort.store(artifactKey, result.getArtifactBytes());

            // Create bundle entity
            Bundle bundle = createBundle(request, result, artifactKey);
            bundleRepository.save(bundle);

            // Update compile job as successful
            compileJob.setStatus(CompileJob.JobStatus.SUCCESS);
            compileJob.setBundleHash(result.getBundleHash());
            compileJob.setCompletedAt(Instant.now());
            compileJob.getLogs().addAll(result.getLogs().stream()
                    .map(log -> new CompileJob.LogEntry("INFO", log, Instant.now()))
                    .collect(Collectors.toList()));
            compileJobRepository.save(compileJob);

            // Publish bundle published event
            publishBundlePublishedEvent(request, result.getBundleHash());

            return mapToCompileResponse(bundle, result.getLogs());

        } catch (Exception e) {
            // Update compile job as failed
            compileJob.setStatus(CompileJob.JobStatus.FAILED);
            compileJob.setCompletedAt(Instant.now());
            compileJob.getErrors().add(e.getMessage());
            compileJob.getLogs().add(new CompileJob.LogEntry("ERROR", e.getMessage(), Instant.now()));
            compileJobRepository.save(compileJob);

            throw new RuntimeException("Compilation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CompileJobResponse> getCompileJobs(String tenantId, String ruleId, String status,
                                                  String from, String to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CompileJob> jobs;

        if (status != null && from != null && to != null) {
            try {
                Instant fromInstant = Instant.parse(from);
                Instant toInstant = Instant.parse(to);
                CompileJob.JobStatus jobStatus = CompileJob.JobStatus.valueOf(status.toUpperCase());
                jobs = compileJobRepository.findByTenantIdAndRuleIdAndStatusAndRequestedAtBetween(
                        tenantId, ruleId, jobStatus, fromInstant, toInstant, pageable);
            } catch (DateTimeParseException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid date format or status: " + e.getMessage());
            }
        } else if (ruleId != null) {
            jobs = compileJobRepository.findByTenantIdAndRuleId(tenantId, ruleId, pageable);
        } else {
            jobs = compileJobRepository.findByTenantId(tenantId, pageable);
        }

        return jobs.getContent().stream()
                .map(this::mapToCompileJobResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CompileJobResponse getCompileJob(String jobId) {
        Optional<CompileJob> job = compileJobRepository.findById(jobId);
        if (job.isEmpty()) {
            throw new IllegalArgumentException("Compile job not found: " + jobId);
        }
        return mapToCompileJobResponse(job.get());
    }

    private String generateJobId(String tenantId, String ruleId, Integer version) {
        return String.format("pj_%s_%s_v%d", tenantId, ruleId, version);
    }

    private String generateArtifactKey(String bundleHash) {
        return String.format("bundles/%s.kjar", bundleHash.replace("sha256:", ""));
    }

    private CompileJob createCompileJob(String jobId, CompileRequest request) {
        CompileJob job = new CompileJob();
        job.setId(jobId);
        job.setTenantId(request.getTenantId());
        job.setRuleId(request.getRuleId());
        job.setTargetVersion(request.getVersion());
        job.setStatus(CompileJob.JobStatus.RUNNING);
        job.setRequestedBy("system"); // TODO: Get from security context
        job.setRequestedAt(Instant.now());
        job.setOperatorsFingerprint(request.getOperatorsFingerprint());
        job.setEngine(new CompileJob.Engine(request.getCompilerId()));
        job.setLogs(List.of(new CompileJob.LogEntry("INFO", "Compilation started", Instant.now())));
        job.setErrors(List.of());
        return job;
    }

    private Bundle createBundle(CompileRequest request, RuleEnginePort.CompileResult result, String artifactKey) {
        Bundle bundle = new Bundle();
        bundle.setId(result.getBundleHash());
        bundle.setTenantId(request.getTenantId());
        bundle.setRuleId(request.getRuleId());
        bundle.setRuleVersion(request.getVersion());
        bundle.setOperatorsFingerprint(request.getOperatorsFingerprint());

        // Engine info
        Bundle.Engine engine = new Bundle.Engine();
        engine.setType("drools");
        engine.setCompilerId(request.getCompilerId());
        engine.setDroolsVersion(result.getDroolsVersion());
        bundle.setEngine(engine);

        // Convert limits and timeLinks
        if (request.getLimits() != null) {
            Bundle.Limits limits = new Bundle.Limits();
            limits.setPerCustomer(request.getLimits().getPerCustomer());
            limits.setPerDay(request.getLimits().getPerDay());
            bundle.setLimits(limits);
        }

        if (request.getTimeLinks() != null) {
            List<Bundle.TimeLink> timeLinks = request.getTimeLinks().stream()
                    .map(tl -> new Bundle.TimeLink(tl.getPolicyId(), tl.getMode()))
                    .collect(Collectors.toList());
            bundle.setTimeLinks(timeLinks);
        }

        // Artifact info
        Bundle.Artifact artifact = new Bundle.Artifact();
        artifact.setStore("s3"); // TODO: Make configurable
        artifact.setKey(artifactKey);
        artifact.setSize(result.getSize());
        bundle.setArtifact(artifact);

        bundle.setCreatedAt(Instant.now());

        // Source info
        if (request.getSource() != null) {
            Bundle.Source source = new Bundle.Source();
            source.setValidationRuleVersionId(request.getSource().getRuleVersionId());
            source.setSnapshotHash(request.getSource().getSnapshotHash());
            bundle.setSource(source);
        }

        return bundle;
    }

    private void publishBundlePublishedEvent(CompileRequest request, String bundleHash) {
        // Create outbox event
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId("ox_" + UUID.randomUUID().toString().replace("-", ""));
        outboxEvent.setTenantId(request.getTenantId());
        outboxEvent.setType(OutboxEvent.EventType.BUNDLE_PUBLISHED);
        outboxEvent.setPayload(Map.of(
                "ruleId", request.getRuleId(),
                "ruleVersion", request.getVersion(),
                "bundleHash", bundleHash
        ));
        outboxEvent.setStatus(OutboxEvent.EventStatus.PENDING);
        outboxEvent.setAttempts(0);
        outboxEvent.setCreatedAt(Instant.now());

        outboxEventRepository.save(outboxEvent);

        // Publish event
        EventPublisherPort.BundlePublishedEvent event = new EventPublisherPort.BundlePublishedEvent(
                request.getTenantId(), request.getRuleId(), request.getVersion(), null, bundleHash);
        eventPublisherPort.publishBundlePublished(event);
    }

    private CompileResponse mapToCompileResponse(Bundle bundle, List<String> logs) {
        CompileResponse response = new CompileResponse();
        response.setBundleHash(bundle.getId());
        response.setSize(bundle.getArtifact().getSize());
        response.setLogs(logs);

        CompileResponse.Engine engine = new CompileResponse.Engine();
        engine.setType(bundle.getEngine().getType());
        engine.setDroolsVersion(bundle.getEngine().getDroolsVersion());
        response.setEngine(engine);

        return response;
    }

    private CompileJobResponse mapToCompileJobResponse(CompileJob job) {
        CompileJobResponse response = new CompileJobResponse();
        response.setId(job.getId());
        response.setTenantId(job.getTenantId());
        response.setRuleId(job.getRuleId());
        response.setTargetVersion(job.getTargetVersion());
        response.setStatus(job.getStatus().name());
        response.setRequestedBy(job.getRequestedBy());
        response.setRequestedAt(job.getRequestedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setBundleHash(job.getBundleHash());
        response.setLogs(job.getLogs().stream()
                .map(log -> new CompileJobResponse.LogEntry(log.getLevel(), log.getMsg(), log.getTimestamp()))
                .collect(Collectors.toList()));
        response.setErrors(job.getErrors());
        return response;
    }
}