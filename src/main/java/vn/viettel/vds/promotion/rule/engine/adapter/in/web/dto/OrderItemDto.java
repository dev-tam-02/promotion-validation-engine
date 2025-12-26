package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Order item data")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderItemDto(
        @Schema(description = "SKU identifier (atomic unit of product)", example = "IPHONE-15-PRO-256GB-BLUE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "SKU ID is required")
        @JsonProperty("skuId")
        String skuId,

        @Schema(description = "Product identifier", example = "IPHONE-15-PRO")
        @JsonProperty("productId")
        String productId,

        @Schema(description = "Collection identifiers", example = "[\"Premium Phones\", \"Apple Products\"]")
        @JsonProperty("collectionIds")
        List<String> collectionIds,

        @Schema(description = "Item quantity", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        @JsonProperty("quantity")
        Integer quantity,

        @Schema(description = "Item price", example = "250000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Price is required")
        @PositiveOrZero(message = "Price must not be negative")
        @JsonProperty("price")
        BigDecimal price,

        @Schema(description = "Product category", example = "ELECTRONICS")
        @JsonProperty("category")
        String category,

        @Schema(description = "Product brand", example = "SAMSUNG")
        @JsonProperty("brand")
        String brand,

        @Schema(description = "Item metadata")
        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
}