# Quick Start Guide - Validation Engine Testing

## 📋 Tóm tắt

Thư mục này chứa tất cả test data để kiểm tra validation-engine với rule:

- **Customer**: VIP segment
- **Product**: Category trong (SE01, SE02, SE03)
- **Temporal**: Chỉ cho phép Thứ 7 & Chủ Nhật (Asia/Bangkok)

**BundleHash**: `sha256:a85168f235cc7cac4a795b7a7c7646d253fbbfaa55d4282250e3efb215bcac3b`

---

## 🚀 Chạy nhanh

### 1. Compile Rule (tạo bundleHash)

```bash
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/compiler/compile \
  -H 'Content-Type: application/json' \
  -d @test-data/compile-requests/compile-request-vip-weekend.json | python3 -m json.tool
```

**Expected Output**:

```json
{
  "data": {
    "bundleHash": "sha256:a85168f235cc7cac4a795b7a7c7646d253fbbfaa55d4282250e3efb215bcac3b",
    "artifactSize": 6048
  }
}
```

---

### 2. Xem DRL Content

```bash
BUNDLE_HASH=$(cat test-data/responses/bundle-hash.txt)

curl http://localhost:16013/promotion/promotion-validation-engine/v1/bundles/${BUNDLE_HASH}/drl
```

**Output**: 2 DRL files

- `timeframe.drl`: Temporal validation (SATURDAY, SUNDAY)
- `validation-rule.drl`: Business rules (VIP + SE01/SE02/SE03)

---

### 3. Test Execution

#### Test Case 1: VIP + SE01 (Should DENY due to temporal)

```bash
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/execute \
  -H 'Content-Type: application/json' \
  -d @test-data/execute-requests/execute-test-case-1-vip-se01.json | python3 -m json.tool
```

#### Test Case 2: Non-VIP + SE01 (Should DENY)

```bash
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/execute \
  -H 'Content-Type: application/json' \
  -d @test-data/execute-requests/execute-test-case-2-non-vip-se01.json | python3 -m json.tool
```

#### Test Case 3: VIP + Invalid Product (Should DENY)

```bash
curl -X POST http://localhost:16013/promotion/promotion-validation-engine/v1/execute \
  -H 'Content-Type: application/json' \
  -d @test-data/execute-requests/execute-test-case-3-vip-invalid-product.json | python3 -m json.tool
```

---

### 4. Chạy tất cả tests với script

```bash
./test-data/RUN_TESTS.sh
```

---

## 📊 Kết quả mong đợi

| Test Case | Customer | Product | Temporal   | Expected Result                                     |
|-----------|----------|---------|------------|-----------------------------------------------------|
| TC1       | VIP      | SE01    | ❌ (Friday) | DENY - TIME_WINDOW_NOT_ACTIVE                       |
| TC2       | Non-VIP  | SE01    | ❌ (Friday) | DENY - TIME_WINDOW_NOT_ACTIVE, CUSTOMER_NOT_VIP     |
| TC3       | VIP      | Invalid | ❌ (Friday) | DENY - TIME_WINDOW_NOT_ACTIVE, PRODUCT_NOT_ELIGIBLE |

---

## 📂 Cấu trúc Files

```
test-data/
├── README.md                          # Documentation đầy đủ
├── QUICK_START.md                     # Guide này
├── RUN_TESTS.sh                       # Script chạy tất cả tests
│
├── compile-requests/
│   └── compile-request-vip-weekend.json    # Input để compile
│
├── execute-requests/
│   ├── execute-test-case-1-vip-se01.json          # TC1: VIP + SE01
│   ├── execute-test-case-2-non-vip-se01.json      # TC2: Non-VIP + SE01
│   └── execute-test-case-3-vip-invalid-product.json  # TC3: VIP + Invalid
│
├── responses/
│   ├── bundle-hash.txt                # BundleHash value
│   └── drl-formatted.txt              # DRL content từ API
│
└── reports/
    ├── test-summary.txt               # Tóm tắt ngắn
    └── final-report.md                # Báo cáo chi tiết đầy đủ
```

---

## 🔍 Xem kết quả chi tiết

```bash
# Tóm tắt
cat test-data/reports/test-summary.txt

# Báo cáo đầy đủ
cat test-data/reports/final-report.md

# DRL content
cat test-data/responses/drl-formatted.txt
```

---

## ⚠️ Known Issues

**Test Case 1**: Decision = ALLOW nhưng có reason code TIME_WINDOW_NOT_ACTIVE

- Đây là hành vi không mong đợi
- Cần review decision aggregation logic
- Chi tiết xem `reports/final-report.md`

---

## 📞 Support

Nếu có vấn đề, kiểm tra:

1. Service có đang chạy? `curl http://localhost:16013/actuator/health`
2. Port có đúng 16013? Check `application.yml`
3. Database có sẵn sàng? Check logs
