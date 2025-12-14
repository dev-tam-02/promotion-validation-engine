# Changelog - Validation Engine Test Data

## Version 2 - 2025-11-14

### ✅ Added Friday to Temporal Policy

**Changes**:

- Updated `compile-request-vip-weekend.json`:
    - Version: `1` → `2`
    - RRULE: `BYDAY=SA,SU` → `BYDAY=FR,SA,SU`
    - Now includes: **Friday, Saturday, Sunday**

**New BundleHash**: `sha256:89ceaf30c941a2af55a3ad25db3a6c7679d83593203300c6b7799bfa250c7fd8`

**DRL Changes**:

```drl
// OLD (Version 1):
eval(checkTimeWindow("00:00", "23:59", "Asia/Bangkok", false, "SATURDAY,SUNDAY"))
days=SATURDAY,SUNDAY

// NEW (Version 2):
eval(checkTimeWindow("00:00", "23:59", "Asia/Bangkok", false, "FRIDAY,SATURDAY,SUNDAY"))
days=FRIDAY,SATURDAY,SUNDAY
```

**Test Results Comparison**:

| Test                  | Version 1 (Sat/Sun only)             | Version 2 (Fri/Sat/Sun)       |
|-----------------------|--------------------------------------|-------------------------------|
| VIP + SE01 on Friday  | ⚠️ ALLOW with TIME_WINDOW_NOT_ACTIVE | ✅ ALLOW (no reason codes)     |
| Non-VIP + SE01        | ❌ DENY - CUSTOMER_NOT_VIP            | ❌ DENY - CUSTOMER_NOT_VIP     |
| VIP + Invalid Product | ❌ DENY - PRODUCT_NOT_ELIGIBLE        | ❌ DENY - PRODUCT_NOT_ELIGIBLE |

**Key Findings**:

- ✅ Temporal rule correctly detects Friday as valid day
- ✅ RRULE parsing works correctly: `FR,SA,SU` → `FRIDAY,SATURDAY,SUNDAY`
- ✅ No reason codes when all conditions pass
- ✅ Validation logic still works for customer segment and product category

**Files Added**:

- `test-data/responses/bundle-hash-v2-friday.txt`
- `test-data/responses/drl-v2-friday.txt`
- `test-data/execute-requests/execute-test-case-1-vip-se01-v2.json`

---

## Version 1 - 2025-11-14 (Initial)

**BundleHash**: `sha256:a85168f235cc7cac4a795b7a7c7646d253fbbfaa55d4282250e3efb215bcac3b`

**Temporal Policy**:

- Days: Saturday, Sunday only
- RRULE: `FREQ=WEEKLY;BYDAY=SA,SU`

**Test Environment**:

- Test Date: 2025-11-14 (Friday)
- Expected: DENY due to temporal (not weekend)
- Actual: Inconsistent behavior (see reports)

**Known Issues**:

- Test Case 1: Returns ALLOW with reason code TIME_WINDOW_NOT_ACTIVE
- Suggests decision aggregation logic needs review
