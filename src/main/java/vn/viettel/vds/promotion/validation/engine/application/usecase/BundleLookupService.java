package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.engine.event.WarmupRequestedEvent;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.BundleSubjectIndexEntity;
import vn.viettel.vds.promotion.validation.engine.application.dto.BundleMetadataResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.WarmupRequest;
import vn.viettel.vds.promotion.validation.engine.application.port.in.BundleLookupUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.*;
import vn.viettel.vds.promotion.validation.engine.domain.exception.BundleNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BundleLookupService implements BundleLookupUseCase {

    @Autowired
    private BundleRepositoryPort bundleRepository;

    @Autowired
    private BundleSubjectIndexRepositoryPort bundleSubjectIndexRepository;

    @Autowired
    @Qualifier("droolsRuleEngineAdapter")
    private RuleEnginePort ruleEnginePort;

    @Autowired
    private ObjectStoragePort objectStoragePort;

    @Autowired
    private EventPublisherPort eventPublisherPort;

    @Override
    public BundleMetadataResponse getBundleMetadata(String bundleHash) {
        Optional<BundleEntity> bundle = bundleRepository.findById(bundleHash);
        if (bundle.isEmpty()) {
            throw new IllegalArgumentException("Bundle not found: " + bundleHash);
        }

        return mapToBundleMetadataResponse(bundle.get());
    }

    @Override
    public LatestBundleResponse getLatestBundle(String tenantId, String subjectType, String subjectKey) {
        Optional<BundleSubjectIndexEntity> subjectIndex = bundleSubjectIndexRepository
                .findByTenantIdAndSubjectTypeAndSubjectKey(tenantId, subjectType, subjectKey);

        if (subjectIndex.isEmpty()) {
            throw new IllegalArgumentException("Subject not mapped: " + tenantId + "/" + subjectType + "/" + subjectKey);
        }

        BundleSubjectIndexEntity index = subjectIndex.get();
        Optional<BundleEntity> bundle = bundleRepository.findById(index.getBundleHash());

        if (bundle.isEmpty()) {
            throw new IllegalStateException("Bundle not found for mapped subject: " + index.getBundleHash());
        }

        return mapToLatestBundleResponse(index, bundle.get());
    }

    @Override
    @Transactional
    public void warmupBundles(WarmupRequest request) {
        for (String bundleHash : request.getBundleHashes()) {
            warmupBundle(request.getTenantId(), bundleHash);
        }
    }

    private void warmupBundle(String tenantId, String bundleHash) {
        // Check if bundle exists
        if (!bundleRepository.existsById(bundleHash)) {
            throw new IllegalArgumentException("Bundle not found: " + bundleHash);
        }

        // Check if already cached
        if (ruleEnginePort.isBundleCached(bundleHash)) {
            return; // Already warmed up
        }

        // Retrieve artifact from object storage
        BundleEntity bundle = bundleRepository.findById(bundleHash).orElseThrow();
        String artifactKey = bundle.getArtifact().getKey();

        Optional<byte[]> artifactBytes = objectStoragePort.retrieve(artifactKey);
        if (artifactBytes.isEmpty()) {
            throw new IllegalStateException("Artifact not found in storage: " + artifactKey);
        }

        // Warm up in rule engine
        ruleEnginePort.warmupBundle(bundleHash, artifactBytes.get());

        // Publish warmup requested event
        WarmupRequestedEvent event = new WarmupRequestedEvent(tenantId, bundleHash);
        eventPublisherPort.publishWarmupRequested(event);
    }

    private BundleMetadataResponse mapToBundleMetadataResponse(BundleEntity bundle) {
        BundleMetadataResponse response = new BundleMetadataResponse();
        response.setTenantId(bundle.getTenantId());
        response.setRuleId(bundle.getRuleId());
        response.setRuleVersion(bundle.getRuleVersion());
        response.setOperatorsFingerprint(bundle.getOperatorsFingerprint());

        // Map limits
        if (bundle.getLimits() != null) {
            BundleMetadataResponse.Limits limits = new BundleMetadataResponse.Limits();
            limits.setPerCustomer(bundle.getLimits().getPerCustomer());
            limits.setPerDay(bundle.getLimits().getPerDay());
            response.setLimits(limits);
        }

        // Map timeLinks
        if (bundle.getTimeLinks() != null) {
            List<BundleMetadataResponse.TimeLink> timeLinks = bundle.getTimeLinks().stream()
                    .map(tl -> new BundleMetadataResponse.TimeLink(tl.getPolicyId(), tl.getMode()))
                    .toList();
            response.setTimeLinks(timeLinks);
        }

        // Map engine
        if (bundle.getEngine() != null) {
            BundleMetadataResponse.Engine engine = new BundleMetadataResponse.Engine();
            engine.setType(bundle.getEngine().getType());
            engine.setCompilerId(bundle.getEngine().getCompilerId());
            engine.setDroolsVersion(bundle.getEngine().getDroolsVersion());
            response.setEngine(engine);
        }

        return response;
    }

    private LatestBundleResponse mapToLatestBundleResponse(BundleSubjectIndexEntity index, BundleEntity bundle) {
        LatestBundleResponse response = new LatestBundleResponse();
        response.setRuleId(index.getRuleId());
        response.setRuleVersion(index.getRuleVersion());
        response.setAssignmentVersion(index.getAssignmentVersion());
        response.setBundleHash(index.getBundleHash());

        // Map limits from bundle
        if (bundle.getLimits() != null) {
            LatestBundleResponse.Limits limits = new LatestBundleResponse.Limits();
            limits.setPerCustomer(bundle.getLimits().getPerCustomer());
            limits.setPerDay(bundle.getLimits().getPerDay());
            response.setLimits(limits);
        }

        // Map timeLinks from bundle
        if (bundle.getTimeLinks() != null) {
            List<LatestBundleResponse.TimeLink> timeLinks = bundle.getTimeLinks().stream()
                    .map(tl -> new LatestBundleResponse.TimeLink(tl.getPolicyId(), tl.getMode()))
                    .toList();
            response.setTimeLinks(timeLinks);
        }

        return response;
    }

    @Override
    public String getDrlContent(String bundleHash) {
        Optional<BundleEntity> bundle = bundleRepository.findById(bundleHash);
        if (bundle.isEmpty()) {
            throw new BundleNotFoundException(bundleHash);
        }

        String drlContent = bundle.get().getDrlContent();
        if (drlContent == null || drlContent.isEmpty()) {
            throw new IllegalStateException("DRL content not available for bundle: " + bundleHash);
        }

        return drlContent;
    }
}