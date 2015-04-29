Basic Configuration
===================

This guide will help you to change the basic configuration settings which are required or at least strongly recommended
for each Matterhorn installation. This is basically what you should do directly after you installed Matterhorn on your
machine.

All these settings are made in the config.properties file. It can be found directly in you Matterhorn configuration
directory. In most cases, that should be /etc/matterhorn/config.properties. Open this file using the editor of your
choice, i.e.:
```
  vim /etc/matterhorn/config.properties
```


Step 1: Setting the Server URL
------------------------------

The first thing you should do is to set the server URL. To do that, find the property org.opencastproject.server.url in
your config.properties configuration file. This key is set to http://localhost:8080 by default.  That will only allow
access from the local machine. You should change this if your server should be accessible within a network. Set it to
your domain name or IP address like:
```
  org.opencastproject.server.url=http://example.com:8080
```
*Note:* This value will be witten to all generated mediapackages and thus cannot be changed for already generated media
afterwards. At least not without problems. So think about this setting for a minute.


Step 2: Setting the Login Details
---------------------------------

There are two authentication methods for Matterhorn. HTTP Digest authntication and form based authentication. Both
methods need a username and a password. It's obvious why you should change these. The important keys for this are:

 - `org.opencastproject.security.admin.user`: The user for the administrative account. This is set to “admin” by
   default. You should definitely change the credentials if your server is reachable from the internet.
 - `org.opencastproject.security.admin.pass`: The password for the administrative account. This is set to “opencast” by
   default. You should definitely change the credentials if your server is reachable from the internet.
 - `org.opencastproject.security.digest.user`: The user for the communication between Matterhorn nodes. This is set to
   “matterhorn_system_account” by default.
 - `org.opencastproject.security.digest.pass`: The password for the communication between Matterhorn nodes. This is set
   to “CHANGE_ME” by default.

*Note:* The Digest credentials are also used for internal communication of matterhorn servers. So these keys have to be
set to the same value on each of you Matterhon nodes (Core, Worker, Capture Agent, …)


Step 3: Setting the Storage Directory (optional)
------------------------------------------------

Even though it is not important for all systems – on test setups you can probably omit this – you will often want to set
the storage directory. This directory is used to store all media, metadata, … Often, a NFS mount is used for this. You
can set the directory by changing org.opencastproject.storage.dir like:
```
  org.opencastproject.storage.dir=/media/mhdatamount
```


Step 4: Database Configuration (optional)
-----------------------------------------

Matterhorn uses an integrated HSQL database by default. While you will find it perfectly working, it is rather slow. So
you are highly encouraged to switch to a stand-alone database for productional use. For more information about database
configuration, have a look at the [Database Configuration section](database).
