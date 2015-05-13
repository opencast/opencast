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

In the Opencast Matterhorn configuration, there are currently two places for Active MQ configuration (*Work in progress
to put this to one central place*). These are:

 - `services/org.opencastproject.message.broker.impl.MessageReceiverImpl.properties`
 - `services/org.opencastproject.message.broker.impl.MessageSenderImpl.properties`

In both places, the connection to the message broker can be configured like this:

    activemq.broker.url = failover://tcp://example.opencast.org:61616


### Security

*To be determined!*

Ideas:

 - Limit `transportConnectores` (see `activemq.xml`) to certain hosts. This may require ActiveMQ to use multiple hosts.
   Have a look at the Matterhorn mailing list for the ongoing discussion.

Do not forget that ActiveMQ uses TCP port 61616 (default configuration) for communication which you might have to allow in your firewall.
