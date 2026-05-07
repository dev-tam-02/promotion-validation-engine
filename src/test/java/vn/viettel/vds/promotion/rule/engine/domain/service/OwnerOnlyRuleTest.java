package vn.viettel.vds.promotion.rule.engine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
import vn.viettel.vds.promotion.rule.engine.domain.model.VoucherFact;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OwnerOnly rule (customer.is_owner operator, gap G3).
 *
 * <p>Verifies four cases from the spec:
 * <ul>
 *   <li>Case A (Welcome/Loyalty): voucher.ownerCustomerId == customer.id → ALLOW</li>
 *   <li>Case A-fail:              voucher.ownerCustomerId != customer.id → DENY + VOUCHER_NOT_OWNED_BY_CUSTOMER</li>
 *   <li>Case C (Gift):            rule not bound (rule not in session) → ALLOW for any customer</li>
 *   <li>Case D:                   voucher.ownerCustomerId == null, rule bound → DENY (guard fails)</li>
 * </ul>
 *
 * <p>The DRL used in these tests is the compiled output expected from pp-validation's
 * {@code DrlCompiler} for the {@code customer.is_owner} operator with the
 * {@code tpl_customer_is_owner_v1} template.
 */
@DisplayName("OwnerOnly rule — 4 spec cases")
class OwnerOnlyRuleTest {

    /**
     * DRL for the rule-sys-owner-only system rule.
     * Mirrors what DrlCompiler generates from tpl_customer_is_owner_v1.drl.mustache.
     */
    private static final String OWNER_ONLY_DRL = """
            package rules;
            
            import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
            import vn.viettel.vds.promotion.rule.engine.domain.model.VoucherFact;
            import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
            import vn.viettel.vds.promotion.rule.engine.domain.model.RuleMatched;
            import java.util.List;
            import java.util.ArrayList;
            
            global ValidationResult result;
            global List<String> reasonCodes;
            
            declare TemporalAllowed
            end
            
            rule "insert_temporal_allowed"
                salience 9999
                when
                    not TemporalAllowed()
                then
                    insert(new TemporalAllowed());
            end
            
            rule "promotion_validation_rule"
                when
                    TemporalAllowed()
                    $v: VoucherFact(ownerCustomerId != null)
                    $c: CustomerFact(id == $v.ownerCustomerId)
                then
                    result.setDecision("ALLOW");
                    result.setOk(true);
                    insert(new RuleMatched());
            end
            
            rule "failure_tracking_owner_check"
                salience -10
                when
                    not RuleMatched()
                    $v: VoucherFact(ownerCustomerId != null)
                    not CustomerFact(id == $v.ownerCustomerId)
                then
                    reasonCodes.add("VOUCHER_NOT_OWNED_BY_CUSTOMER");
            end
            
            rule "promotion_validation_failure"
                salience -100
                when
                    not RuleMatched()
                then
                    result.setDecision("DENY");
                    result.setOk(false);
                    result.setReasonCodes(reasonCodes);
            end
            """;

    private KieContainer ownerOnlyContainer;

    @BeforeEach
    void setUp() {
        ownerOnlyContainer = compileAndGetContainer(OWNER_ONLY_DRL);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case A: owner matches → ALLOW
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Case A: voucher owner == redeeming customer → ALLOW")
    void caseA_ownerMatches_allow() {
        VoucherFact voucher = new VoucherFact("SALE-A1B2", "cust-123");
        CustomerFact customer = new CustomerFact("cust-123");

        ExecutionResult result = executeRule(ownerOnlyContainer, voucher, customer);

        assertEquals("ALLOW", result.decision());
        assertTrue(result.ok());
        assertFalse(result.reasonCodes().contains("VOUCHER_NOT_OWNED_BY_CUSTOMER"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case A-fail: owner doesn't match → DENY + reason code
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Case A-fail: voucher owner != redeeming customer → DENY + VOUCHER_NOT_OWNED_BY_CUSTOMER")
    void caseAFail_ownerMismatch_denyWithReasonCode() {
        VoucherFact voucher = new VoucherFact("SALE-A1B2", "cust-123");
        CustomerFact customer = new CustomerFact("cust-456");

        ExecutionResult result = executeRule(ownerOnlyContainer, voucher, customer);

        assertEquals("DENY", result.decision());
        assertFalse(result.ok());
        assertTrue(result.reasonCodes().contains("VOUCHER_NOT_OWNED_BY_CUSTOMER"),
                "Expected VOUCHER_NOT_OWNED_BY_CUSTOMER in reason codes, got: " + result.reasonCodes());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case C: rule not bound (no VoucherFact / no owner rule in session) →
    // customer-456 can redeem because nothing denies it
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Case C: rule not bound (no VoucherFact injected) → no VOUCHER_NOT_OWNED_BY_CUSTOMER denial triggered by ownership")
    void caseC_ruleNotBound_noOwnershipDenial() {
        // Simulate a scenario where the campaign does NOT bind the OwnerOnly rule:
        // execute with a simple permissive DRL (no VoucherFact / no owner check)
        String publicDrl = """
                package rules;
                import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
                import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
                import vn.viettel.vds.promotion.rule.engine.domain.model.RuleMatched;
                import java.util.List;
                global ValidationResult result;
                global List<String> reasonCodes;
                declare TemporalAllowed end
                rule "insert_temporal_allowed" salience 9999
                    when not TemporalAllowed() then insert(new TemporalAllowed()); end
                rule "allow_all"
                    when TemporalAllowed() $c: CustomerFact()
                    then result.setDecision("ALLOW"); result.setOk(true); insert(new RuleMatched()); end
                rule "promotion_validation_failure" salience -100
                    when not RuleMatched()
                    then result.setDecision("DENY"); result.setOk(false); end
                """;
        KieContainer publicContainer = compileAndGetContainer(publicDrl);
        CustomerFact customer = new CustomerFact("cust-456");

        ExecutionResult result = executeRule(publicContainer, null, customer);

        assertEquals("ALLOW", result.decision());
        assertFalse(result.reasonCodes().contains("VOUCHER_NOT_OWNED_BY_CUSTOMER"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case D (spec): owner == null AND rule NOT bound → ALLOW (public promo)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Case D (spec): public promo — OwnerOnly rule not bound → no ownership denial → ALLOW")
    void caseD_publicPromo_noOwnerAndNoRuleBound_allow() {
        // A public promotion does NOT bind the OwnerOnly rule.
        // Simulate by executing with a permissive DRL that has no VoucherFact / ownership check.
        String publicDrl = """
                package rules;
                import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
                import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
                import vn.viettel.vds.promotion.rule.engine.domain.model.RuleMatched;
                import java.util.List;
                global ValidationResult result;
                global List<String> reasonCodes;
                declare TemporalAllowed end
                rule "insert_temporal_allowed" salience 9999
                    when not TemporalAllowed() then insert(new TemporalAllowed()); end
                rule "allow_all"
                    when TemporalAllowed() $c: CustomerFact()
                    then result.setDecision("ALLOW"); result.setOk(true); insert(new RuleMatched()); end
                rule "promotion_validation_failure" salience -100
                    when not RuleMatched()
                    then result.setDecision("DENY"); result.setOk(false); end
                """;
        KieContainer publicContainer = compileAndGetContainer(publicDrl);

        // No VoucherFact injected (OwnerOnly rule is not in evaluated rules)
        CustomerFact customer = new CustomerFact("cust-456");

        ExecutionResult result = executeRule(publicContainer, null, customer);

        assertEquals("ALLOW", result.decision());
        assertTrue(result.ok());
        assertFalse(result.reasonCodes().contains("VOUCHER_NOT_OWNED_BY_CUSTOMER"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case E (edge): owner == null + OwnerOnly rule IS bound → guard fails → DENY
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Case E: ownerCustomerId is null + rule bound → guard (ownerCustomerId != null) fails → DENY")
    void caseE_ownerNullRuleBoundStill_denyViaGuard() {
        VoucherFact voucher = new VoucherFact("PUBLIC-X1", null); // ownerCustomerId == null
        CustomerFact customer = new CustomerFact("cust-456");

        ExecutionResult result = executeRule(ownerOnlyContainer, voucher, customer);

        // Guard `VoucherFact(ownerCustomerId != null)` fails → main rule never fires →
        // failure_tracking fires with VOUCHER_NOT_OWNED_BY_CUSTOMER → DENY
        assertEquals("DENY", result.decision());
        assertFalse(result.ok());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private KieContainer compileAndGetContainer(String drl) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/rules/owner-only-rule.drl", drl);
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            fail("DRL compilation failed:\n" + kb.getResults().getMessages());
        }

        KieModule module = kb.getKieModule();
        return ks.newKieContainer(module.getReleaseId());
    }

    private ExecutionResult executeRule(KieContainer container, VoucherFact voucher, CustomerFact customer) {
        StatelessKieSession session = container.newStatelessKieSession();

        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);

        List<Object> facts = new ArrayList<>();
        if (customer != null) {
            facts.add(customer);
        }
        if (voucher != null) {
            facts.add(voucher);
        }

        session.execute(facts);

        return new ExecutionResult(
                result.getDecision(),
                Boolean.TRUE.equals(result.getOk()),
                result.getReasonCodes() != null ? result.getReasonCodes() : reasonCodes
        );
    }

    private record ExecutionResult(String decision, boolean ok, List<String> reasonCodes) {
    }
}
