Message Broker Configuration
============================

Since version 2, Opencast Matterhorn requires an Apache ActiveMQ message broker as message relay for the administrative
user interface. ActiveMQ can either be set up to run on its own machine or on one of the existing Matterhorn nodes
(usually the admin node).

### Required Version

 - ActiveMQ 5.10 or above should work.
 - ActiveMQ 5.6 will not work.
 - Versions in between are untested.


### Installation

 - If you use the Matterhorn RPM repository, simply install the `activemq-dist` package.
 - If you are running RHEL, CentOS or Fedora you can use the [ActiveMQ-dist Copr RPM repository
   ](https://copr.fedoraproject.org/coprs/lkiesow/apache-activemq-dist/)
 - You can download binary distributions from the [Apache ActiveMQ website](http://activemq.apache.org/download.html)


### Configuration

What you basically need to do:

 - Set-up required message queues for Matterhorn
 - Point all your Matterhorn nodes to your message broker.

The first task is easy. Matterhorn comes with a ActiveMQ configuration file, located at
`docs/scripts/activemq/activemq.xml` (RPM repo: `/usr/share/matterhorn/docs/scripts/activemq/activemq.xml`). This file
will give you a basic configuration with all queues set-up and accepting connections from all hosts over TCP port
61616.`Simply replace the default ActiveMQ configuration, usually located at `/etc/activemq/activemq.xml`, with this
file.

Then configure the ActiveMQ connectvion in the `config.properties`. The default configuration points to a local
installation of ActiveMQ, but that can be changed with:

    activemq.broker.url = failover://tcp://example.opencast.org:61616


### Security
ActiveMQ can secure its message queues with user name and password access. This will go through the steps of setting up a configured username and password. On the [ActiveMQ security site](http://activemq.apache.org/security.html) there are more details about using alternative authentication and authorization providers.
#### Create ActiveMQ Admin User
First you need to create a new user that will have acess to the queues. This is configured in the user.properties configuration file in the configuration directory for ActiveMQ. It is a list of "username=password" so for example we could create a new admin user with the following file contents:

    admin=password

#### Create ActiveMQ Admin Group
Next is to provide a group that will have our user in it and will secure access to the message queues. This is configured in the groups.properties configuration file in the configuration directory for ActiveMQ. It is a list of groups equal to a comma separated list of users so for example:

    groups=user1,user2,user3

To setup our new user to be a part of the admins group:

    admins=admin

#### Configure Users and Groups Configuration Files
Next we need to make sure that ActiveMQ is using our user.properties and group.properties files to authenticate and authorize users. The login.conf file should be in the ActivemQ configuration directory and contain: 

    activemq {
        org.apache.activemq.jaas.PropertiesLoginModule required
        org.apache.activemq.jaas.properties.user="users.properties"
        org.apache.activemq.jaas.properties.group="groups.properties";
    };

#### Configure Message Broker Security
The final step to secure the ActiveMQ queues by limiting them with a group. This can be done by editing the activemq.xml configuration file in the ActiveMQ configuration directory. Inside this configuration file we need to add some XML in between the tags:

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

The jaasAuthenticationPlugin configures the broker to use our login.conf file to do the authentication.

    <jaasAuthenticationPlugin configuration="activemq" />

 The property:

    configuration=activemq

needs to match the name given for surrounding object in login.conf i.e. activemq{};

The authorizationEntry gives read, write and admin access to only those members in the group admins for queues and topics.

##### Configure Matterhorn to Connect with Username and Password to Message Broker
Now that we have secured the queues if you try to start Matterhorn it will complain that it is unable to connect with the current username and password. The username and password used above needs to be added to the config.properties file normally in /etc/matterhorn/. There are two properties to set:

    activemq.broker.username=admin
    activemq.broker.password=password

## Firewall
Do not forget that ActiveMQ uses TCP port 61616 (default configuration) for communication which you might have to allow in your firewall.
