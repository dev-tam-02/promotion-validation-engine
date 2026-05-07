package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.rule.engine.application.usecase.RuleDiscoveryService;

import java.util.List;
import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/discovery")
@Tag(name = "Rule Discovery", description = "Discover valid campaigns API")
public class RuleDiscoveryController {

    private final RuleDiscoveryService ruleDiscoveryService;

    public RuleDiscoveryController(RuleDiscoveryService ruleDiscoveryService) {
        this.ruleDiscoveryService = ruleDiscoveryService;
    }

    @Operation(summary = "Discover valid campaigns",
            description = "Find all valid campaigns for given context")
    @PostMapping("/campaigns")
    public ResponseEntity<List<String>> discoverValidCampaigns(
            @RequestBody Map<String, Object> context) {

        RuleDiscoveryService.DiscoveryRequest request =
                new RuleDiscoveryService.DiscoveryRequest(context);

        List<String> validCampaigns = ruleDiscoveryService.discoverValidCampaigns(request);

        return ResponseEntity.ok(validCampaigns);
    }

    @Operation(summary = "Discover campaigns with details",
            description = "Find all campaigns with detailed validation results")
    @PostMapping("/campaigns/details")
    public ResponseEntity<List<RuleDiscoveryService.CampaignValidationResult>> discoverWithDetails(
            @RequestBody Map<String, Object> context,
            @RequestParam(defaultValue = "false") boolean explain) {

        RuleDiscoveryService.DiscoveryRequest request =
                new RuleDiscoveryService.DiscoveryRequest(context);
        request.setExplainResults(explain);

        List<RuleDiscoveryService.CampaignValidationResult> results =
                ruleDiscoveryService.discoverWithDetails(request);

        return ResponseEntity.ok(results);
    }
}
