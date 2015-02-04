Install Across Multiple Servers
===============================

*Note that this is not a comprehensive guide of all possible ways to install Matterhorn. It is more like a guide to good
practice and presents what a lot of people are running.*

Step 1: Install Matterhorn
--------------------------

For a distributed set-up you basically only need to put the right modules onto the right node in the Matterhorn system.
To make things less complicated, these modules are grouped together as profiles which you can directly build and
install.

If you want to build Matterhorn yourself, you can invoke the build process for certain modules by using mavens `-P`
option. For example the following command will build the three profiles called worker-standalone, serviceregistry and
workspace (These are the profiles needed for a worker node):

    mvn clean install -DdeployTo=/path/to/matterhorn/ \
      -Pworker-standalone,serviceregistry,workspace

If you are using the Matterhorn RPM repository instead, you can do the same by installing the profile packages like
this:

    yum install opencast-matterhorn14-profile-worker-standalone \
      opencast-matterhorn14-profile-serviceregistry \
      opencast-matterhorn14-profile-workspace

To make things easier, the repository also contains a set of predefined distribution packages which will automatically
install all dependencies for a given node type. For example, to install a Matterhorn worker node:

    yum install opencast-matterhorn14-distribution-worker

This is the general idea behind a distributed set-up of Matterhorn. The following list will now give a list of examples
about how you could distribute Matterhorn over a given set of machines and what you need to install for that.  You
should be aware that these examples are not the only possible ways of setting up Matterhorn. They are, however, a good
way to start.

   *What is not specified in this list is the location of the database and the storage server. You can place them either
   on one of the Matterhorn nodes or create a dedicated machine for them. The latter will obviously give you more
   performance.*


### All-In-One

This is the default set-up described in the basic installation guides. It works fine for testing purposes should,
however, not be used in production. It is not distributed but is listed here to have a comprehensive list of necessary
profiles. For an All-In-One system the following profiles need to be installed:

    admin, dist, engage, worker, workspace, serviceregistry, directory-db

Maven build command:

    mvn clean install -DdeployTo=/path/to/matterhorn/

RPM Repository installation:

    yum install opencast-matterhorn14-distribution-default


### Two-Server Set-up

This set-up is the minimum set-up recommended for productive use. It will separate the distribution layer from the
administrative and working layer. This means that even if one server is under heavy load as videos are processed, etc.
it will not effect the distribution and users should still be able to watch videos smoothly. However, it might happen
that under heavy load the handling of the administrative ui gets a bit rough.

Necessary profiles to build:

    admin-worker: admin,workspace,dist-stub,engage-stub,worker,serviceregistry
    engage: engage-standalone,serviceregistry,dist-standalone,workspace

Maven build commands:

    # admin-worker
    mvn clean install -DdeployTo=/path/to/matterhorn/ \
      -Padmin,workspace,dist-stub,engage-stub,worker,serviceregistry
    # engage
    mvn clean install -DdeployTo=/path/to/matterhorn/ \
      -Pengage-standalone,serviceregistry,dist-standalone,workspace

RPM Repository installation:

    # admin-worker
    yum install opencast-matterhorn14-distribution-admin-worker
    # engage
    yum install opencast-matterhorn14-distribution-engage


### Three (or more) Server Set-up

While in the last example we have created one combined node for both the administrative tools and the workers, in this
example we will split this node into dedicated worker and admin nodes. Using this set-up it is easy to increase the
systems performance simply by adding further worker nodes to the system.

Necessary profiles to build:

    admin: admin,workspace,dist-stub,engage-stub,worker-stub,serviceregistry
    worker: serviceregistry,workspace,worker-standalone
    engage: engage-standalone,serviceregistry,dist-standalone,workspace

Maven build commands:

    # admin
    mvn clean install -DdeployTo=/path/to/matterhorn/ \
      -Padmin,workspace,dist-stub,engage-stub,worker-stub,serviceregistry
    # worker
    mvn clean install -DdeployTo=/path/to/matterhorn/ \
      -Pserviceregistry,workspace,worker-standalone
    # engage
    mvn clean install -DdeployTo=/path/to/matterhorn/ \
      -Pengage-standalone,serviceregistry,dist-standalone,workspace

RPM Repository installation:

    # admin
    yum install opencast-matterhorn14-distribution-admin
    # worker
    yum install opencast-matterhorn14-distribution-worker
    # engage
    yum install opencast-matterhorn14-distribution-engage


Step 2: Set-Up NFS Server
-------------------------

Though it is possible to have Matterhorn run without shared storage, it is still a good idea to do so, as hard links can
be used to link files instead of copying them and not everything has to be tunneled over HTTP.

Thus you should first set-up your NFS server. The best solution is certainly to have a dedicated storage server. For
smaller set-ups, however, it can also be put on one of the Matterhorn nodes, i.e. on the admin node.

To do this, you first have to install and enable the NFS server:

    yum install nfs-utils nfs-utils-lib
    chkconfig  --level 345 nfs on
    service nfs start

Later on you want to have one common user on all your systems that has access to the share as you do not want everyone to
have access. As preparation for this it makes sense to manually create a matterhorn user and group with a common UID and
GID. In the following example we use 992 as group id and 995 as user id for matterhorn:

    groupadd -g 1234 matterhorn
    useradd -g 1234 -u 1234 matterhorn

If the user and group id `1234` is already used, just pick another one but make sure to pick the same one on all your
Matterhorn nodes.

Then create the directory to be shared and set its ownership to the newly created users:

    mkdir -p /srv/matterhorn
    chown matterhorn:matterhorn /srv/matterhorn

Next we actually share the storage dir. For this we need to edit the file `/etc/exports` and set:

    /srv/matterhorn  131.173.172.190(rw,sync,no_subtree_check)

with 131.173.172.190 being the IP address of the other machine that should get access. Finally we enable the share with:

    exportfs -a

Of cause you have to open the necessary ports in your firewall configuration.  For iptables, appropriate rules could be
for example:

    -A INPUT -m state --state NEW -p tcp -m multiport --dport 111,892,2049,32803 -j ACCEPT
    -A INPUT -m state --state NEW -p udp -m multiport --dport 111,892,2049,32803 -j ACCEPT

You can set them by editing `/etc/sysconfig/iptables` and restarting the service afterwards.

Now you have set-up your storage server. What is still left to do is to mount the network storage on all other servers
of the matterhorn clusters except the capture agents. To do that you need to edit the `/etc/fstab` on each server and
add the command to mount the network storage on startup:

    storageserver.example.com:/srv/matterhorn /srv/matterhorn   nfs rw,hard,intr,rsize=32768,wsize=32768 0 0

*Important:* Do not use multiple NFS shares for different parts of the Matterhorn storage dir. Matterhorn will check if
hard links are possible across in a distributed set-up, but the detection may fail if hard links are only possible
between certain parts of the storage. This may lead to failures.



Step 3: Set-Up the Database
---------------------------

First make sure to follow the [regular database set-up
](https://opencast.jira.com/wiki/display/MHTRUNK/MySQL+Database+Configuration).

Do not forget to set the user also for the remote servers and grant them the necessary rights. Additionally, you need to
configure your firewall:

    -A INPUT -p tcp -s 131.173.172.190 --dport 3306 -m state --state NEW,ESTABLISHED -j ACCEPT



Step 4: Configure Matterhorn
----------------------------

You did already set-up and configured your database in the last step, but there is some more configuration you have to
do. First of all you should follow the Basic Configuration guide which will tell you how to set the login credentials
etc. After that continue with the following steps:

### config.properties

Set the server URL to the public url of each server (admin URL on admin, worker URL on worker, engage URL on engage, …).
This may either be this nodes IP address or preferable its domain name:

    org.opencastproject.server.url=http://<URL>:8080

Set the location of the shared storage directory:

    org.opencastproject.storage.dir=/srv/matterhorn

Define that the file repository shall access all files locally:

    org.opencastproject.file.repo.url=${org.opencastproject.admin.ui.url}

### load/org.opencastproject.organization-mh_default_org.cfg

Set the base URL of the server hosting the administrative tools. Again use a domain name instead of an IP address if
possible:

    org.opencastproject.admin.ui.url=http://<ADMIN-URL>:8080

Set the base URL of the server hosting the engage tools:

    org.opencastproject.engage.ui.url=http://<ENGAGE-URL>:8080

### services/org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl.properties

To ensure that jobs are not dispatched by non-admin nodes you may also want to set:

    dispatchinterval=0
