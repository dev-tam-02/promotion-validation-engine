package vn.viettel.vds.promotion.rule.engine;

import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestFixtures {

    private TestFixtures() {
    }

    // --- Domain Models ---

    public static Order order(String id, BigDecimal total) {
        Order order = new Order(id);
        order.setTotal(total);
        order.setCurrency("VND");
        return order;
    }

    public static Order orderWithItems(String id, BigDecimal total, List<OrderItem> items) {
        Order order = order(id, total);
        order.setItems(items);
        return order;
    }

    public static OrderItem orderItem(String category, String brand, double price, int quantity) {
        OrderItem item = new OrderItem();
        item.setCategory(category);
        item.setBrand(brand);
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    public static OrderItem orderItem(String productName, String category, String sku, double price, int quantity) {
        return new OrderItem(productName, category, sku, price, quantity);
    }

    public static Customer customer(String id, String tier) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setLoyaltyTier(tier);
        return customer;
    }

    public static Customer customerWithSegments(String id, Set<String> segments) {
        return new Customer(id, segments);
    }

    public static Candidate voucher(String code, String id) {
        return new Candidate("voucher", code, id);
    }

    public static Candidate tierCandidate(String code) {
        return new Candidate("tier", code);
    }

    public static Candidate loyaltyCandidate(String code) {
        return new Candidate("loyalty", code);
    }

    public static Decision validDecision(Candidate candidate) {
        return new Decision(candidate, true);
    }

    public static Decision invalidDecision(Candidate candidate) {
        Decision decision = new Decision(candidate, false);
        decision.setReasons(List.of(new ReasonCode("VALIDATION_FAILED")));
        return decision;
    }

    public static ValidationResult matchedResult() {
        ValidationResult result = new ValidationResult(true, "Matched");
        result.setOk(true);
        result.setDecision("ALLOW");
        return result;
    }

    public static ValidationResult unmatchedResult(String reason) {
        ValidationResult result = new ValidationResult(false, reason);
        result.setOk(false);
        result.setDecision("DENY");
        result.setReasonCodes(List.of(reason));
        return result;
    }

    public static Redemption redemption(boolean isCodeHolder, String locationId) {
        return new Redemption(isCodeHolder, locationId);
    }

    public static LimitsCtx limitsCtx(int totalRedemptions, int maxTotal) {
        LimitsCtx ctx = new LimitsCtx();
        ctx.setTotalRedemptions(totalRedemptions);
        ctx.setMaxTotalRedemptions(maxTotal);
        return ctx;
    }

    public static LimitsCtx limitsCtxWithDaily(int dailyRedemptions, int maxDaily) {
        LimitsCtx ctx = new LimitsCtx();
        ctx.setRedemptionsPerDay(dailyRedemptions);
        ctx.setMaxRedemptionsPerDay(maxDaily);
        return ctx;
    }

    public static EnvCtx envCtx(String channel) {
        return new EnvCtx(channel, null, null);
    }

    // --- JPA Entities ---

    public static BundleEntity bundleEntity(String hash, String ruleId, int version) {
        BundleEntity entity = new BundleEntity();
        entity.setId(hash);
        entity.setRuleId(ruleId);
        entity.setRuleVersion(version);
        entity.setEnabled(true);
        entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        BundleEntity.EngineInfo engineInfo = new BundleEntity.EngineInfo();
        engineInfo.setType("drools");
        engineInfo.setCompilerId("drools");
        engineInfo.setDroolsVersion("10.1.0");
        entity.setEngine(engineInfo);

        BundleEntity.Artifact artifact = new BundleEntity.Artifact();
        artifact.setStore("s3");
        artifact.setKey("bundles/" + hash.replace("sha256:", "") + ".kjar");
        artifact.setSize(1024L);
        entity.setArtifact(artifact);

        return entity;
    }

    public static CompileJobEntity compileJob(String jobId, String ruleId, int version,
                                              CompileJobEntity.JobStatus status) {
        CompileJobEntity job = new CompileJobEntity();
        job.setId(jobId);
        job.setRuleId(ruleId);
        job.setTargetVersion(version);
        job.setStatus(status);
        job.setRequestedBy("system");
        job.setRequestedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return job;
    }

    public static EngineConfigEntity engineConfig(String id, Integer timeoutMs, Integer maxRulesFired) {
        EngineConfigEntity config = new EngineConfigEntity();
        config.setId(id);
        EngineConfigEntity.ExecuteConfig executeConfig = new EngineConfigEntity.ExecuteConfig();
        executeConfig.setTimeoutMs(timeoutMs);
        executeConfig.setMaxRulesFired(maxRulesFired);
        config.setExecute(executeConfig);
        return config;
    }

    // --- Application DTOs ---

    public static ExecuteRequest executeRequest(String bundleHash, Map<String, Object> context) {
        ExecuteRequest.Bundle bundle = new ExecuteRequest.Bundle(bundleHash, 1, 1);
        return new ExecuteRequest(bundle, context, null);
    }

    public static ExecuteRequest executeRequestWithOptions(String bundleHash, Map<String, Object> context,
                                                           Integer timeoutMs, Integer maxRulesFired) {
        ExecuteRequest.Bundle bundle = new ExecuteRequest.Bundle(bundleHash, 1, 1);
        ExecuteRequest.ExecuteOptions options = new ExecuteRequest.ExecuteOptions("NONE", timeoutMs, maxRulesFired);
        return new ExecuteRequest(bundle, context, options);
    }

    public static ExecuteResponse executeResponse(boolean ok, String decision) {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(ok);
        response.setDecision(decision);
        response.setReasonCodes(ok ? List.of() : List.of("VALIDATION_FAILED"));
        return response;
    }

    public static CompileRequest compileRequest(String ruleId, int version) {
        CompileRequest request = new CompileRequest();
        request.setRuleId(ruleId);
        request.setVersion(version);
        request.setLogic("AND");
        request.setNodes(List.of(
                Map.of("id", "node-1", "type", "condition", "operatorName", "order.total.gte",
                        "params", Map.of("amount", "100"))
        ));
        return request;
    }

    public static Map<String, Object> validContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("now", Instant.now().toString());
        context.put("customer", Map.of("id", "cust-1", "tier", "GOLD"));
        context.put("order", Map.of("id", "ord-1", "total", 50000, "currency", "VND"));
        return context;
    }
}
