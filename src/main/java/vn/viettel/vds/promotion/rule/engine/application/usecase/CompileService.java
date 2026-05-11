package vn.viettel.vds.promotion.rule.engine.application.usecase;

import com.promix.platform.outbox.spi.OutboxService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;
import vn.viettel.vds.promotion.engine.event.BundlePublishedEvent;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.TimeLinkEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileJobResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.in.CompileUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompileService implements CompileUseCase {

    private static final String ENGINE_TYPE_DROOLS = "drools";

    private static final String BUNDLE_PUBLISHED_TOPIC = "promotion_bundle_published";
    private static final String AGGREGATE_TYPE_BUNDLE = "Bundle";
    private static final String EVENT_TYPE_BUNDLE_PUBLISHED = "BundlePublished";

    private final BundleRepositoryPort bundleRepository;
    private final CompileJobRepositoryPort compileJobRepository;
    private final OutboxService outboxService;
    private final ObjectStoragePort objectStoragePort;
    private final EventPublisherPort eventPublisherPort;
    private final RuleEnginePort ruleEnginePort;

    @Value("${validation.engine.object-storage.store-type:s3}")
    private String objectStoreType;

    public CompileService(
            BundleRepositoryPort bundleRepository,
            CompileJobRepositoryPort compileJobRepository,
            OutboxService outboxService,
            ObjectStoragePort objectStoragePort,
            EventPublisherPort eventPublisherPort,
            @Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.bundleRepository = bundleRepository;
        this.compileJobRepository = compileJobRepository;
        this.outboxService = outboxService;
        this.objectStoragePort = objectStoragePort;
        this.eventPublisherPort = eventPublisherPort;
        this.ruleEnginePort = ruleEnginePort;
    }

    @Override
    public CompileResponse compile(CompileRequest request) {
        // Check for existing job (idempotency)
        String jobId = generateJobId(request.getRuleId(), request.getVersion());
        Optional<CompileJobEntity> existingJob = compileJobRepository.findByRuleIdAndTargetVersion(
                request.getRuleId(), request.getVersion());

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
                    request.getRuleId(),
                    request.getVersion(),
                    request.getNodes(),
                    request.getOperatorsFingerprint(),
                    ENGINE_TYPE_DROOLS
            );

            // Map timeLinks from CompileRequest to CompileInput
            if (request.getTimeLinks() != null && !request.getTimeLinks().isEmpty()) {
                List<RuleEnginePort.TimeLink> timeLinkInputs = request.getTimeLinks().stream()
                        .map(tl -> new RuleEnginePort.TimeLink(tl.getPolicyId(), tl.getMode(), tl.getData()))
                        .toList();
                compileInput.setTimeLinks(timeLinkInputs);
            }

            RuleEnginePort.CompileResult result = ruleEnginePort.compile(compileInput);

            // Store artifact in object storage
            String artifactKey = generateArtifactKey(result.getBundleHash());
            objectStoragePort.store(artifactKey, result.getArtifactBytes());

            // Bundle hash is deterministic: reuse existing bundle if the same
            // hash was already persisted to avoid optimistic-locking conflicts
            // on re-compile of identical DRL content.
            BundleEntity bundle = bundleRepository.findById(result.getBundleHash())
                    .orElseGet(() -> bundleRepository.save(createBundle(request, result, artifactKey)));

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
            List<String> logMessages = job.getLogs(); // Already strings
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
        compileJob.getLogs().addAll(logs);
    }

    private void handleCompilationFailure(CompileJobEntity compileJob, Exception e) {
        compileJob.setStatus(CompileJobEntity.JobStatus.FAILED);
        compileJob.setCompletedAt(Instant.now());

        String errorMessage = getErrorMessage(e);
        compileJob.getErrors().add(errorMessage);
        compileJob.getLogs().add("ERROR: " + errorMessage);

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
    public List<CompileJobResponse> getCompileJobs(String ruleId, String status,
                                                   String from, String to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CompileJobEntity> jobs;

        if (status != null && from != null && to != null) {
            try {
                Instant fromInstant = Instant.parse(from);
                Instant toInstant = Instant.parse(to);
                CompileJobEntity.JobStatus jobStatus = CompileJobEntity.JobStatus.valueOf(status.toUpperCase());
                jobs = compileJobRepository.findByRuleIdAndStatusAndRequestedAtBetween(
                        ruleId, jobStatus, fromInstant, toInstant, pageable);
            } catch (DateTimeParseException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid date format or status: " + e.getMessage());
            }
        } else if (ruleId != null) {
            jobs = compileJobRepository.findByRuleId(ruleId, pageable);
        } else {
            jobs = compileJobRepository.findAll(pageable);
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

    private String generateJobId(String ruleId, Integer version) {
        return String.format("pj_%s_v%d", ruleId, version);
    }

    private String generateArtifactKey(String bundleHash) {
        return String.format("bundles/%s.kjar", bundleHash.replace("sha256:", ""));
    }

    private CompileJobEntity createCompileJob(String jobId, CompileRequest request) {
        CompileJobEntity job = new CompileJobEntity();
        job.setId(jobId);
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
                    .collect(Collectors.toCollection(ArrayList::new));
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
        BundlePublishedEvent event = new BundlePublishedEvent(
                request.getRuleId(), request.getVersion(), null, bundleHash);

        outboxService.createEvent(
                AGGREGATE_TYPE_BUNDLE,
                bundleHash,
                EVENT_TYPE_BUNDLE_PUBLISHED,
                event,
                BUNDLE_PUBLISHED_TOPIC,
                null,
                BundlePublishedEvent.class
        );

        BundleCacheInvalidationEvent cacheInvalidationEvent = new BundleCacheInvalidationEvent(
                bundleHash,
                request.getRuleId(),
                request.getVersion(),
                BundleCacheInvalidationEvent.InvalidationType.BUNDLE_UPDATED,
                null
        );
        eventPublisherPort.publishCacheInvalidation(cacheInvalidationEvent);
    }

    private CompileResponse mapToCompileResponse(BundleEntity bundle, List<String> logs) {
        CompileResponse response = new CompileResponse();
        response.setBundleHash(bundle.getId());
        response.setSize(bundle.getArtifact().getSize());
        response.setLogs(logs);
        response.setDrlContent(bundle.getDrlContent());

        CompileResponse.Engine engine = new CompileResponse.Engine();
        engine.setType(bundle.getEngine().getType());
        engine.setDroolsVersion(bundle.getEngine().getDroolsVersion());
        response.setEngine(engine);

        return response;
    }

    private CompileJobResponse mapToCompileJobResponse(CompileJobEntity job) {
        List<CompileJobResponse.LogEntry> logs = job.getLogs().stream()
                .map(log -> new CompileJobResponse.LogEntry("INFO", log, null))
                .toList();

        return new CompileJobResponse(
                job.getId(),
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