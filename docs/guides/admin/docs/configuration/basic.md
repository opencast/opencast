Basic Configuration
===================

The basic configuration guide will help you to adjust the settings strongly recommended for each Opencast installation.
This is what you should do right after installing Opencast.
While there are alternatives for some of these settings, this is the recommended setup.

All settings changes are made to files residing in the Opencast configuration directory.
The location of the configuration directory depends on how you installed Opencast.
If you used the Linux packages, the location is `/etc/opencast`.


Step 1: Setting the Server URL
------------------------------

Find the property `org.opencastproject.server.url` in `etc/custom.properties` and set your domain name.
The value must be set to the URL from which the server can be accessed later.

    org.opencastproject.server.url=https://example.opencast.org

It is not supported for Opencast to be hosted in a subpath.
Opencast needs to be served from the root path element.
The RFC 3986 URI path component needs to be empty.

*Note:* This value will be written to all generated media packages and thus cannot be changed easily for already
processed media. Please think about this setting carefully.


Step 2: Setting Authentication Details
--------------------------------------

Configure authentication and security details of Opencast, including the login credentials.
For this, the important keys in the `etc/custom.properties` configuration file are:

* `org.opencastproject.security.admin.user`
    * The user for the administrative account. This is set to `admin` by default.
* `org.opencastproject.security.admin.pass`
    * The password for the administrative account. This is set to `opencast` by default.
* `org.opencastproject.security.digest.user`
    * The user for the communication between Opencast nodes. It is sometimes also used by capture agents.
      This is set to `opencast_system_account` by default.
* `org.opencastproject.security.digest.pass`
    * The password for the communication between Opencast nodes. It is sometimes also used by capture agents.
      This is set to `CHANGE_ME` by default.
* `karaf.shutdown.command`
    * The security token used for shutting down Opencast. Set this to a random string.

Make sure that these settings are identical on all nodes of the cluster.


Step 3: Database Configuration
------------------------------

Opencast uses an integrated H2 database by default, which has certain drawbacks:

* It cannot be used for distributed set-ups
* Upgrading Opencast with this database is not possible

The internal database will suffice for testing, however a stand-alone database is required for production uses.
Details about the configuration can be found at:

- [Database Configuration](database.md)


Step 4: Setting up OpenSearch
--------------------------------

Opencast requires OpenSearch. Instructions for installing OpenSearch can be found in the
[installation documentation](../installation/index.md).


Step 5: HTTPS Configuration
---------------------------

This configuration is required in order to:

- Make Opencast available externally
- Secure connections from/to Opencast

For this, follow one of the

- [configuration guides for HTTPS](https/index.md).


Step 6: Setting the Storage Directory (optional)
------------------------------------------------

If you want to use a specific location for storing media, metadata and other data,
you can set the directory by changing `org.opencastproject.storage.dir`.

    org.opencastproject.storage.dir=/path/to/data/folder

Often, an NFS mount is used for data storage.
Make sure that the user running Opencast has read/write permissions to the storage directory.
You can check that, for example, by running:

```no-highlight
sudo -u opencast touch /path/to/data/folder/test
sudo -u opencast rm /path/to/data/folder/test
```


Finish Installation
-------------------

If you came here as part of an installation, please head back to the installation guide you used for notes on how to run
Opencast as a service.
