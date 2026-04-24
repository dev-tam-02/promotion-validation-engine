package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.util.Set;

/**
 * Drools fact representing a customer, used by template-compiled DRL rules.
 *
 * <p>This is a lightweight projection of {@link Customer} for use in
 * template-based DRL rules (compiled via {@code DrlCompiler} in pp-validation).
 * Template operators such as {@code customer.in_segment} and
 * {@code customer.is_owner} reference {@code CustomerFact} in their DRL snippets.
 *
 * <p>Both {@link Customer} and {@code CustomerFact} are injected into every
 * Drools session by {@code FactPreparationService} — existing operator-translator
 * rules continue to bind against {@link Customer} while template-compiled rules
 * bind against {@code CustomerFact}.
 */
public class CustomerFact {

    private String id;
    private Set<String> segments;
    private String loyaltyTier;

    public CustomerFact() {
    }

    public CustomerFact(String id) {
        this.id = id;
    }

    public CustomerFact(String id, Set<String> segments, String loyaltyTier) {
        this.id = id;
        this.segments = segments;
        this.loyaltyTier = loyaltyTier;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getSegments() {
        return segments;
    }

    public void setSegments(Set<String> segments) {
        this.segments = segments;
    }

    public String getLoyaltyTier() {
        return loyaltyTier;
    }

    public void setLoyaltyTier(String loyaltyTier) {
        this.loyaltyTier = loyaltyTier;
    }
}
