Message Broker Configuration
============================
<!-- _"Since the release of version 2.x, ..." -->
Since version 2, Opencast requires an Apache ActiveMQ message broker as message relay for the administrative user
interface. ActiveMQ can either be set up to run on its own machine or on one of the existing Opencast nodes (usually the
admin node).

### Required Version

* ActiveMQ 5.10 or above


Installation
------------

There are multiple options for installing ActiveMQ:

* If you used the Opencast package repository, simply install the `activemq-dist` package.
* If you are running RHEL, CentOS or Fedora you can use the [ActiveMQ-dist Copr RPM
  repository](https://copr.fedoraproject.org/coprs/lkiesow/apache-activemq-dist/)
* Newer Debian based operating systems contain a sufficiently new version, however the ActiveMQ configuration file will
  require modification to function correctly.
* You can download binary distributions from the [Apache ActiveMQ website](http://activemq.apache.org/download.html)

<!-- _This is where things start to go up in flames. -->
Configuration
-------------

What you need to do:

1. Set up the required message queues for Opencast
2. Point all your Opencast nodes to your message broker.
3. Configure authentication and access control

The first task is easy. Opencast comes with an ActiveMQ configuration file, located at
`docs/scripts/activemq/activemq.xml` (RPM repo: `/usr/share/opencast/docs/scripts/activemq/activemq.xml`). This file
will give you a basic configuration with all queues set-up and accepting connections from the local host over TCP port
`61616`.

<!-- _This step is already mentioned AND explained more clearly in the rpm-el8.md doc at "Install Apache ActiveMQ". -->
Replacing the default ActiveMQ configuration with this file will already give you a fully functional ActiveMQ set-up for
an all-in-one server. You will find the configuration in the usual locations, e.g. `/etc/activemq/`. On Debian you
first need to activate or create a new ActiveMQ instance. For more details on that see
`/usr/share/doc/activemq/README.Debian`. <!-- _This might be better on a new line/list items or as a note at the beginning of the paragraph, "not all OSs configure the same: -Most do this, -Debian does this" -->

Note that the default configuration needs to be adjusted for distributed set-ups since:
<!-- _Instance of a difference between single and multiple servers/machines? -->
* ActiveMQ listens to localhost only (`activemq.xml`)
* Opencast tries to connect to ActiveMQ locally (`custom.properties`)
* No password is set (`activemq.xml`, `custom.properties`)

<!-- _Please be more explicit about when the user will be working in which directory (OC's or ActiveMQ's), e.g. state where to find the directory, or state beforehand that "until stated otherwise, the mentioned config files are found in ActiveMQ's config directory."  -->
### 1. Connection
<!-- _Unclear/Check:: Not sure if this is correct information: which `custom.properties` file is meant, OC's or ActiveMQ's? Can the location be given (i.e. where to find the directory as shown in rpm-el8.md "Install Apache ActiveMQ") -->
The ActiveMQ connection is configured in Opencast's `custom.properties` file. The default configuration points to a local
installation of ActiveMQ. You can easily configure this to point somewhere else:

    activemq.broker.url = failover://tcp://example.opencast.org:61616


### 2. Bind Host
<!-- _Unclear/Check:: In which file/directory are we finding this config setting, OC's or ActiveMQ's (I assume it's ActiveMQ)? -->
The default configuration tells ActiveMQ to listen to `127.0.0.1` only. On a distributed system, you want to set this to
`0.0.0.0` to listen to all hosts by changing the `transportConnector`:

    <transportConnector name="openwire" uri="tcp://127.0.0.1:61616?..."/>


### 3. Username and Password
<!-- _Unclear/Check: Not sure if this is correct information: In which files/directories are we finding all of the following config settings, OC's or ActiveMQ's (I assume it's ActiveMQ)? -->
ActiveMQ can secure its message queues by requiring login credentials. This section will go through the steps of setting
up a username and a password. Have a look at the [ActiveMQ security site](http://activemq.apache.org/security.html) for
details about using alternative authentication and authorization providers.

#### Step 1: Create ActiveMQ Admin User

First, you need to create a new user that will have access to the queues. This is configured in the `users.properties`
configuration file in the configuration directory for ActiveMQ. It is a list of the format `username = password` so, for
example, we could create a new admin user with the following file contents:

    admin=password

#### Step 2: Create ActiveMQ Admin Group

The next step is to provide a group that will have our user in it and will secure access to the message queues. This is
configured in the file `groups.properties` in the configuration directory for ActiveMQ. It is a list of the format
`group = user1,user2,…`. For example:

    groups=user1,user2,user3

To set-up our new user to be a part of the admins group:

    admins=admin

#### Step 3: Configure Users and Groups Configuration Files

Next, we need to make sure that ActiveMQ is using our `users.properties` and `groups.properties` files to authenticate
and authorize users. The `login.config` file should be in the ActiveMQ configuration directory and contain:

    activemq {
        org.apache.activemq.jaas.PropertiesLoginModule required
        org.apache.activemq.jaas.properties.user="users.properties"
        org.apache.activemq.jaas.properties.group="groups.properties";
    };

#### Step 4: Configure Message Broker Security

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

#### Step 5: Configure Opencast to Connect with Username and Password to Message Broker
<!-- _Unclear/Check: Not sure if this is correct information: In which file/directory are we finding this config setting, OC's or ActiveMQ's (I assume it's OC)? -->
Now that we have secured the queues, Opencast will complain that it is unable to connect, using the current username and
password. The username and password used above need to be added to the `custom.properties` file of Opencast.  There are
two properties to set:

    activemq.broker.username=admin
    activemq.broker.password=password

<!-- _Maybe make it more obvious that these next two configs are separate from the more basic config previously, e.g. write it in the heading "Other Optional Config" or something -->
Firewall
--------

Do not forget that ActiveMQ uses the TCP port 61616 (default configuration) for communication. You probably want to
allow communication over this port in your firewall on a distributed setup or explicitly forbid public access on an
all-in-one installation. <!-- _Instance of "probably": Wording should be more decisive: either it is necessary, or it is optional(/can also be highly recommended...) -->


Memory settings
---------------
<!-- _Make more clear that this is optional, e.g. write it in the heading -->
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
