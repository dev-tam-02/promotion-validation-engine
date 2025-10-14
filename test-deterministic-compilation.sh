#!/bin/bash

# Test script to verify deterministic DRL compilation
# This script compiles the same rule multiple times and verifies bundleHash consistency

echo "=== Testing Deterministic Compilation ==="
echo ""

VALIDATION_ENGINE_URL="http://localhost:16013"
RULE_ID="01932b6f-0005-7000-8000-000000000001"

# Test payload - simple rule with multiple nodes
TEST_PAYLOAD='{
  "tenantId": "default",
  "ruleId": "test-deterministic-rule",
  "version": 1,
  "logic": "ALL",
  "nodes": [
    {
      "id": "node_1",
      "type": "COND",
      "operatorName": "customer.in_segment",
      "params": {
        "segments": ["VIP", "GOLD"]
      },
      "reasonCode": "CUSTOMER_NOT_VIP"
    },
    {
      "id": "node_2",
      "type": "COND",
      "operatorName": "order.total.gte",
      "params": {
        "amount": 500000
      },
      "reasonCode": "ORDER_AMOUNT_TOO_LOW"
    },
    {
      "id": "node_3",
      "type": "COND",
      "operatorName": "time.window.active",
      "params": {
        "policyId": "weekend_policy"
      },
      "reasonCode": "TIME_WINDOW_INACTIVE"
    },
    {
      "id": "group_root",
      "type": "GROUP",
      "groupLogic": "ALL",
      "children": ["node_1", "node_2", "node_3"]
    }
  ],
  "operatorsFingerprint": "customer.in_segment:v1;order.total.gte:v1;time.window.active:v1"
}'

echo "Payload:"
echo "$TEST_PAYLOAD" | jq '.'
echo ""

# Compile the rule 5 times and collect bundleHashes
echo "Compiling rule 5 times to verify determinism..."
echo ""

HASHES=()
for i in {1..5}; do
    echo "--- Compilation $i ---"

    RESPONSE=$(curl -s -X POST "$VALIDATION_ENGINE_URL/v1/compiler/compile" \
        -H "Content-Type: application/json" \
        -d "$TEST_PAYLOAD")

    BUNDLE_HASH=$(echo "$RESPONSE" | jq -r '.bundleHash')
    OK=$(echo "$RESPONSE" | jq -r '.ok')

    if [ "$OK" = "true" ]; then
        echo "✅ Compilation $i successful"
        echo "   BundleHash: $BUNDLE_HASH"
        HASHES+=("$BUNDLE_HASH")
    else
        echo "❌ Compilation $i failed"
        echo "   Response: $RESPONSE"
        exit 1
    fi

    echo ""

    # Small delay to avoid rate limiting
    sleep 0.5
done

echo "=== Verification Results ==="
echo ""

# Check if all hashes are identical
UNIQUE_HASHES=$(printf '%s\n' "${HASHES[@]}" | sort -u | wc -l)

if [ "$UNIQUE_HASHES" -eq 1 ]; then
    echo "✅ SUCCESS: All 5 compilations produced the same bundleHash!"
    echo "   BundleHash: ${HASHES[0]}"
    echo ""
    echo "✅ Deterministic compilation is working correctly!"
    exit 0
else
    echo "❌ FAILED: Compilations produced different bundleHashes!"
    echo ""
    echo "BundleHashes collected:"
    for i in "${!HASHES[@]}"; do
        echo "  Compilation $((i+1)): ${HASHES[$i]}"
    done
    echo ""
    echo "❌ Non-deterministic compilation detected - fix did not work!"
    exit 1
fi
