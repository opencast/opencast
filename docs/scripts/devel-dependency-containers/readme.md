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

You can use the `-f` flag to launch specific compose files with additional services if required.

```sh
podman-compose -f docker-compose-postgresql.yml up -d
```

This also applies to other `{docker,podman}-compose` commands.

To shut down all services again:

```sh
# Podman
podman-compose stop
# Docker
docker-compose stop
```

The containers are automatically shut down when you restart your physical machine, and they are **not**
automatically started on the next boot.

About state
-----------

Note that the `elasticsearch` and the database containers persist their state in anonymous volumes by default.
That means: They retain their state/data cross any restarts you perform as stated above. This is true
**even across different compose files**.

If you want to start with a fresh system, you can clean everything up by running something like

```sh
# Docker
docker-compose down --volumes
# Podman
podman-compose down --volumes
```

before starting the containers anew. This stops and removes the containers, and all associated networks
and named and anonymous volumes. Removing the anonymous volumes using the `--volumes` switch is optional
but recommended, since otherwise these accumulate on your system over time.

SELinux
-------

Make sure to label the ActiveMQ configuration file for container usage:

```sh
chcon -Rt container_file_t ../activemq/activemq.xml
```
