#!/bin/bash
# Build-time seeding script.
# Starts arangod against /arangodb_seed (not the VOLUMEd path), inserts
# test data via arangosh, then shuts down cleanly.
set -e

SEED_DIR=/arangodb_seed
ENDPOINT="tcp://127.0.0.1:8529"
HTTP_URL="http://127.0.0.1:8529/_api/version"
INIT_SCRIPT=/arangodb_seed_init.js
MAX_WAIT_SECS=120

# ── Verify required binaries are in PATH ─────────────────────────────────────
for bin in arangod arangosh curl; do
    if ! command -v "$bin" >/dev/null 2>&1; then
        echo "[seed] ERROR: '$bin' not found. PATH=$PATH" >&2
        exit 1
    fi
    echo "[seed] $bin -> $(command -v "$bin")"
done

mkdir -p "$SEED_DIR"

# ── Start ArangoDB against the seed directory ─────────────────────────────────
echo "[seed] Starting arangod (database.directory=$SEED_DIR)..."
arangod \
    --server.endpoint "$ENDPOINT" \
    --server.authentication false \
    --database.directory "$SEED_DIR" \
    --log.level error \
    2>/tmp/arangod-build.log &
ARANGOD_PID=$!

# ── Wait for HTTP readiness (curl, no seq dependency) ────────────────────────
echo "[seed] Waiting for ArangoDB HTTP API (max ${MAX_WAIT_SECS}s)..."
ELAPSED=0
while [ "$ELAPSED" -lt "$MAX_WAIT_SECS" ]; do
    if curl -sf "$HTTP_URL" >/dev/null 2>&1; then
        echo "[seed] ArangoDB ready after ${ELAPSED}s."
        break
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

if ! curl -sf "$HTTP_URL" >/dev/null 2>&1; then
    echo "[seed] ERROR: ArangoDB did not start within ${MAX_WAIT_SECS}s." >&2
    echo "[seed] --- arangod log ---" >&2
    cat /tmp/arangod-build.log >&2
    kill "$ARANGOD_PID" 2>/dev/null || true
    exit 1
fi

# ── Run init script ───────────────────────────────────────────────────────────
echo "[seed] Running init script..."
arangosh \
    --server.endpoint "$ENDPOINT" \
    --server.authentication false \
    --javascript.execute "$INIT_SCRIPT"

# ── Graceful shutdown ─────────────────────────────────────────────────────────
echo "[seed] Shutting down arangod..."
kill "$ARANGOD_PID"
wait "$ARANGOD_PID" 2>/dev/null || true

# Fix ownership so the arangodb runtime user can read the snapshot
chown -R arangodb:arangodb "$SEED_DIR"
echo "[seed] Done. Snapshot stored in $SEED_DIR."
