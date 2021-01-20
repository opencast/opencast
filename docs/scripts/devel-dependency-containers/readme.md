Runtime Dependencies for Developers
===================================

This directory contains container configurations for an easy way of starting the necessary runtime dependencies for
Opencast as a developer. You can use `docker-compose` or `podman-compose` to launch everything necessary for the current
version:

```sh
# Podman
podman-compose up -d
# Docker
docker-compose up -d
```

To shut down all services again:

```sh
# Podman
podman-compose down
# Docker
docker-compose down
```

You can use the `-f` flag to launch specific compose files with additional services if required.

```sh
podman-compose -f docker-compose-postgresql.yml up -d
```

No persistence for any data is configured so that you always start with a clean system for development and testing.


SELinux
-------

Make sure to label the ActiveMQ configuration file for container usage:

```sh
chcon -Rt container_file_t ../activemq/activemq.xml
```
