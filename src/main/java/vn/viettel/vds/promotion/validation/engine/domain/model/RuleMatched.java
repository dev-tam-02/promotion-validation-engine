package vn.viettel.vds.promotion.validation.engine.domain.model;

/**
 * Marker fact to indicate that the main validation rule has matched.
 * This is inserted into working memory when the promotion validation rule fires successfully.
 *
 * Used to prevent failure tracking rules from firing when the main rule has already succeeded.
 */
public class RuleMatched {

    public RuleMatched() {
        // Empty constructor - this is just a marker
    }

    @Override
    public String toString() {
        return "RuleMatched{}";
    }
}
