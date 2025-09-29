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

            String bundleHash = generateBundleHash(artifactBytes);

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
            logger.error("DRL compilation failed for rule: {}", ruleId, e);
            throw new CompilationException("DRL compilation failed: " + e.getMessage(), e);
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
            logger.error("Failed to create KIE container", e);
            throw new RuntimeException("Failed to create KIE container: " + e.getMessage(), e);
        }
    }

    public boolean validateArtifact(byte[] artifactBytes) {
        try {
            KieContainer container = createKieContainer(artifactBytes);
            KieSession session = container.newKieSession();
            session.dispose();
            container.dispose();
            return true;
        } catch (Exception e) {
            logger.warn("Artifact validation failed", e);
            return false;
        }
    }

    private String generateBundleHash(byte[] artifactBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(artifactBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "sha256:" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
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

        public String getBundleHash() { return bundleHash; }
        public byte[] getArtifactBytes() { return artifactBytes; }
        public Long getSize() { return size; }
        public List<String> getLogs() { return logs; }
        public String getDroolsVersion() { return droolsVersion; }
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

        public List<String> getLogs() { return logs; }
    }
}