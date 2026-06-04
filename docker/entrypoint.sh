#!/bin/bash
# Runtime entrypoint wrapper.
#
# On the very first container start (empty data volume or no volume at all),
# copies the seed snapshot baked into the image into the real data directory,
# then hands off to the official ArangoDB entrypoint (/entrypoint.sh).
#
# On subsequent starts the data directory is non-empty, so the copy is skipped
# and the container starts normally with whatever state it was left in.
set -e

DB_DIR=/var/lib/arangodb3
SEED_DIR=/arangodb_seed

if [ -z "$(ls -A "$DB_DIR" 2>/dev/null)" ]; then
    echo "[entrypoint] Empty data directory — restoring seed snapshot..."
    cp -a "$SEED_DIR/." "$DB_DIR/"
    echo "[entrypoint] Seed data restored to $DB_DIR."
fi

exec /entrypoint.sh "$@"
