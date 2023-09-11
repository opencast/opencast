Example Installation
===========================================================================

The following instructions will focus on installing the most common Opencast setup meaning a distributed system across three nodes on Debian 12 VMs. If your setup differs, you can always refer to the linked sections for a more detailed instruction covering alternative options. The installation steps will be detailed so you can follow them step by step but for maintanance reasons of course it might make sense to automate some of the steps. This can be done for example with [ansible](ansible.md).


Hardware
---------------

For this installation three Debian 12 VMs with the following hardware will be used:

Admin node:

- Hostname: admin.example.com
- Four cores
- 8GB of memory

Worker node:

- Hostname: worker.example.com
- Four cores
- 8GB of memory

Presentation node:

- Hostname: presentation.example.com
- Four cores
- 4GB of memory

NFS share:

- Hostname: storageserver.example.com
- 5TB disk space

This setup will be sufficient for a basic distributed Opencast installation which can be used in production.


Prerequisites
------------


Database
------------
Opencast needs a database in order to operate and store information about events, workflows, metadata etc. For this setup a MariaDB database will be created on the Admin node but you can also use a seperate dedicated VM.

First install and start MariaDB:

```sh
% sudo apt update
% sudo apt install mariadb-server
% sudo systemctl start mariadb.service
% sudo systemctl enable mariadb.service
```

Then set secure root user credentials by running


    sudo mysql_secure_installation


The next step is to create a database for Opencast. For this start the mysql client as root


    mysql -u root -p


You will be asked for the previosly chosen password of the user root.
Next, create a database called `opencast` by executing:

```sql
CREATE DATABASE opencast CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then create a user `opencast` with a password and grant it all necessary rights:

```sql
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,DROP,INDEX,TRIGGER,CREATE TEMPORARY TABLES,REFERENCES ON opencast.*
  TO 'opencast'@'%' IDENTIFIED BY 'opencast_password';
```

Finally, leave the client and restart the database server to enable the new user(s):

    systemctl restart mariadb.service

NFS
------------

In the next step you will connect the NFS share to all of your opencast nodes.

You want to have one common user on all your systems, so that file permissions do not become an issue.. As preparation for this it makes sense to manually create an opencast user and group with a common UID and GID:

    sudo groupadd -g 1234 opencast
    sudo useradd -g 1234 -u 1234 opencast

Now you need to mount the network storage on all three servers of the Opencast clusters. To do that you need to edit the /etc/fstab on each server and add the command to mount the network storage on startup:

    storageserver.example.com:/srv/opencast /srv/opencast   nfs rw,hard,intr,rsize=32768,wsize=32768 0 0

After a reload the NFS share should be correctly mounted and ready to store the Opencast data.

    sudo mount -a


OpenSearch
------------
Opencast uses OpenSearch as a search index in order to cache and quickly access user data for the admin interface. Therefore it should be installed on the same node that serves the admin interface.

    sudo apt-get install opensearch

After installing an configuring make sure to start and enable the service:

    systemctl restart opensearch
    systemctl enable opensearch

Nginx
------------

Opencast
------------

Essential configuration
------------

Troubleshooting
------------