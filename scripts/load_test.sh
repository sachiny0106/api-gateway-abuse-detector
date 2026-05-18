#!/bin/bash

ENDPOINT="${1:-localhost:8080}"
API_KEY="${2:-test-api-key}"
CONCURRENT="${3:-50}"
REQUESTS="${4:-5000}"

echo "Running load test against $ENDPOINT"
echo "API Key: $API_KEY"
echo "Concurrent: $CONCURRENT, Total: $REQUESTS"

if ! command -v hey &> /dev/null; then
    echo "hey not installed. Installing..."
    go install github.com/rakyll/hey@latest
fi

echo ""
echo "=== Baseline Test (200 req/sec for 60s) ==="
hey -n $REQUESTS -c $CONCURRENT -H "X-API-Key: $API_KEY" http://$ENDPOINT/api/health

echo ""
echo "=== Rate Limit Test ==="
hey -n 500 -c 50 -H "X-API-Key: limited-key" http://$ENDPOINT/api/data

echo ""
echo "=== Brute Force Simulation ==="
for i in $(seq 1 50); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST -H "X-API-Key: attacker" http://$ENDPOINT/api/login
done | sort | uniq -c

echo ""
echo "=== Bot Pattern Test (100ms interval) ==="
hey -n 300 -c 10 -H "X-API-Key: bot-key" -qPS 100ms http://$ENDPOINT/api/data 2>/dev/null || true

echo ""
echo "Load test complete!"