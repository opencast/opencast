Install Across Multiple Servers
===============================

*Note that this is not a comprehensive guide of all possible ways to install Opencast. It is more like a guide to good
practices and presents what a lot of people are running.*


Step 1: Install Opencast
--------------------------

Opencast consists of a large set of modules which together build the whole system. In a distributed set-up, different
kinds of nodes are basically only defined by the existence or absence of specific modules.

While it is possible to stick together a system module by module, opencast comes with a set of pre-defined distribution
which can directly be built and installed. To build these distributions, you would compile Opencast just like it is
outlined in the basic installation guides and will then find a set of different distributions, both as archive and in a
separate directory.

To list all distributions, run the following command after Opencast is built:

    % ls -1 build/*.tar.gz
    build/opencast-dist-admin-${version}.tar.gz
    build/opencast-dist-allinone-${version}.tar.gz
    build/opencast-dist-presentation-${version}.tar.gz
    build/opencast-dist-worker-${version}.tar.g
    ...


The same distributions can be found in the packages provided in the Opencast RPM repository.  These packages will
automatically install all dependencies for a given node type. For example, to install an Opencast worker node, you would
install the package `opencast21-distribution-worker`.

The following list describes possible set-ups:

### All-In-One

This is the default set-up described in the basic installation guides. It works fine for testing purposes. It should
usually not be used in production. It is not distributed but is listed here to have a comprehensive list of predefined
distributions.


### Two-Server Set-up

This set-up is the minimum set-up recommended for productive use. It will separate the video processing from the rest of
the system, making the user-facing parts of your system much less affected by heavier loads.


### Three (or more) Server Set-up

While in the last example we have created one combined node for both the administrative tools and the workers, in this
example we will split it into dedicated worker and admin nodes. Using this set-up it is easy to increase the systems
performance simply by adding further worker nodes to the system.



Step 2: Set-Up NFS Server
-------------------------

Though it is possible to have Opencast run without shared storage, it is still a good idea to do so, as hard links can
be used to link files instead of copying them and not everything has to be tunneled over HTTP.

Thus you should first set-up your NFS server. The best solution is certainly to have a dedicated storage server. For
smaller set-ups, however, it can also be put on one of the Opencast nodes, i.e. on the admin node.

To do this, you first have to install and enable the NFS server:

    yum install nfs-utils nfs-utils-lib
    chkconfig  --level 345 nfs on
    service nfs start

You want to have one common user on all your systems, so that file permissions do not become an issue.. As preparation
for this it makes sense to manually create an *opencast* user and group with a common UID and GID:

    groupadd -g 1234 opencast
    useradd -g 1234 -u 1234 opencast

If the user and group id `1234` is already used, just pick another one but make sure to pick the same one on all your
Opencast nodes.

Then create the directory to be shared and set its ownership to the newly created users:

    mkdir -p /srv/opencast
    chown opencast:opencast /srv/opencast

Next we actually share the storage dir. For this we need to edit the file `/etc/exports` and set:

    /srv/opencast  131.173.172.190(rw,sync,no_subtree_check)

with 131.173.172.190 being the IP address of the other machine that should get access. Finally we enable the share with:

    exportfs -a

Of cause you have to open the necessary ports in your firewall configuration.  For iptables, appropriate rules could be
for example:

    -A INPUT -m state --state NEW -p tcp -m multiport --dport 111,892,2049,32803 -j ACCEPT
    -A INPUT -m state --state NEW -p udp -m multiport --dport 111,892,2049,32803 -j ACCEPT

You can set them by editing `/etc/sysconfig/iptables` and restarting the service afterwards.

Now you have set-up your storage server. What is still left to do is to mount the network storage on all other servers
of the Opencast clusters except the capture agents. To do that you need to edit the `/etc/fstab` on each server and add
the command to mount the network storage on startup:

    storageserver.example.com:/srv/opencast /srv/opencast   nfs rw,hard,intr,rsize=32768,wsize=32768 0 0

*Important:* Do not use multiple NFS shares for different parts of the Opencast storage dir. Opencast will check if
hard links are possible across in a distributed set-up, but the detection may fail if hard links are only possible
between certain parts of the storage. This may lead to failures.

*Important:* Do not share the Karaf data directory. Doing so will cause Opencast to fail. Please share the storage
directory only.


Step 3: Set-Up the Database
---------------------------

First make sure to follow the [regular database set-up](../configuration/database.md).

Do not forget to set the user also for the remote servers and grant them the necessary rights. Additionally, you need to
configure your firewall:

    -A INPUT -p tcp -s 131.173.172.190 --dport 3306 -m state --state NEW,ESTABLISHED -j ACCEPT



Step 4: Set-Up ActiveMQ
-----------------------

Since version 2, Opencast requires an Apache ActiveMQ message broker as message relay for the administrative user
interface. ActiveMQ can either be set up to run on its own machine or on one of the existing Opencast nodes (usually the
admin node).

ActiveMQ 5.10 or above should work. ActiveMQ 5.6 will not work. Versions in between are untested.


### Installation

* If you use the Opencast RPM repository, simply install the `activemq-dist` package.
* If you are running RHEL, CentOS or Fedora you can use the [ActiveMQ-dist Copr RPM
  repository](https://copr.fedoraproject.org/coprs/lkiesow/apache-activemq-dist/)
* You can download binary distributions from the [Apache ActiveMQ website](http://activemq.apache.org/download.html)


### Configuration

What you basically need to do is to point all your Opencast nodes to your message broker. For more information about
the configuration, have a look at the [Message Broker Set-Up Guide](../configuration/message-broker.md).

Do not forget that ActiveMQ uses TCP port 61616 (default configuration) for communication which you might have to allow
in your firewall.


Step 5: Configure Opencast
----------------------------

You did already set-up and configured your database and message broker in the last steps, but there is some more
configuration you have to do. First of all you should follow the Basic Configuration guide which will tell you how to
set the login credentials etc. After that continue with the following steps:

### custom.properties

Set the server URL to the public URL of each server (admin URL on admin, worker URL on worker, presentation URL on
presentation, …).  This may either be this nodes IP address or preferable its domain name:

    org.opencastproject.server.url=http://<URL>:8080

Set the location of the shared storage directory:

    org.opencastproject.storage.dir=/srv/opencast

### org.opencastproject.organization-mh\_default\_org.cfg

Set the base URL of the server hosting the administrative tools. Again use a domain name instead of an IP address if
possible:

    prop.org.opencastproject.admin.ui.url=http://<ADMIN-URL>:8080

Set the base URL of the server hosting the engage tools (usually the presentation node):

    prop.org.opencastproject.engage.ui.url=http://<ENGAGE-URL>:8080

Set the base URL of the file server. When using a shared filesystem between servers,
set all servers to use the same URL (e.g. URL of the admin node).

    prop.org.opencastproject.file.repo.url=http://<ADMIN-URL>:8080

### org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.cfg

To ensure that jobs are not dispatched by non-admin nodes, on these you should also set:

    dispatchinterval=0
