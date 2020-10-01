Basic Configuration
===================

This guide will help you to change the basic configuration settings which are required or at least strongly recommended
for each Opencast installation. This is basically what you should do, right after installing Opencast on your machine.

All settings are made to files residing in the Opencast configuration directory. In most cases, that should be either
`/etc/opencast/` or `/opt/opencast/etc/`. Edit the files using the editor of your choice, e.g.:

    vim /etc/opencast/custom.properties


Step 1: Setting the Server URL
------------------------------

By default, only connections from the local machine are accepted by Opencast.  You want to change this if the system
should be accessible within a network.

First, find the property `org.opencastproject.server.url` in your `custom.properties` configuration file and set it to
your own domain name:

    org.opencastproject.server.url=http://example.com:8080

*Note:* This value will be written to all generated mediapackages and thus cannot be changed easily for already
processed media. At least not without an extra amount of work involving modifications to the database. That is why you
should think about this setting carefully.


Step 2: Setting the Login Details
---------------------------------

There are two authentication methods for Opencast. HTTP Digest authentication and form-based authentication. Both
methods need a username and a password. Change the password for both! The important keys in the 'custom.properties'
configuration file are:

* `org.opencastproject.security.admin.user`
    * The user for the administrative account. This is set to `admin` by default.
* `org.opencastproject.security.admin.pass`
    * The password for the administrative account. This is set to `opencast` by default.
* `org.opencastproject.security.digest.user`
    * The user for the communication between Opencast nodes, as well as for capture agents. This is set to
    `opencast_system_account` by default.
* `org.opencastproject.security.digest.pass`
    * The password for the communication between Opencast nodes and capture agents. This is set to `CHANGE_ME` by default.

*Note:* The digest credentials are also used for internal communication of Opencast servers. So these keys have to be
set to the same value on each of you Opencast nodes (Core, Worker, Capture Agent, …)


Step 3: Change the default shutdown command
-------------------------------------------

Karaf provides a socket over wich you can send a shutdown command. The socket does not provide any kind of
authentication. Therefore anyone who obtains write access to this socket is able to shutdown karaf and everything
that runs on it. There is a default `karaf.shutdown.command` defined in `custom.properties`. Change this to something
secret.


Step 4: Setting up Apache ActiveMQ Message Broker
-------------------------------------------------

Since version 2.0, Opencast requires a running Apache ActiveMQ instance with a specific configuration.  The message
broker is mostly run on the admin server of Opencast but can be run separately. It needs to be started before Opencast.
For more details about the setup, have a look at the [Apache ActiveMQ configuration guide](message-broker.md).


Step 5: Database Configuration
------------------------------

Opencast uses an integrated H2 database by default. While you will find it perfectly functional, it has certain
drawbacks:

* It is rather slow
* It cannot be used for distributed set-ups
* Upgrading Opencast with this database is not possible

For testing, it is totally fine to keep the internal database, but you are highly encouraged to switch to a stand-alone
database for productional use. For more information about database configuration, have a look at the [Database
Configuration](database.md) section.


Step 6: HTTPS Configuration
---------------------------

An installation without HTTPS does not make much sense today.
Thus, make sure to follow [a configuration guide for HTTPS](https/index.md).


Step 7: Setting the Storage Directory (optional)
------------------------------------------------

Even though it is not important for all systems – on test setups you can probably omit this – you will often want to set
the storage directory. This directory is used to store all media, metadata, … Often, an NFS mount is used for this. You
can set the directory by changing `org.opencastproject.storage.dir` like:

    org.opencastproject.storage.dir=/media/mhdatamount

Please keep in mind that the user running Opencast must have read/write permissions to the storage directory.

