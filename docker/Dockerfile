FROM arangodb:latest

# Copy all helper files in one layer
COPY docker/init/01_setup.js  /arangodb_seed_init.js
COPY docker/seed.sh           /seed.sh
COPY docker/entrypoint.sh     /arangodb_entrypoint.sh

# Normalize line endings (CRLF → LF) for files authored on Windows,
# then make scripts executable and run the build-time seeding.
RUN sed -i 's/\r//' /seed.sh /arangodb_entrypoint.sh /arangodb_seed_init.js \
    && chmod +x /seed.sh /arangodb_entrypoint.sh \
    && /seed.sh

ENTRYPOINT ["/arangodb_entrypoint.sh"]
CMD ["arangod"]
