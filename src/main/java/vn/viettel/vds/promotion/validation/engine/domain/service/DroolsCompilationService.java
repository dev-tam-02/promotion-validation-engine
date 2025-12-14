package vn.viettel.vds.promotion.validation.engine.domain.service;

import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DroolsCompilationService {

    private static final Logger logger = LoggerFactory.getLogger(DroolsCompilationService.class);

    private final KieServices kieServices;

    public DroolsCompilationService() {
        this.kieServices = KieServices.Factory.get();
    }

    public CompilationResult compileDrl(String tenantId, String ruleId, Integer version, String drlContent) {
        logger.info("Compiling DRL for rule: {}", ruleId);
        logger.debug("DRL Content:\n{}", drlContent);

        try {
            List<String> logs = new ArrayList<>();

            ReleaseId releaseId = kieServices.newReleaseId(
                    tenantId,
                    ruleId,
                    version != null ? version.toString() : "1.0.0"
            );

            KieFileSystem kfs = kieServices.newKieFileSystem();

            String resourcePath = "src/main/resources/rules/" + ruleId + ".drl";
            kfs.write(resourcePath, drlContent.getBytes(StandardCharsets.UTF_8));

            kfs.generateAndWritePomXML(releaseId);

            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
            kieBuilder.buildAll();

            Results results = kieBuilder.getResults();

            for (Message message : results.getMessages()) {
                String logMessage = String.format("[%s] %s", message.getLevel(), message.getText());
                logs.add(logMessage);

                if (message.getLevel() == Message.Level.ERROR) {
                    logger.error("Compilation error: {}", message.getText());
                } else if (message.getLevel() == Message.Level.WARNING) {
                    logger.warn("Compilation warning: {}", message.getText());
                } else {
                    logger.debug("Compilation info: {}", message.getText());
                }
            }

            if (results.hasMessages(Message.Level.ERROR)) {
                throw new CompilationException("DRL compilation failed with errors", logs);
            }

            InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModule();
            byte[] artifactBytes = kieModule.getBytes();

            // Generate deterministic bundleHash from DRL content instead of artifact bytes
            // This ensures consistency across compilations as Drools may add non-deterministic
            // metadata (timestamps, build numbers) to the .kjar artifact
            String bundleHash = generateBundleHashFromDrl(drlContent);

            logger.info("DRL compilation successful: bundleHash={}, size={} bytes",
                    bundleHash, artifactBytes.length);

            return new CompilationResult(
                    bundleHash,
                    artifactBytes,
                    (long) artifactBytes.length,
                    logs,
                    KieServices.Factory.get().getClass().getPackage().getImplementationVersion()
            );

        } catch (Exception e) {
            throw new CompilationException(
                    String.format("DRL compilation failed for rule: %s", ruleId), e);
        }
    }

    /**
     * Compile multiple DRL files into a single bundle.
     * Used for compiling both temporal DRL and validation rule DRL together.
     *
     * @param tenantId tenant identifier
     * @param bundleId bundle identifier (e.g., assignmentId)
     * @param version  bundle version
     * @param drlFiles map of filename to DRL content
     * @return compilation result with bundleHash and artifact bytes
     */
    public CompilationResult compileMultipleDrls(String tenantId, String bundleId, Integer version,
                                                 java.util.Map<String, String> drlFiles) {
        logger.info("Compiling multiple DRLs for bundle: {}, fileCount={}", bundleId, drlFiles.size());

        try {
            List<String> logs = new ArrayList<>();

            ReleaseId releaseId = kieServices.newReleaseId(
                    tenantId,
                    bundleId,
                    version != null ? version.toString() : "1.0.0"
            );

            KieFileSystem kfs = kieServices.newKieFileSystem();

            // Write each DRL file to KieFileSystem
            for (java.util.Map.Entry<String, String> entry : drlFiles.entrySet()) {
                String filename = entry.getKey();
                String drlContent = entry.getValue();
                String resourcePath = "src/main/resources/rules/" + filename;

                logger.debug("Writing DRL file: {} (size={} bytes)", resourcePath, drlContent.length());
                kfs.write(resourcePath, drlContent.getBytes(StandardCharsets.UTF_8));
            }

            kfs.generateAndWritePomXML(releaseId);

            KieBuilder kieBuilder = kieServices.newKieBuilder(kfs);
            kieBuilder.buildAll();

            Results results = kieBuilder.getResults();

            for (Message message : results.getMessages()) {
                String logMessage = String.format("[%s] %s", message.getLevel(), message.getText());
                logs.add(logMessage);

                if (message.getLevel() == Message.Level.ERROR) {
                    logger.error("Compilation error: {}", message.getText());
                } else if (message.getLevel() == Message.Level.WARNING) {
                    logger.warn("Compilation warning: {}", message.getText());
                } else {
                    logger.debug("Compilation info: {}", message.getText());
                }
            }

            if (results.hasMessages(Message.Level.ERROR)) {
                throw new CompilationException("DRL compilation failed with errors", logs);
            }

            InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModule();
            byte[] artifactBytes = kieModule.getBytes();

            // Generate deterministic bundleHash from all DRL contents combined
            String combinedDrl = String.join("\n\n", drlFiles.values());
            String bundleHash = generateBundleHashFromDrl(combinedDrl);

            logger.info("Multiple DRL compilation successful: bundleHash={}, size={} bytes, files={}",
                    bundleHash, artifactBytes.length, drlFiles.keySet());

            return new CompilationResult(
                    bundleHash,
                    artifactBytes,
                    (long) artifactBytes.length,
                    logs,
                    KieServices.Factory.get().getClass().getPackage().getImplementationVersion()
            );

        } catch (Exception e) {
            throw new CompilationException(
                    String.format("Multiple DRL compilation failed for bundle: %s", bundleId), e);
        }
    }

    public KieContainer createKieContainer(byte[] artifactBytes) {
        try {
            logger.debug("Creating KIE container from artifact bytes: {} bytes", artifactBytes.length);

            Resource resource = kieServices.getResources()
                    .newInputStreamResource(new ByteArrayInputStream(artifactBytes));

            KieRepository repository = kieServices.getRepository();
            KieModule kieModule = repository.addKieModule(resource);

            return kieServices.newKieContainer(kieModule.getReleaseId());

        } catch (Exception e) {
            throw new CompilationException("Failed to create KIE container", e);
        }
    }

    @SuppressWarnings("java:S2095")
    // Suppressing S2095 because KieSession and KieContainer do not implement AutoCloseable, and resources are properly disposed in the finally block.
    public boolean validateArtifact(byte[] artifactBytes) {
        KieContainer container = null;
        KieSession session = null;
        try {
            container = createKieContainer(artifactBytes);
            session = container.newKieSession();
            return true;
        } catch (Exception e) {
            logger.warn("Artifact validation failed", e);
            return false;
        } finally {
            if (session != null) {
                try {
                    session.dispose();
                } catch (Exception e) {
                    logger.warn("Failed to dispose KieSession", e);
                }
            }
            if (container != null) {
                try {
                    container.dispose();
                } catch (Exception e) {
                    logger.warn("Failed to dispose KieContainer", e);
                }
            }
        }
    }

    /**
     * Generate deterministic bundle hash from DRL content.
     * This ensures consistency across compilations as Drools compiler may add
     * non-deterministic metadata (timestamps, build numbers) to the .kjar artifact.
     *
     * @param drlContent The DRL content to hash
     * @return SHA-256 hash prefixed with "sha256:"
     */
    private String generateBundleHashFromDrl(String drlContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(drlContent.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new CompilationException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static class CompilationResult {
        private final String bundleHash;
        private final byte[] artifactBytes;
        private final Long size;
        private final List<String> logs;
        private final String droolsVersion;

        public CompilationResult(String bundleHash, byte[] artifactBytes, Long size,
                                 List<String> logs, String droolsVersion) {
            this.bundleHash = bundleHash;
            this.artifactBytes = artifactBytes;
            this.size = size;
            this.logs = logs;
            this.droolsVersion = droolsVersion;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public byte[] getArtifactBytes() {
            return artifactBytes;
        }

        public Long getSize() {
            return size;
        }

        public List<String> getLogs() {
            return logs;
        }

        public String getDroolsVersion() {
            return droolsVersion;
        }
    }

    public static class CompilationException extends RuntimeException {
        private final List<String> logs;

        public CompilationException(String message, List<String> logs) {
            super(message);
            this.logs = logs;
        }

        public CompilationException(String message, Throwable cause) {
            super(message, cause);
            this.logs = new ArrayList<>();
        }

        public List<String> getLogs() {
            return logs;
        }
    }
}