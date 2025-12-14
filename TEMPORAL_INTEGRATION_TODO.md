# TODO: Temporal DRL Integration

## 📋 TRẠNG THÁI HIỆN TẠI

### ✅ ĐÃ HOÀN THÀNH:

1. ✅ **CompileRequest.TemporalPolicyData** - DTO cho temporal data
2. ✅ **TemporalDrlGenerator** - Service generate timeframe.drl từ TemporalPolicyData
3. ✅ **DroolsCompilationService.compileMultipleDrls()** - Compile nhiều DRL vào 1 bundle

**Commit**: `e3f30bf` - feat(validation-engine): add temporal DRL generation support

---

## 🔄 CẦN LÀM TIẾP (VALIDATION-ENGINE)

### **Task 5: Update DroolsRuleEngineAdapter** 🎯 HIGH PRIORITY

**File**: `src/main/java/vn/viettel/vds/promotion/validation/engine/adapter/out/rules/DroolsRuleEngineAdapter.java`

**Yêu cầu:**

1. Inject `TemporalDrlGenerator` vào constructor
2. Update `compile()` method:
    - Check if `request.getTimeLinks()` not null và not empty
    - If YES → Generate temporal DRL:
      ```java
      // Get first timeLink (assumption: 1 policy per assignment)
      TimeLink timeLink = request.getTimeLinks().get(0);
      TemporalPolicyData temporalData = timeLink.getData();
 
      // Generate temporal DRL
      String assignmentId = request.getRuleId(); // Or pass explicitly
      String timeframeDrl = temporalDrlGenerator.generateTimeframeDrl(assignmentId, temporalData);
      ```
    - Generate business rule DRL (existing logic)
    - Combine 2 DRLs:
      ```java
      Map<String, String> drlFiles = new LinkedHashMap<>();
      drlFiles.put("timeframe.drl", timeframeDrl);
      drlFiles.put("validation-rule.drl", businessRuleDrl);
 
      // Use compileMultipleDrls instead of compileDrl
      CompilationResult result = droolsCompilationService.compileMultipleDrls(
          request.getTenantId(),
          request.getRuleId(),
          request.getVersion(),
          drlFiles
      );
      ```

**Code template:**

```java
@Component
@Qualifier("droolsRuleEngineAdapter")
public class DroolsRuleEngineAdapter implements RuleEnginePort {

    private final DroolsCompilationService droolsCompilationService;
    private final RuleTranslationService ruleTranslationService;
    private final TemporalDrlGenerator temporalDrlGenerator; // ← ADD THIS

    public DroolsRuleEngineAdapter(
            DroolsCompilationService droolsCompilationService,
            RuleTranslationService ruleTranslationService,
            TemporalDrlGenerator temporalDrlGenerator) { // ← ADD THIS
        this.droolsCompilationService = droolsCompilationService;
        this.ruleTranslationService = ruleTranslationService;
        this.temporalDrlGenerator = temporalDrlGenerator; // ← ADD THIS
    }

    @Override
    public CompileResult compile(CompileInput input) {
        // ... existing validation ...

        // Generate business rule DRL
        String businessRuleDrl = ruleTranslationService.translateToDrl(
            input.tenantId(),
            input.nodes()
        );

        // Check if temporal policy exists
        boolean hasTemporalPolicy = input.getTimeLinks() != null
            && !input.getTimeLinks().isEmpty()
            && input.getTimeLinks().get(0).getData() != null;

        DroolsCompilationService.CompilationResult compilationResult;

        if (hasTemporalPolicy) {
            // Generate temporal DRL
            TimeLink timeLink = input.getTimeLinks().get(0);
            String timeframeDrl = temporalDrlGenerator.generateTimeframeDrl(
                input.ruleId(),
                timeLink.getData()
            );

            // Compile 2 DRLs together
            Map<String, String> drlFiles = new LinkedHashMap<>();
            drlFiles.put("timeframe.drl", timeframeDrl);
            drlFiles.put("validation-rule.drl", businessRuleDrl);

            compilationResult = droolsCompilationService.compileMultipleDrls(
                input.tenantId(),
                input.ruleId(),
                input.version(),
                drlFiles
            );
        } else {
            // No temporal policy - compile business rule only
            compilationResult = droolsCompilationService.compileDrl(
                input.tenantId(),
                input.ruleId(),
                input.version(),
                businessRuleDrl
            );
        }

        // ... return CompileResult ...
    }
}
```

**Testing checklist:**

- [ ] Service compiles successfully
- [ ] No temporal data → single DRL compiled (existing behavior)
- [ ] With temporal data → 2 DRLs compiled into 1 bundle
- [ ] BundleHash generated correctly from combined DRL
- [ ] Logs show both DRL files being written

---

## 🔄 CẦN LÀM TIẾP (VALIDATION SERVICE)

### **Task 6: Update RulePublishingService**

**File**:
`/Users/hoanglam/promix/validation/src/main/java/vn/viettel/vds/promotion/validation/domain/service/RulePublishingService.java`

**Yêu cầu:**

1. Update `buildCompileRequest()` method:
    - Accept `assignmentId` parameter
    - Query `RuleTemporalLinkJpaRepository.findByAssignmentId(assignmentId)`
    - If temporal links exist:
      ```java
      // Get temporal policy data
      RuleTemporalLinkEntity link = temporalLinks.get(0);
      TemporalPolicyEntity policy = link.getTemporalPolicy();
      List<TemporalPolicyWindowEntity> windows = policy.getTimeOfDayWindows();
 
      // Build TemporalPolicyData DTO
      TemporalPolicyData data = new TemporalPolicyData();
      data.setTimezone(policy.getTz());
      data.setRrule(policy.getRrule());
      data.setStartTs(policy.getStartTs() != null ? policy.getStartTs().toString() : null);
      data.setEndTs(policy.getEndTs() != null ? policy.getEndTs().toString() : null);
 
      List<TimeWindow> windowDtos = windows.stream()
          .map(w -> new TimeWindow(w.getStart(), w.getEnd()))
          .toList();
      data.setWindows(windowDtos);
 
      // Add to timeLinks
      TimeLink timeLink = new TimeLink(policy.getId(), link.getMode(), data);
      compileRequest.setTimeLinks(List.of(timeLink));
      ```

2. Update `deployRuleToEngine()` in SettingValidationRuleCommandHandler:
    - Pass assignmentId to publishRule()
    - Store returned bundleHash to `AssignmentEntity.temporalBundleHash`

**Code template:**

```java
// In RulePublishingService
private CompileRequest buildCompileRequest(Rule rule, Integer version, String logic,
                                          List<RuleNodeDto> nodeDtos, String assignmentId) {
    CompileRequest compileRequest = new CompileRequest();
    compileRequest.setTenantId(tenantProperties.getDefaultTenantId());
    compileRequest.setRuleId(rule.getId());
    compileRequest.setVersion(version);
    compileRequest.setLogic(logic);
    compileRequest.setNodes(nodeDtos);
    compileRequest.setOperatorsFingerprint(generateOperatorFingerprint(rule.getNodes()));

    // Add temporal policy data if available
    if (assignmentId != null) {
        List<RuleTemporalLinkEntity> temporalLinks =
            ruleTemporalLinkRepository.findByAssignmentId(assignmentId);

        if (!temporalLinks.isEmpty()) {
            RuleTemporalLinkEntity link = temporalLinks.get(0);
            TemporalPolicyEntity policy = link.getTemporalPolicy();

            // Build TemporalPolicyData
            TemporalPolicyData data = buildTemporalPolicyData(policy);
            TimeLink timeLink = new TimeLink(policy.getId(), link.getMode(), data);
            compileRequest.setTimeLinks(List.of(timeLink));
        }
    }

    return compileRequest;
}

private TemporalPolicyData buildTemporalPolicyData(TemporalPolicyEntity policy) {
    TemporalPolicyData data = new TemporalPolicyData();
    data.setTimezone(policy.getTz());
    data.setRrule(policy.getRrule());
    data.setStartTs(policy.getStartTs() != null ? policy.getStartTs().toString() : null);
    data.setEndTs(policy.getEndTs() != null ? policy.getEndTs().toString() : null);

    List<TimeWindow> windowDtos = policy.getTimeOfDayWindows().stream()
        .map(w -> new TimeWindow(w.getStart(), w.getEnd()))
        .toList();
    data.setWindows(windowDtos);

    return data;
}
```

---

## 🧪 TESTING PLAN

### **Unit Tests:**

1. TemporalDrlGenerator:
    - [ ] Generate DRL with single time window
    - [ ] Generate DRL with multiple time windows
    - [ ] Generate DRL with day-of-week filtering
    - [ ] Generate DRL with midnight-spanning range
    - [ ] Generate empty DRL when no temporal data

2. DroolsCompilationService:
    - [ ] Compile single DRL (existing)
    - [ ] Compile multiple DRLs
    - [ ] Generate correct bundleHash from combined content

3. DroolsRuleEngineAdapter:
    - [ ] Compile with temporal data
    - [ ] Compile without temporal data
    - [ ] Correct DRL files written to KieFileSystem

### **Integration Tests:**

1. End-to-end flow:
    - [ ] Send SettingValidationRuleCommand with timeframe
    - [ ] Verify temporal_policies created
    - [ ] Verify temporal_policy_windows created
    - [ ] Verify rule_temporal_links created
    - [ ] Verify bundle compiled with 2 DRLs
    - [ ] Verify temporalBundleHash stored in AssignmentEntity

2. Execution tests:
    - [ ] Execute bundle with time in allowed window → ALLOW
    - [ ] Execute bundle with time outside window → DENY with TIME_WINDOW_NOT_ACTIVE
    - [ ] Verify temporal check runs before business rules (salience)

---

## 📁 FILES CẦN XEM THÊM

### Validation Engine:

- `src/main/java/vn/viettel/vds/promotion/validation/engine/adapter/out/rules/DroolsRuleEngineAdapter.java` - **CẦN
  UPDATE**
- `src/main/java/vn/viettel/vds/promotion/validation/engine/domain/service/TemporalDrlGenerator.java` - **ĐÃ TẠO**
- `src/main/java/vn/viettel/vds/promotion/validation/engine/domain/service/DroolsCompilationService.java` - **ĐÃ UPDATE
  **

### Validation Service:

- `src/main/java/vn/viettel/vds/promotion/validation/domain/service/RulePublishingService.java` - **CẦN UPDATE**
- `src/main/java/vn/viettel/vds/promotion/validation/application/service/SettingValidationRuleCommandHandler.java` - *
  *CẦN UPDATE**

---

## 🎯 ACCEPTANCE CRITERIA

### Khi hoàn thành, system phải:

1. ✅ Nhận Kafka command với timeframe data
2. ✅ Lưu temporal_policies + windows + links vào database
3. ✅ Gọi validation-engine với TemporalPolicyData
4. ✅ Validation-engine generate 2 DRL files
5. ✅ Compile 2 DRLs vào 1 bundle (KJAR)
6. ✅ Store bundleHash vào AssignmentEntity.temporalBundleHash
7. ✅ Execution: Check temporal FIRST → nếu DENY thì stop, nếu ALLOW thì check business rules

---

## 📝 NOTES

- **Assignment ID**: Cần pass assignmentId từ SettingValidationRuleCommandHandler xuống RulePublishingService
- **TimeLink assumption**: Hiện tại assume 1 assignment = 1 temporal policy (lấy first timeLink)
- **BundleHash**: Generated from combined DRL content (deterministic)
- **Salience**: Temporal rules có salience 1000/999, business rules có salience 0/-100
- **Package names**: Temporal DRL dùng `promotion.assignment.{assignmentId}`, Business DRL dùng `{tenantId}`

---

## 🚀 NEXT SESSION CHECKLIST

1. [ ] Open validation-engine project
2. [ ] Read this TODO file
3. [ ] Start with Task 5: Update DroolsRuleEngineAdapter
4. [ ] Test compilation với temporal data
5. [ ] Move to Task 6: Update validation service
6. [ ] Run end-to-end integration test

**Good luck! 💪**
