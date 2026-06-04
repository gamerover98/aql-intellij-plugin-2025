#!/bin/bash
# Build-time seeding script.
#
# /var/lib/arangodb3 is declared as VOLUME in the base image, so writes inside
# a RUN step are discarded. Instead we start ArangoDB against a plain directory
# (/arangodb_seed), run the init script, shut down cleanly, and fix ownership.
# The /arangodb_seed directory is then preserved in the image layer and copied
# into the real data directory by entrypoint.sh on the first container start.
set -eu

SEED_DIR=/arangodb_seed
ENDPOINT="tcp://127.0.0.1:8529"
INIT_SCRIPT=/arangodb_seed_init.js
MAX_ATTEMPTS=30

mkdir -p "$SEED_DIR"

echo "[seed] Starting ArangoDB for build-time seeding..."
arangod \
    --server.endpoint "$ENDPOINT" \
    --server.authentication false \
    --database.directory "$SEED_DIR" \
    --log.level error &
ARANGOD_PID=$!

echo "[seed] Waiting for ArangoDB to accept connections (max $((MAX_ATTEMPTS * 2))s)..."
READY=0
for i in $(seq 1 $MAX_ATTEMPTS); do
    if arangosh \
           --server.endpoint "$ENDPOINT" \
           --server.authentication false \
           --javascript.execute "db._version();" \
           >/dev/null 2>&1; then
        echo "[seed] ArangoDB ready after $((i * 2))s."
        READY=1
        break
    fi
    sleep 2
done

if [ "$READY" -eq 0 ]; then
    echo "[seed] ERROR: ArangoDB did not start within $((MAX_ATTEMPTS * 2)) seconds." >&2
    kill "$ARANGOD_PID" 2>/dev/null || true
    exit 1
fi

echo "[seed] Running init script..."
arangosh \
    --server.endpoint "$ENDPOINT" \
    --server.authentication false \
    --javascript.execute "$INIT_SCRIPT"

echo "[seed] Shutting down ArangoDB..."
kill "$ARANGOD_PID"
wait "$ARANGOD_PID" 2>/dev/null || true

# Ensure files are owned by the arangodb runtime user so that the copy
# performed by entrypoint.sh preserves the correct ownership (cp -a).
chown -R arangodb:arangodb "$SEED_DIR"

echo "[seed] Done. Snapshot stored in $SEED_DIR."
