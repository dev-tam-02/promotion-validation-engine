package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOrderRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOrderResponse;
import vn.viettel.vds.promotion.rule.engine.application.usecase.OrderValidationService;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/validate")
@Tag(name = "Order Validation", description = "Order validation against campaigns API")
public class OrderValidationController {

    private static final Logger logger = LoggerFactory.getLogger(OrderValidationController.class);

    private final OrderValidationService orderValidationService;

    public OrderValidationController(OrderValidationService orderValidationService) {
        this.orderValidationService = orderValidationService;
    }

    @Operation(summary = "Validate order against campaigns", 
               description = "Validate an order against applicable campaigns with filtering options")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Validation failed")
    })
    @PostMapping("/order")
    public ResponseEntity<ValidateOrderResponse> validateOrder(
            @Valid @RequestBody ValidateOrderRequest request) {

        logger.info("Validating order: customerId={}, orderId={}, campaignFilter={}", 
                request.getCustomer().id(), 
                request.getOrder().id(),
                request.getCampaignFilter() != null ? "present" : "none");

        try {
            ValidateOrderResponse response = orderValidationService.validateOrder(request);
            
            logger.info("Order validation completed: success={}, totalCampaigns={}, validCampaigns={}", 
                    response.getSuccess(),
                    response.getSummary() != null ? response.getSummary().getTotalCampaigns() : 0,
                    response.getSummary() != null ? response.getSummary().getValidCampaigns() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Order validation failed: customerId={}, orderId={}", 
                    request.getCustomer().id(), request.getOrder().id(), e);

            ValidateOrderResponse errorResponse = new ValidateOrderResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Validation failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
