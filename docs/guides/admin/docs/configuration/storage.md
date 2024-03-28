Setting up a NFS Storage
====================
Though it is possible to have Opencast run without shared storage, it is still a good idea to do so, as hard links can
be used to link files instead of copying them and not everything has to be tunneled over HTTP.

Thus you should first set-up your NFS server. The best solution is certainly to have a dedicated storage server. For
smaller set-ups, however, it can also be put on one of the Opencast nodes, i.e. on the admin node.

The following sections describe both possible methods in detail.

Prerequisites
--------------

You want to have one common user on all your systems, so that file permissions do not become an issue. As preparation for this it makes sense to manually create an opencast user and group with a common UID and GID:

    sudo groupadd -g 1234 opencast
    sudo useradd -g 1234 -u 1234 opencast

If the user and group id `1234` is already used, just pick another one but make sure to pick the same one on all your
Opencast nodes.

Then create the directory to be shared and set its ownership to the newly created users. The directory used in this example will be `/srv/opencast`:

    mkdir -p /srv/opencast
    chown opencast:opencast /srv/opencast


Use a dedicated NFS storage
---------------------

To use a dedicated NFS storage you only need to mount the network storage on all three servers of the Opencast clusters. To do that you need to edit the `/etc/fstab` on each server and add the command to mount the network storage on startup:

    storageserver.example.com:/srv/opencast /srv/opencast   nfs rw,hard,intr,rsize=32768,wsize=32768 0 0

After a reload the NFS share should be correctly mounted and ready to store the Opencast data.

    sudo mount -a

Use the admin node
--------------------

To use the admin node as storage server, you first have to install and enable the NFS server:

    yum install nfs-utils nfs-utils-lib
    chkconfig  --level 345 nfs on
    service nfs start

Next we share the storage dir. For this we need to edit the file `/etc/exports` and set:

    /srv/opencast  131.173.172.190(rw,sync,no_subtree_check)

with 131.173.172.190 being the IP address of the other machine that should get access. You need to add a line with the corresponding IP for every other Opencast node. Finally we enable the share with:

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


Notes
------

*Important:* Remember that after you have configured your shared storage, you still need to configure it in the configuration file `custom.properties` so it will be used by Opencast:

    org.opencastproject.storage.dir=/srv/opencast

*Important:* With exception of the archive, do not use multiple NFS shares for different parts of the Opencast storage dir. Opencast will check if hard links are possible across in a distributed set-up, but the detection may fail if hard links are only possible between certain parts of the storage. This may lead to failures.

*Important:* Do not share the Karaf data directory. Doing so will cause Opencast to fail. Please share the storage
directory only.








