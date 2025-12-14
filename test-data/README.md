# Test Data for Validation-Engine

Thư mục này chứa các file test data cho validation-engine, bao gồm compile requests, execute requests, và báo cáo kết
quả.

## Cấu trúc thư mục

```
test-data/
├── compile-requests/       # Input requests để compile rules thành bundleHash
├── execute-requests/       # Input requests để test execution
├── responses/              # Responses từ API (bundleHash, DRL content)
└── reports/                # Báo cáo test results
```

## Files

### 📂 compile-requests/

**compile-request-vip-weekend.json**

- Input request để tạo bundleHash
- Rules: Customer VIP + Product (SE01, SE02, SE03) + Temporal (Sat/Sun)
- API: `POST /v1/compiler/compile`

### 📂 execute-requests/

**execute-test-case-1-vip-se01.json**

- Test Case 1: VIP Customer + SE01 Product
- Expected: DENY (do temporal - không phải weekend)
- Actual: ALLOW với reason code TIME_WINDOW_NOT_ACTIVE (unexpected behavior)

**execute-test-case-2-non-vip-se01.json**

- Test Case 2: Non-VIP Customer + SE01 Product
- Expected: DENY (TIME_WINDOW_NOT_ACTIVE, CUSTOMER_NOT_VIP)
- Actual: ✅ DENY (passed)

**execute-test-case-3-vip-invalid-product.json**

- Test Case 3: VIP Customer + Invalid Product Category
- Expected: DENY (TIME_WINDOW_NOT_ACTIVE, PRODUCT_NOT_ELIGIBLE)
- Actual: ✅ DENY (passed)

### 📂 responses/

**bundle-hash.txt**

- BundleHash được generate từ compile API
- Value: `sha256:a85168f235cc7cac4a795b7a7c7646d253fbbfaa55d4282250e3efb215bcac3b`

**drl-formatted.txt**

- Nội dung DRL files đã được generate
- Bao gồm: timeframe.drl và validation-rule.drl
- API: `GET /v1/bundles/{bundleHash}/drl`

### 📂 reports/

**test-summary.txt**

- Tóm tắt kết quả test ngắn gọn

**final-report.md**

- Báo cáo chi tiết đầy đủ với phân tích từng test case

## Cách sử dụng

### 1. Compile rules thành bundleHash

```bash
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/compiler/compile \
  -H 'Content-Type: application/json' \
  -d @test-data/compile-requests/compile-request-vip-weekend.json
```

### 2. Test execution

```bash
# Test case 1
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/execute \
  -H 'Content-Type: application/json' \
  -d @test-data/execute-requests/execute-test-case-1-vip-se01.json

# Test case 2
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/execute \
  -H 'Content-Type: application/json' \
  -d @test-data/execute-requests/execute-test-case-2-non-vip-se01.json

# Test case 3
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/execute \
  -H 'Content-Type: application/json' \
  -d @test-data/execute-requests/execute-test-case-3-vip-invalid-product.json
```

### 3. Xem DRL content

```bash
BUNDLE_HASH=$(cat test-data/responses/bundle-hash.txt)
curl http://localhost:16013/promotion/promotion-validation-engine/v1/bundles/${BUNDLE_HASH}/drl
```

## Kết quả Test

- **Test Date**: 2025-11-14 (Friday)
- **Bundle**: sha256:a85168f235cc7cac4a795b7a7c7646d253fbbfaa55d4282250e3efb215bcac3b
- **Results**: 2/3 PASSED, 1/3 UNEXPECTED

Xem chi tiết trong `reports/final-report.md`

## Rules Logic

**Temporal Rule**:

- Days: SATURDAY, SUNDAY only
- Timezone: Asia/Bangkok
- Time: 00:00 - 23:59

**Validation Rule**:

- Customer segment must contain "VIP"
- Product category must be in (SE01, SE02, SE03)
- Logic: ALL (both conditions must be satisfied)

## Notes

- Test case 1 có hành vi không mong đợi: Decision = ALLOW dù có reason code TIME_WINDOW_NOT_ACTIVE
- Cần review decision aggregation logic khi có multiple DRL files
