package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Maintains a single, shared {@link KieContainer} holding all registered DRL rules.
 *
 * <p>On each {@link #registerOrUpdate} call, a new versioned {@link ReleaseId} is minted,
 * the full DRL set (all existing rules + the new/updated rule) is compiled into a
 * {@link KieFileSystem}, and {@link KieContainer#updateToVersion(ReleaseId)} performs an
 * <em>atomic in-place swap</em> of the KieBase. Concurrent evaluations that already started
 * on the old KieSession continue undisturbed; new sessions pick up the updated rules.
 *
 * <p>This eliminates the ~1-5 s downtime that occurred in the Task 07b MVP implementation
 * where every rule change triggered a full {@code KieContainer} reconstruction (Task 09 V5).
 *
 * <h3>Thread-safety</h3>
 * <ul>
 *   <li>A {@link ReentrantLock} serialises concurrent calls to {@code registerOrUpdate}
 *       so only one Drools compile+swap runs at a time.</li>
 *   <li>{@code kieContainerRef} and {@code currentReleaseRef} are {@link AtomicReference}
 *       so readers never see a torn write.</li>
 *   <li>{@link KieContainer#updateToVersion} is thread-safe on the read side: existing
 *       stateless sessions snapshot the KieBase at creation time and are unaffected.</li>
 * </ul>
 *
 * <h3>Rollback</h3>
 * The last {@value #MAX_HISTORY_SIZE} version snapshots are retained in {@code versionHistory}
 * for optional manual rollback via an admin endpoint (future work).
 */
@Service
public class IncrementalKieContainerService {

    private static final Logger logger = LoggerFactory.getLogger(IncrementalKieContainerService.class);

    private static final String GROUP_ID = "com.promix.rules";
    private static final String ARTIFACT_ID = "pp-rules";
    private static final int MAX_HISTORY_SIZE = 3;

    private final KieServices ks = KieServices.Factory.get();
    private final AtomicReference<ReleaseId> currentReleaseRef = new AtomicReference<>();
    private final AtomicReference<KieContainer> kieContainerRef = new AtomicReference<>();
    private final AtomicInteger versionCounter = new AtomicInteger(0);
    private final ReentrantLock updateLock = new ReentrantLock();
    private final Deque<VersionSnapshot> versionHistory = new ArrayDeque<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Register or update a DRL rule in the shared KieContainer using incremental update.
     *
     * <p>The caller is responsible for supplying {@code allCurrentDrls} — a snapshot of
     * <em>all currently registered</em> rules (ruleId → drl content) taken <em>before</em>
     * the new rule is saved to the registry. This method merges the new rule into that
     * snapshot and recompiles the combined set.
     *
     * @param ruleId         the rule identifier
     * @param drl            the new / updated DRL content
     * @param allCurrentDrls current registry snapshot (may or may not include {@code ruleId})
     * @return SHA-256 hex of the new DRL content, prefixed with {@code "sha256:"} (bundleHash)
     * @throws IncrementalCompileException if DRL compilation or container update fails
     */
    public String registerOrUpdate(String ruleId, String drl, Map<String, String> allCurrentDrls) {
        updateLock.lock();
        try {
            int newVersionNum = versionCounter.incrementAndGet();
            ReleaseId newRelease = ks.newReleaseId(GROUP_ID, ARTIFACT_ID, newVersionNum + ".0.0");

            logger.info("Incremental KieBase update: ruleId={}, candidateVersion={}", ruleId, newVersionNum);

            KieFileSystem kfs = buildKieFileSystem(ruleId, drl, allCurrentDrls, newRelease);
            KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
            Results buildResults = kb.getResults();

            if (buildResults.hasMessages(Message.Level.ERROR)) {
                // Release the version slot — it was never applied
                versionCounter.decrementAndGet();
                List<String> errorLogs = buildResults.getMessages(Message.Level.ERROR).stream()
                        .map(Message::getText)
                        .toList();
                throw new IncrementalCompileException(
                        "DRL compilation failed for rule '" + ruleId + "'", errorLogs);
            }

            // Atomic swap: first ever call → create container; subsequent → updateToVersion
            KieContainer existing = kieContainerRef.get();
            if (existing == null) {
                KieContainer newContainer = ks.newKieContainer(newRelease);
                kieContainerRef.set(newContainer);
                logger.info("Created initial shared KieContainer: release={}", newRelease);
            } else {
                Results updateResults = existing.updateToVersion(newRelease);
                if (updateResults != null && updateResults.hasMessages(Message.Level.ERROR)) {
                    versionCounter.decrementAndGet();
                    List<String> errorLogs = updateResults.getMessages(Message.Level.ERROR).stream()
                            .map(Message::getText)
                            .toList();
                    throw new IncrementalCompileException(
                            "KieContainer.updateToVersion failed for rule '" + ruleId + "'", errorLogs);
                }
                logger.info("Atomic KieBase swap via updateToVersion: ruleId={}, release={}", ruleId, newRelease);
            }

            currentReleaseRef.set(newRelease);
            addToHistory(newVersionNum, newRelease, ruleId, drl);

            String bundleHash = sha256Hex(drl);
            logger.info("Incremental update complete: ruleId={}, version={}, bundleHash={}",
                    ruleId, newVersionNum, bundleHash);
            return bundleHash;

        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Return the active shared {@link KieContainer}.
     * May be {@code null} if no rule has been registered yet.
     */
    public KieContainer getActiveContainer() {
        return kieContainerRef.get();
    }

    /**
     * Return the current {@link ReleaseId} in use.
     * May be {@code null} if no rule has been registered yet.
     */
    public ReleaseId getCurrentRelease() {
        return currentReleaseRef.get();
    }

    /**
     * Return the monotonic version number of the currently active KieBase (0 = uninitialised).
     */
    public int getCurrentVersion() {
        return versionCounter.get();
    }

    /**
     * Return a copy of the recent version history (up to last {@value #MAX_HISTORY_SIZE} entries).
     * Intended for diagnostic / rollback endpoints.
     */
    public List<VersionSnapshot> getVersionHistory() {
        updateLock.lock();
        try {
            return List.copyOf(versionHistory);
        } finally {
            updateLock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private KieFileSystem buildKieFileSystem(String ruleId, String drl,
                                              Map<String, String> allCurrentDrls,
                                              ReleaseId releaseId) {
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.generateAndWritePomXML(releaseId);

        // Deduplicate by DRL content hash: multiple ruleIds may share the same bundle
        // (identical DRL content with identical rule names). Writing the same DRL content
        // under different filenames causes Drools to reject with "Duplicate rule name".
        // Only the first occurrence of each unique DRL content is written to the KieFileSystem.
        Set<String> writtenContentHashes = new HashSet<>();

        // Write all existing rules that are NOT the one being updated/created
        for (Map.Entry<String, String> entry : allCurrentDrls.entrySet()) {
            if (!entry.getKey().equals(ruleId)) {
                String contentHash = sha256Hex(entry.getValue());
                if (writtenContentHashes.add(contentHash)) {
                    kfs.write("src/main/resources/rules/" + entry.getKey() + ".drl",
                            entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        // Write the new / updated rule only if its content is not already present
        String newContentHash = sha256Hex(drl);
        if (writtenContentHashes.add(newContentHash)) {
            kfs.write("src/main/resources/rules/" + ruleId + ".drl",
                    drl.getBytes(StandardCharsets.UTF_8));
        }

        return kfs;
    }

    private void addToHistory(int version, ReleaseId releaseId, String ruleId, String drl) {
        versionHistory.addLast(new VersionSnapshot(version, releaseId, ruleId, drl));
        while (versionHistory.size() > MAX_HISTORY_SIZE) {
            versionHistory.removeFirst();
        }
    }

    private String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return "sha256:" + hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * Immutable snapshot of a previously applied rule version.
     * Retained for rollback / auditing.
     */
    public record VersionSnapshot(int version, ReleaseId releaseId, String ruleId, String drl) {
    }

    /**
     * Thrown when Drools compilation or {@link KieContainer#updateToVersion} returns errors.
     * Domain-layer exception — callers in the application layer should wrap this into the
     * appropriate use-case exception before propagating to the API layer.
     */
    public static class IncrementalCompileException extends RuntimeException {
        private final List<String> compileLogs;

        public IncrementalCompileException(String message, List<String> compileLogs) {
            super(message);
            this.compileLogs = compileLogs != null ? List.copyOf(compileLogs) : List.of();
        }

        public List<String> getCompileLogs() {
            return compileLogs;
        }
    }
}
