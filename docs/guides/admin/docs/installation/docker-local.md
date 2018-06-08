# Testing Locally with Docker

Opencast is a complex system that requires multiple steps to install and configure properly, which may be too
complicated for quick, local testing. Therefore, the University of Münster provides various Docker images for Opencast
that can simplify this process. The only requirement is an `x86_64` Linux system with a running Docker Engine.

This method is ideal for new adopters who just want to try out Opencast. It can also be used to test workflows. Because
of the isolation that Docker provides, multiple instances of Opencast can run in parallel on a single system. This might
be helpful for developers.

## Install Docker

Docker is available for multiple Linux distributions. Please have a look at the [official
documentation](https://docs.docker.com/engine/installation/) for the latest installation instructions. Note that it
might be necessary to install [`docker-compose`](https://docs.docker.com/compose/install/) separately.

## Start with docker-compose

Opencast is packaged into multiple distributions. There is a separate Docker image for each distribution. Simple
installations can use the all-in-one distribution.

Opencast requires a database and a message broker (Apache ActiveMQ). We currently support H2 or MySQL/MariaDB databases.
The Docker Hub repository has official Docker images for MySQL and MariaDB. H2 is already integrated into Opencast so
that no database container is needed. There are multiple 3rd-party Docker images for ActiveMQ; this guide uses
`webcenter/activemq`.

`docker-compose` can be used to configure, start and connect all services automatically. The [opencast-docker
repository](https://github.com/opencast/opencast-docker/tree/master/docker-compose) contains multiple configuration
examples:

| Configuration                         | Compose file                                                                                                                 |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| all-in-one + H2                       | `https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/docker-compose.allinone.h2.yml`         |
| all-in-one + MariaDB                  | `https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/docker-compose.allinone.mariadb.yml`    |
| admin, presentation, worker + MariaDB | `https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/docker-compose.multiserver.mariadb.yml` |

Choose and download a configuration:

```sh
$ curl -o docker-compose.yml <URL>
```

You might want to edit the compose file and add extra volumes to include custom configurations or workflows (see
[compose file reference](https://docs.docker.com/compose/compose-file/)).

The compose files assume that the ActiveMQ configuration is located at `./assets/activemq.xml`. Additionally, if you use
MariaDB, the SQL DDL commands for the Opencast database must be available at `./assets/opencast-ddl.sql`. You can
download both files from the repository:

```sh
$ mkdir assets
$ curl -o assets/activemq.xml https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/assets/activemq.xml
$ curl -o assets/opencast-ddl.sql https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/assets/opencast-ddl.sql
```

Alternatively, you can use the Docker images to generate these files. This has the advantage that the correct version is
always used:

```sh
$ mkdir assets

$ docker run -it --rm \
    quay.io/opencast/allinone:<version> \
    app:print:activemq.xml > assets/activemq.xml

$ docker run -it --rm \
    -e ORG_OPENCASTPROJECT_DB_VENDOR=MySQL \
    quay.io/opencast/allinone:<version> \
    app:print:ddl > assets/opencast-ddl.sql
```

At this point you are ready to start Opencast with the `up` command:

```sh
$ docker-compose up
```

After downloading the necessary Docker images, `docker-compose` should start all relevant services and you should see
the logging output. Alternatively, adding the `-d` flag will start Opencast in the background and hide the log messages.
The admin UI is available at <http://localhost:8080>.

The `down` command will stop Opencast and remove the created Docker containers. All relevant Opencast files are still
preserved in Docker volumes. To remove them as well, run `down -v` instead.
