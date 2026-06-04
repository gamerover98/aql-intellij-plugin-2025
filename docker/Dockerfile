FROM arangodb:latest

# Copy build-time scripts and init data
COPY docker/init/01_setup.js /arangodb_seed_init.js
COPY docker/seed.sh /seed.sh
RUN chmod +x /seed.sh && /seed.sh

# Runtime wrapper: seeds the data volume on first run, then delegates
COPY docker/entrypoint.sh /arangodb_entrypoint.sh
RUN chmod +x /arangodb_entrypoint.sh

ENTRYPOINT ["/arangodb_entrypoint.sh"]
CMD ["arangod"]
