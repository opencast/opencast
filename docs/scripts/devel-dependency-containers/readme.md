Runtime Dependencies for Developers
===================================

This directory contains container configurations for an easy way of starting the necessary runtime dependencies for
Opencast as a developer. You can use `docker compose`, `podman compose` or `podman-compose` to launch everything
necessary for the current version:

```sh
# Podman
podman compose up -d
# Docker
docker compose up -d
```

The default `docker-compose.yml` starts OpenSearch only, which is all Opencast needs. Use the `-f` flag to launch
additional services if required:

| File                            | Services                         |
|---------------------------------|----------------------------------|
| `docker-compose.yml`            | OpenSearch                       |
| `docker-compose-mariadb.yml`    | OpenSearch, MariaDB              |
| `docker-compose-postgresql.yml` | OpenSearch, PostgreSQL           |
| `docker-compose-all-sql.yml`    | OpenSearch, MariaDB, PostgreSQL  |

```sh
podman compose -f docker-compose-postgresql.yml up -d
```

The databases use the database `opencast`, the user `opencast` and the password `dbpassword`.

To shut down all services again, use `docker-compose-all-sql.yml`. It contains every service, so this also stops
containers started from any of the other files:

```sh
# Podman
podman compose -f docker-compose-all-sql.yml down
# Docker
docker compose -f docker-compose-all-sql.yml down
```

No persistence for any data is configured so that you always start with a clean system for development and testing.
