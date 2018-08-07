Message Broker Configuration
============================

Since version 2, Opencast requires an Apache ActiveMQ message broker as message relay for the administrative user
interface. ActiveMQ can either be set up to run on its own machine or on one of the existing Opencast nodes (usually the
admin node).

### Required Version

* ActiveMQ 5.10 or above


Installation
------------

* If you use the Opencast package repository, simply install the `activemq-dist` package.
* If you are running RHEL, CentOS or Fedora you can use the [ActiveMQ-dist Copr RPM
  repository](https://copr.fedoraproject.org/coprs/lkiesow/apache-activemq-dist/)
* Newer Debian based operating systems contain a sufficient new version, however the ActiveMQ configuration file will
  require modification to function correctly.
* You can download binary distributions from the [Apache ActiveMQ website](http://activemq.apache.org/download.html)


Configuration
-------------

What you need to do:

* Set-up required message queues for Opencast
* Point all your Opencast nodes to your message broker.
* Configure authentication and access control

The first task is easy. Opencast comes with a ActiveMQ configuration file, located at
`docs/scripts/activemq/activemq.xml` (RPM repo: `/usr/share/opencast/docs/scripts/activemq/activemq.xml`). This file
will give you a basic configuration with all queues set-up and accepting connections from the local host over TCP port
`61616`.

Replacing the default ActiveMQ configuration with this file will already give you a fully functional ActiveMQ set-up for
an all-in-one server. You will find the configuration in the usually locations, e.g. `/etc/activemq/`. On Debian you
first need to activate or create a new ActiveMQ instance. For more details on that see
`/usr/share/doc/activemq/README.Debian`.

Note that the default configuration needs to be adjusted for distributed set-ups since:

* ActiveMQ listens to localhost only (`activemq.xml`)
* Opencast tries to connect to ActiveMQ locally (`custom.properties`)
* No password is set (`activemq.xml`, `custom.properties`)


### Connection

The ActiveMQ connection is configured in the `custom.properties`. The default configuration points to a local
installation of ActiveMQ. You can easily configure this to point somewhere else:

    activemq.broker.url = failover://tcp://example.opencast.org:61616


### Bind Host

The default configuration tells ActiveMQ to listen to `127.0.0.1` only. On a distributed system, you want to set this to
`0.0.0.0` to listen to all hosts by changing the `transportConnector`:

    <transportConnector name="openwire" uri="tcp://127.0.0.1:61616?..."/>


### Username and Password

ActiveMQ can secure its message queues by requiring login credentials. This section will go through the steps of setting
up a username and a password. Have a look at the [ActiveMQ security site](http://activemq.apache.org/security.html) for
details about using alternative authentication and authorization providers.

#### Create ActiveMQ Admin User

First, you need to create a new user that will have access to the queues. This is configured in the `users.properties`
configuration file in the configuration directory for ActiveMQ. It is a list of the format `username = password` so, for
example, we could create a new admin user with the following file contents:

    admin=password

#### Create ActiveMQ Admin Group

The next step is to provide a group that will have our user in it and will secure access to the message queues. This is
configured in the file `groups.properties` in the configuration directory for ActiveMQ. It is a list of the format
`group = user1,user2,…`. For example:

    groups=user1,user2,user3

To set-up our new user to be a part of the admins group:

    admins=admin

#### Configure Users and Groups Configuration Files

Next, we need to make sure that ActiveMQ is using our `users.properties` and `groups.properties` files to authenticate
and authorize users. The `login.config` file should be in the ActiveMQ configuration directory and contain:

    activemq {
        org.apache.activemq.jaas.PropertiesLoginModule required
        org.apache.activemq.jaas.properties.user="users.properties"
        org.apache.activemq.jaas.properties.group="groups.properties";
    };

#### Configure Message Broker Security

The final step to secure the ActiveMQ queues is to limit access to a specific group. This can be done by editing
`activemq.xml` in the ActiveMQ configuration directory. In this file, we need to add some XML in between these tags:

    <broker></broker>

We will add the following plugin configuration:

    <plugins>
      <jaasAuthenticationPlugin configuration="activemq" />
      <authorizationPlugin>
        <map>
          <authorizationMap>
            <authorizationEntries>
              <authorizationEntry queue=">" read="admins" write="admins" admin="admins" />
              <authorizationEntry topic=">" read="admins" write="admins" admin="admins" />
              <authorizationEntry topic="ActiveMQ.Advisory.>" read="admins" write="admins" admin="admins"/>
            </authorizationEntries>
          </authorizationMap>
        </map>
      </authorizationPlugin>
    </plugins>

* The `jaasAuthenticationPlugin` configures the broker to use our `login.config` file for the authentication.

        <jaasAuthenticationPlugin configuration="activemq" />

* The property:

        configuration=activemq

  needs to match the name given for surrounding object in `login.config` i.e. activemq{};

* The `authorizationEntry` restricts read, write and admin access for queues and topics to members of the group admins.

#### Configure Opencast to Connect with Username and Password to Message Broker

Now that we have secured the queues, Opencast will complain that it is unable to connect, using the current username and
password. The username and password used above need to be added to the `custom.properties` file of Opencast.  There are
two properties to set:

    activemq.broker.username=admin
    activemq.broker.password=password

## Firewall

Do not forget that ActiveMQ uses the TCP port 61616 (default configuration) for communication. You probably want to
allow communication over this port in your firewall on a distributed setup or explicitly forbid public access on an
all-in-one installation.

## Memory settings

When ActiveMQ is under heavy load it may require additional RAM. There are two places to change this:

In `docs/scripts/activemq/activemq.xml`:

    ...
    <systemUsage>
      <systemUsage>
        <memoryUsage>
          <!--<memoryUsage percentOfJvmHeap="70" />-->
          <memoryUsage limit="2048 MB"/>
    ...

This controls the allowed memory of ActiveMQ inside of its JVM instance. For more information see [the ActiveMQ
documentation](http://activemq.apache.org/javalangoutofmemory.html)

In `/usr/share/activemq/bin/env`:

    ACTIVEMQ_OPTS_MEMORY="-Xms64M -Xmx4G"

These are the classic JVM minimum and maximum memory flags.
