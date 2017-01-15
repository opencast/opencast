# Testing Locally with Docker

Opencast is a complex system that requires multiple steps to install and configure properly. For quick local testing
this might be too complicated. The University of Münster provides multiple Docker images for Opencast that can simplify
this process. Required is only a `x86_64` Linux system with a running Docker Engine.

This method is ideal for new adopters, who just want to try out Opencast. It can also be used to test workflows. Because
of Docker's provided isolation, multiple instances of Opencast can run in parallel on a single system. This might be
helpful for developers.

## Install Docker

Docker is available for multiple Linux distributions. Please have a look at the [official
documentation](https://docs.docker.com/engine/installation/) for the latest installation instructions. Note that it
might be necessary to install [`docker-compose`](https://docs.docker.com/compose/install/) separately.

## Start with `docker-compose`

Opencast is packaged into multiple distributions. For each distribution there is a separate Docker image. Simple
installations can use the all-in-one distribution. Since version 2.0 Opencast requires Apache ActiveMQ. There are
multiple 3rd-party images for ActiveMQ; this documentation uses `webcenter/activemq`. As a data store you can currently
use HSQL or MySQL/MariaDB. The Docker Hub repository has official images for MySQL and MariaDB.

To automatically configure, start, and connect all services, `docker-compose` can be used. The [opencast-docker
repository](https://github.com/opencast/opencast-docker/tree/master/docker-compose) comes with multiple examples for
different configurations:

-   [all-in-one + HSQL](https://raw.githubusercontent.com/opencast/opencast-docker/master/docker-compose/docker-compose.allinone.hsql.yml)
-   [all-in-one + MariaDB](https://raw.githubusercontent.com/opencast/opencast-docker/master/docker-compose/docker-compose.allinone.mariadb.yml)
-   [admin, presentation, worker + MariaDB](https://raw.githubusercontent.com/opencast/opencast-docker/master/docker-compose/docker-compose.multiserver.mariadb.yml)

Choose a configuration and download it:

```sh
$ curl -o docker-compose.yml <URL>
```

You might want to edit the compose file to add volumes for custom configurations or workflows.

The compose files assume that the ActiveMQ configuration is located at `./assets/activemq.xml`. Additionally, if you use
MariaDB the SQL DDL commands for the Opencast database should be available at `./assets/opencast-ddl.sql`. You can
download both files from the repository:

```sh
$ mkdir assets
$ curl -o assets/activemq.xml https://raw.githubusercontent.com/opencast/opencast-docker/master/docker-compose/assets/activemq.xml
$ curl -o assets/opencast-ddl.sql https://raw.githubusercontent.com/opencast/opencast-docker/master/docker-compose/assets/opencast-ddl.sql
```

Alternatively, you can use the Docker images to generate these files. This has the advantage that the correct version is
used:

```sh
$ mkdir assets

$ docker run -it --rm \
    -e ORG_OPENCASTPROJECT_DB_VENDOR=MySQL \
    opencast/allinone:<version> \
    app:print:activemq.xml > assets/activemq.xml

$ docker run -it --rm \
    -e ORG_OPENCASTPROJECT_DB_VENDOR=MySQL \
    opencast/allinone:<version> \
    app:print:ddl > assets/opencast-ddl.sql
```

At this point you are ready to start Opencast with the `up` command:

```sh
$ docker-compose up
```

After downloading the necessary Docker images, `docker-compose` should start all relevant services and you should see
the logging output. Passing `-d` will start Opencast in the background. The admin UI is available at
<http://localhost:8080>. The `down` command will stop Opencast and remove the created Docker containers. All relevant
Opencast files are still preserved in Docker volumes. To remove them as well, pass the `-v` flag to `down`.
