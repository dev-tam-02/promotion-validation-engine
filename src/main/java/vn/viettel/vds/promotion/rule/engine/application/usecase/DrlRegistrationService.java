package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.application.port.in.RegisterDrlUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleRegistryPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;
import vn.viettel.vds.promotion.rule.engine.domain.service.DroolsCompilationService;

import java.time.Instant;
import java.util.Optional;

/**
 * Application service for DRL rule CRUD operations exposed at POST/PUT/DELETE /v1/rules.
 *
 * <p><b>POST idempotency</b>: If the same (ruleId, drl) pair is submitted again, the existing
 * bundleHash is returned without re-compiling. If ruleId exists with different DRL, a 409 conflict
 * is raised — callers must use PUT to update.
 *
 * <p><b>Persistence</b>: Delegates to {@link RuleRegistryPort}. MVP uses an in-memory store;
 * Task 09 V2 will swap in a JPA-backed adapter.
 *
 * <p><b>KieBase lifecycle</b>: Each successful compile is warmed up immediately via
 * {@link RuleEnginePort#warmupBundle(String, byte[])} so the bundle is ready for execution
 * without a cold-start round-trip to MinIO. Full KieBase rebuild per change (MVP).
 * Task 09 V5 will introduce incremental KieContainer.updateToVersion().
 */
@Service
public class DrlRegistrationService implements RegisterDrlUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DrlRegistrationService.class);

    private final DroolsCompilationService compilationService;
    private final RuleRegistryPort ruleRegistry;
    private final RuleEnginePort ruleEnginePort;

    public DrlRegistrationService(DroolsCompilationService compilationService,
                                  RuleRegistryPort ruleRegistry,
                                  @Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.compilationService = compilationService;
        this.ruleRegistry = ruleRegistry;
        this.ruleEnginePort = ruleEnginePort;
    }

    @Override
    public RegisterRuleResult register(String ruleId, String drl) {
        logger.info("Registering DRL rule: ruleId={}", ruleId);

        Optional<RegisteredRule> existing = ruleRegistry.findById(ruleId);
        if (existing.isPresent()) {
            RegisteredRule existingRule = existing.get();
            if (existingRule.hasSameDrl(drl)) {
                logger.info("Idempotent POST: rule already registered with identical DRL, ruleId={}, bundleHash={}",
                        ruleId, existingRule.getBundleHash());
                return new RegisterRuleResult(ruleId, existingRule.getBundleHash());
            }
            throw new DrlConflictException(
                    "Rule '" + ruleId + "' already exists with different DRL content. Use PUT /v1/rules/{id} to update.");
        }

        DroolsCompilationService.CompilationResult compiled = compileDrl(ruleId, drl);

        ruleEnginePort.warmupBundle(compiled.getBundleHash(), compiled.getArtifactBytes());

        RegisteredRule rule = new RegisteredRule(
                ruleId, drl, compiled.getBundleHash(), Instant.now(), Instant.now());
        ruleRegistry.save(rule);

        logger.info("Rule registered: ruleId={}, bundleHash={}", ruleId, compiled.getBundleHash());
        return new RegisterRuleResult(ruleId, compiled.getBundleHash());
    }

    @Override
    public RegisterRuleResult update(String ruleId, String drl) {
        logger.info("Updating DRL rule: ruleId={}", ruleId);

        RegisteredRule existing = ruleRegistry.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Rule not found: " + ruleId));

        DroolsCompilationService.CompilationResult compiled = compileDrl(ruleId, drl);

        ruleEnginePort.warmupBundle(compiled.getBundleHash(), compiled.getArtifactBytes());

        RegisteredRule updated = new RegisteredRule(
                ruleId, drl, compiled.getBundleHash(), existing.getRegisteredAt(), Instant.now());
        ruleRegistry.save(updated);

        logger.info("Rule updated: ruleId={}, newBundleHash={}", ruleId, compiled.getBundleHash());
        return new RegisterRuleResult(ruleId, compiled.getBundleHash());
    }

    @Override
    public void delete(String ruleId) {
        logger.info("Deleting DRL rule: ruleId={}", ruleId);

        if (ruleRegistry.findById(ruleId).isEmpty()) {
            throw new RuleNotFoundException("Rule not found: " + ruleId);
        }

        ruleRegistry.deleteById(ruleId);
        logger.info("Rule deleted: ruleId={}", ruleId);
    }

    /**
     * Compile DRL via Drools KieBuilder. Translates {@link DroolsCompilationService.CompilationException}
     * (DRL syntax errors) into {@link DrlCompileException} for clean exception boundary.
     */
    private DroolsCompilationService.CompilationResult compileDrl(String ruleId, String drl) {
        try {
            return compilationService.compileDrl(ruleId, 1, drl);
        } catch (DroolsCompilationService.CompilationException e) {
            String message = "DRL compilation failed for rule '" + ruleId + "': " + e.getMessage();
            logger.warn(message);
            throw new DrlCompileException(message, e.getLogs());
        }
    }
}
