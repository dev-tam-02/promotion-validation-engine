package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.engine.event.BundlePublishedEvent;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.*;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileJobResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.CompileUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompileService implements CompileUseCase {

    private static final String ENGINE_TYPE_DROOLS = "drools";

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

    @Value("${validation.engine.object-storage.store-type:s3}")
    private String objectStoreType;

    @Override
    public CompileResponse compile(CompileRequest request) {
        // Check for existing job (idempotency)
        String jobId = generateJobId(request.getTenantId(), request.getRuleId(), request.getVersion());
        Optional<CompileJobEntity> existingJob = compileJobRepository.findByTenantIdAndRuleIdAndTargetVersion(
                request.getTenantId(), request.getRuleId(), request.getVersion());

        CompileResponse existingResult = handleExistingJob(existingJob, jobId);
        if (existingResult != null) {
            return existingResult;
        }

        // Create new compile job
        CompileJobEntity compileJob = createCompileJob(jobId, request);
        compileJobRepository.save(compileJob);

        try {
            // Execute compilation
            RuleEnginePort.CompileInput compileInput = new RuleEnginePort.CompileInput(
                    request.getTenantId(),
                    request.getRuleId(),
                    request.getVersion(),
                    request.getNodes(),
                    request.getOperatorsFingerprint(),
                    ENGINE_TYPE_DROOLS
            );

            RuleEnginePort.CompileResult result = ruleEnginePort.compile(compileInput);

            // Store artifact in object storage
            String artifactKey = generateArtifactKey(result.getBundleHash());
            objectStoragePort.store(artifactKey, result.getArtifactBytes());

            // Create bundle entity
            BundleEntity bundle = createBundle(request, result, artifactKey);
            bundleRepository.save(bundle);

            return handleCompilationSuccess(compileJob, request, result, bundle);

        } catch (Exception e) {
            handleCompilationFailure(compileJob, e);
            throw new CompilationFailedException("Compilation failed: " + getErrorMessage(e), e);
        }
    }

    private CompileResponse handleExistingJob(Optional<CompileJobEntity> existingJob, String jobId) {
        if (existingJob.isEmpty()) {
            return null;
        }

        CompileJobEntity job = existingJob.get();
        if (job.getStatus() == CompileJobEntity.JobStatus.SUCCESS && job.getBundleHash() != null) {
            return buildResponseFromExistingJob(job);
        }

        if (job.getStatus() == CompileJobEntity.JobStatus.RUNNING) {
            throw new IllegalStateException("Compilation already in progress for " + jobId);
        }

        return null;
    }

    private CompileResponse buildResponseFromExistingJob(CompileJobEntity job) {
        Optional<BundleEntity> bundle = bundleRepository.findById(job.getBundleHash());
        if (bundle.isPresent()) {
            List<String> logMessages = job.getLogs().stream()
                    .map(LogEntryEntity::getMsg)
                    .toList();
            return mapToCompileResponse(bundle.get(), logMessages);
        }
        return null;
    }

    private CompileResponse handleCompilationSuccess(CompileJobEntity compileJob, CompileRequest request,
                                                      RuleEnginePort.CompileResult result, BundleEntity bundle) {
        // Update compile job as successful
        compileJob.setStatus(CompileJobEntity.JobStatus.SUCCESS);
        compileJob.setBundleHash(result.getBundleHash());
        compileJob.setCompletedAt(Instant.now());

        // Create new log entries
        addLogEntries(compileJob, result.getLogs());
        compileJobRepository.save(compileJob);

        // Publish bundle published event
        publishBundlePublishedEvent(request, result.getBundleHash());

        return mapToCompileResponse(bundle, result.getLogs());
    }

    private void addLogEntries(CompileJobEntity compileJob, List<String> logs) {
        for (String log : logs) {
            LogEntryEntity logEntry = new LogEntryEntity();
            logEntry.setLevel("INFO");
            logEntry.setMsg(log);
            logEntry.setTimestamp(Instant.now());
            logEntry.setCompileJob(compileJob);
            compileJob.getLogs().add(logEntry);
        }
    }

    private void handleCompilationFailure(CompileJobEntity compileJob, Exception e) {
        compileJob.setStatus(CompileJobEntity.JobStatus.FAILED);
        compileJob.setCompletedAt(Instant.now());

        String errorMessage = getErrorMessage(e);
        compileJob.getErrors().add(errorMessage);

        LogEntryEntity errorLog = new LogEntryEntity();
        errorLog.setLevel("ERROR");
        errorLog.setMsg(errorMessage);
        errorLog.setTimestamp(Instant.now());
        errorLog.setCompileJob(compileJob);
        compileJob.getLogs().add(errorLog);

        compileJobRepository.save(compileJob);
    }

    private String getErrorMessage(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            errorMessage = e.getClass().getSimpleName();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMessage += ": " + e.getCause().getMessage();
            }
        }
        return errorMessage;
    }

    @Override
    public List<CompileJobResponse> getCompileJobs(String tenantId, String ruleId, String status,
                                                   String from, String to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CompileJobEntity> jobs;

        if (status != null && from != null && to != null) {
            try {
                Instant fromInstant = Instant.parse(from);
                Instant toInstant = Instant.parse(to);
                CompileJobEntity.JobStatus jobStatus = CompileJobEntity.JobStatus.valueOf(status.toUpperCase());
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
                .toList();
    }

    @Override
    public CompileJobResponse getCompileJob(String jobId) {
        Optional<CompileJobEntity> job = compileJobRepository.findById(jobId);
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

    private CompileJobEntity createCompileJob(String jobId, CompileRequest request) {
        CompileJobEntity job = new CompileJobEntity();
        job.setId(jobId);
        job.setTenantId(request.getTenantId());
        job.setRuleId(request.getRuleId());
        job.setTargetVersion(request.getVersion());
        job.setStatus(CompileJobEntity.JobStatus.RUNNING);
        job.setRequestedBy("system");
        job.setRequestedAt(Instant.now());
        job.setOperatorsFingerprint(request.getOperatorsFingerprint());
        CompileJobEntity.EngineInfo engineInfo = new CompileJobEntity.EngineInfo();
        engineInfo.setCompilerId(ENGINE_TYPE_DROOLS);  // Default compiler ID
        job.setEngine(engineInfo);
        // Logs and errors will be added separately as they are separate entities
        // errors is already initialized as ArrayList in the entity, so don't override with immutable list
        return job;
    }

    private BundleEntity createBundle(CompileRequest request, RuleEnginePort.CompileResult result, String artifactKey) {
        BundleEntity bundle = new BundleEntity();
        bundle.setId(result.getBundleHash());
        bundle.setTenantId(request.getTenantId());
        bundle.setRuleId(request.getRuleId());
        bundle.setRuleVersion(request.getVersion());
        bundle.setOperatorsFingerprint(request.getOperatorsFingerprint());

        // Engine info
        BundleEntity.EngineInfo engineInfo = new BundleEntity.EngineInfo();
        engineInfo.setType(ENGINE_TYPE_DROOLS);
        engineInfo.setCompilerId(ENGINE_TYPE_DROOLS);  // Default compiler ID
        engineInfo.setDroolsVersion(result.getDroolsVersion());
        bundle.setEngine(engineInfo);

        // Convert limits and timeLinks
        if (request.getLimits() != null) {
            BundleEntity.Limits limits = new BundleEntity.Limits();
            limits.setPerCustomer(request.getLimits().getPerCustomer());
            limits.setPerDay(request.getLimits().getPerDay());
            bundle.setLimits(limits);
        }

        if (request.getTimeLinks() != null) {
            List<TimeLinkEntity> timeLinks = request.getTimeLinks().stream()
                    .map(tl -> {
                        TimeLinkEntity timeLink = new TimeLinkEntity();
                        timeLink.setPolicyId(tl.getPolicyId());
                        timeLink.setMode(tl.getMode());
                        timeLink.setBundle(bundle);
                        return timeLink;
                    })
                    .toList();
            bundle.setTimeLinks(timeLinks);
        }

        // Artifact info
        BundleEntity.Artifact artifact = new BundleEntity.Artifact();
        artifact.setStore(objectStoreType);
        artifact.setKey(artifactKey);
        artifact.setSize(result.getSize());
        bundle.setArtifact(artifact);

        bundle.setCreatedAt(Instant.now());

        // Set DRL content
        bundle.setDrlContent(result.getDrlContent());

        return bundle;
    }

    private void publishBundlePublishedEvent(CompileRequest request, String bundleHash) {
        // Create outbox event
        OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setId("ox_" + UUID.randomUUID().toString().replace("-", ""));
        outboxEvent.setTenantId(request.getTenantId());
        outboxEvent.setType(OutboxEventEntity.EventType.BUNDLE_PUBLISHED);
        Map<String, String> payload = new HashMap<>();
        payload.put("ruleId", request.getRuleId());
        payload.put("ruleVersion", String.valueOf(request.getVersion()));
        payload.put("bundleHash", bundleHash);
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus(OutboxEventEntity.EventStatus.PENDING);
        outboxEvent.setAttempts(0);
        outboxEvent.setCreatedAt(Instant.now());

        outboxEventRepository.save(outboxEvent);

        // Publish event
        BundlePublishedEvent event = new BundlePublishedEvent(
                request.getTenantId(), request.getRuleId(), request.getVersion(), null, bundleHash);
        eventPublisherPort.publishBundlePublished(event);
    }

    private CompileResponse mapToCompileResponse(BundleEntity bundle, List<String> logs) {
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

    private CompileJobResponse mapToCompileJobResponse(CompileJobEntity job) {
        List<CompileJobResponse.LogEntry> logs = job.getLogs().stream()
                .map(log -> new CompileJobResponse.LogEntry(log.getLevel(), log.getMsg(), log.getTimestamp()))
                .toList();

        return new CompileJobResponse(
                job.getId(),
                job.getTenantId(),
                job.getRuleId(),
                job.getTargetVersion(),
                job.getStatus().name(),
                job.getRequestedBy(),
                job.getRequestedAt(),
                job.getCompletedAt(),
                job.getBundleHash(),
                logs,
                job.getErrors()
        );
    }

    public static class CompilationFailedException extends RuntimeException {
        public CompilationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}