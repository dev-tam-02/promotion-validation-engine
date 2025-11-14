#!/bin/bash

# Script to run all test cases for validation-engine
# Usage: ./test-data/RUN_TESTS.sh

BASE_URL="http://localhost:16013/promotion/promotion-validation-engine"

echo "=============================================="
echo "  VALIDATION-ENGINE TEST RUNNER"
echo "=============================================="
echo ""

# Check if service is running
echo "🔍 Checking if validation-engine is running..."
if ! curl -s "${BASE_URL}/actuator/health" > /dev/null; then
    echo "❌ Validation-engine is not running on port 16013"
    echo "   Please start the service first: mvn spring-boot:run"
    exit 1
fi
echo "✅ Service is running"
echo ""

# Test 1: Compile request
echo "=============================================="
echo "TEST 1: Compile Rule Bundle"
echo "=============================================="
echo "📝 Request: test-data/compile-requests/compile-request-vip-weekend.json"
echo ""

COMPILE_RESPONSE=$(curl -s -X POST "${BASE_URL}/v1/compiler/compile" \
  -H 'Content-Type: application/json' \
  -d @test-data/compile-requests/compile-request-vip-weekend.json)

BUNDLE_HASH=$(echo "$COMPILE_RESPONSE" | grep -o 'sha256:[a-f0-9]\{64\}' | head -1)

if [ -z "$BUNDLE_HASH" ]; then
    echo "❌ Compilation failed"
    echo "$COMPILE_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$COMPILE_RESPONSE"
    exit 1
fi

echo "✅ Compilation successful"
echo "📦 BundleHash: $BUNDLE_HASH"
echo ""

# Test 2: Execute test cases
echo "=============================================="
echo "TEST 2: Execute Test Cases"
echo "=============================================="
echo ""

for i in 1 2 3; do
    TEST_FILE="test-data/execute-requests/execute-test-case-${i}*.json"
    TEST_NAME=$(ls $TEST_FILE 2>/dev/null | head -1 | xargs basename)
    
    if [ -z "$TEST_NAME" ]; then
        continue
    fi
    
    echo "-------------------------------------------"
    echo "Test Case $i: $TEST_NAME"
    echo "-------------------------------------------"
    
    RESULT=$(curl -s -X POST "${BASE_URL}/v1/execute" \
      -H 'Content-Type: application/json' \
      -d @test-data/execute-requests/$TEST_NAME)
    
    DECISION=$(echo "$RESULT" | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['decision'])" 2>/dev/null)
    OK=$(echo "$RESULT" | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['ok'])" 2>/dev/null)
    REASONS=$(echo "$RESULT" | python3 -c "import sys, json; print(', '.join(json.load(sys.stdin)['data']['reasonCodes']))" 2>/dev/null)
    
    echo "Result:"
    echo "  Decision: $DECISION"
    echo "  OK: $OK"
    echo "  Reason Codes: $REASONS"
    echo ""
done

echo "=============================================="
echo "✅ All tests completed"
echo "=============================================="
echo ""
echo "📊 View detailed reports:"
echo "   - Summary: test-data/reports/test-summary.txt"
echo "   - Full report: test-data/reports/final-report.md"
echo "   - DRL content: test-data/responses/drl-formatted.txt"
