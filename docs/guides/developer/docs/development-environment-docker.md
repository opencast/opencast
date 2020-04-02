# Development Environment with Docker

Setting up and maintaining a proper Opencast build environment can be challenging. The `quay.io/opencast/build` Docker image,
developed by the University of Münster, provides such a build environment already configured and ready to use. In fact,
because of Docker's isolation functionality, multiple environments can be operated side by side on a single machine.

## Setting up a Docker build environment

Two `docker-compose` files are provided to start up different development environments. You also need the ActiveMQ configuration
(see "Testing Locally with Docker" guide in the administration documentation).


First, download the support assets:
```sh
$ mkdir assets
$ curl -o assets/activemq.xml https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/assets/activemq.xml
$ curl -o assets/opencast-ddl.sql https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/assets/opencast-ddl.sql
```

Now create a folder where the Opencast repository should be located, and expose its path as an environment variable. You
must also create the local Maven repository if it does not already exist.

```sh
$ mkdir -p opencast ~/.m2
$ export OPENCAST_SRC=$PWD/opencast
```

The `OPENCAST_SRC` variable is used in the compose file to set up a Docker volume so that the host and Docker container
can share the Opencast codebase. Similarly, the local Maven repository is shared in order to persist Maven artifacts
beyond the lifetime of the Docker container. If you do not want to use the default path `~/.m2` you can set the
`M2_REPO` variable to any other directory on the host system.

Next, you should specify your UID and GID. A matching user will then be created within the container so that all new
files can also be accessed from the host. If these variables remain unset, both default to 1000.

```sh
$ export OPENCAST_BUILD_USER_UID=$(id -u)
$ export OPENCAST_BUILD_USER_GID=$(id -g)
```

## Single node Opencast Development

Now download the Docker compose file:
```sh
$ curl -o docker-compose.yml https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/docker-compose.build.yml
```

With this you are ready to start up the build environment:

```sh
$ docker-compose up -d
```


You can enter the Opencast build environment with the `exec` command. Omitting the `--user opencast-builder` argument
would give you a root shell, but that is not necessary because the user `opencast-builder` can use `sudo` within the
container.

```sh
$ docker-compose exec --user opencast-builder opencast bash
```

There are multiple helper scripts available within the container:

```sh
# Clone the Opencast source code to the shared volume.
$ oc_clone

# Build Opencast.
$ oc_build

# Install Opencast in the same way as it would be installed in the other Opencast Docker images.
$ oc_install <distribution>

# Run the installed Opencast
$ oc_run

# Uninstall Opencast.
$ oc_uninstall

# Remove all Opencast files (database, media packages, etc.).
$ oc_clean_data
```

These scripts are provided to automate common tasks, but you can also run the necessary commands directly. The install
script has the advantage that it automatically connects Opencast to the configured ActiveMQ instance available at
`tcp://activemq:61616`.

Since the Opencast code is shared, any change from an IDE is directly visible within the container.

## Multi-node Opencast Development

Development with multi-node Opencast environments are also supported using a different compose file.  Instead of
downloading the single-node Docker compose file, download the multi-node version:

```sh
$ curl -o docker-compose.yml https://raw.githubusercontent.com/opencast/opencast-docker/<version>/docker-compose/docker-compose.multiserver.build.yml
```

This file defines a three node (admin, presentation, worker) cluster for use in testing, with all of the appropriate
ports exported.  To access the a node run ```docker-compose exec --user opencast-builder opencast-$nodetype bash```.  For
example, to access the presentation node run ```docker-compose exec --user opencast-builder opencast-presentation bash```.

Available commands are otherwise identical.

## Attaching a Remote Debugger to Karaf

By default, the compose file sets the necessary variables to enable remote debugging. The network port is published by
the container so that you can connect the remote debugger of your IDE to the port `5005` on `localhost`.  For multi-node
setups each node has its debug port exposed: admin lives on `5005`, presentation on `5006`, and worker on `5007`.
