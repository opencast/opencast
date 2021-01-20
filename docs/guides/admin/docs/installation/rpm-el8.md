Install from Repository (Red Hat Enterprise Linux 8.x, CentOS 8.x)
==================================================================

> *This guide is for EL8 only. There is a separate [CentOS 7 and Red Hat Enterprise Linux 7 guide](rpm-el7.md).*

This guide is based on an RPM software repository available for Red Hat-based Linux distributions provided by [Osnabrück
University](https://uni-osnabrueck.de). This repository provides preconfigured Opencast installations and all necessary
3rd-party-tools.

<div class=warn>
<b>Opencast 9</b> is currently available from the testing repository only.
</div>


Availability
------------

Note that it may take some time (usually about two weeks after a new release is out) before the RPMs are available.
Watch for announcements on [the users list](https://docs.opencast.org/#mailing-lists) or just check which versions are
available in the repository.


Currently Supported
-------------------

- CentOS 8.x (x86\_64)
- Red Hat Enterprise Linux 8.x (x86\_64)

Other architectures like i386, i686, arm, … are not supported.


Activate Repository
-------------------

First you have to install the necessary repositories:

```sh
dnf install -y https://pkg.opencast.org/rpms/release/el/8/oc-09/noarch/opencast-repository-9-1.el8.noarch.rpm
```

It might take some time after the release of a new Opencast version before the RPMs are moved to the stable repository.
Until then, you can use `/etc/yum.repos.d/opencast-testing.repo` instead to get the latest version.
Note that the testing repository is an additional repository and still requires the stable repository to be active.

<div class=warn>
For <b>Opencast 9</b> temporarily enable the testing repository in <code>/etc/yum.repo.d/opencast-testing.repo</code>
while it is not yet in the stable repository.
</div>

You can check if the repositories were successfully enabled using:

```
dnf repolist enabled
  epel
  epel-modular
  opencast
  opencast-noarch
  opencast-testing
  opencast-testing-noarch
```


Install Opencast
------------------

For a basic all-in-one installation just run:

```sh
dnf install opencast-allinone
```

This will install the default distribution of Opencast and all its dependencies.
For more options, see the [advanced installation section below](#advanced-installation).


Install Apache ActiveMQ
-----------------------

The Apache ActiveMQ message broker is required by Opencast.
It can be run on the same machine as Opencast.
Install ActiveMQ by running:

```sh
dnf install activemq-dist
```

A prepared configuration file for ActiveMQ comes with Opencast.
It should suffice for an all-in-one installation and can be copied to replace the default configuration:

```sh
cp /usr/share/opencast/docs/scripts/activemq/activemq.xml /etc/activemq/activemq.xml
```

Then start and enable ActiveMQ by running:

```sh
systemctl start activemq
systemctl enable activemq
```

More information about how to properly set up ActiveMQ for Opencast, cluster installations in particular,
can be found in the [message broker configuration documentation](../configuration/message-broker.md).


Install Elasticsearch
---------------------

Opencast uses Elasticsearch as a search index and a cache for quick access to some data from user interfaces.
Make sure to install it on the node which also serves the admin interface.

```sh
dnf install elasticsearch-oss
```

Opencast automatically configures the search index once it is connected.
The default configuration will work for a local Elasticsearch with no modifications.
Just make sure to start and enable the service:

```sh
systemctl start elasticsearch
systemctl enable elasticsearch
```


Configuration
-------------

Make sure to set your hostname, login information and other configuration details by following the

- [Basic Configuration guide](../configuration/basic.md)


Start Opencast
--------------

Finally, start and enable Opencast by running:

```sh
systemctl start opencast.service
systemctl enable opencast.service
```


Advanced Installation
---------------------

The basic installation will give you an all-in-one Opencast distribution on a single server.
For production, most users prefer deploying Opencast as a cluster, which allows for a better workload distribution.
You can list all available Opencast packages/distributions with:

```sh
dnf search opencast
```

This will list all available Opencast distributions in the form `opencast-<dist-type>`.
Some commonly used distributions are:

- `opencast-allinone`
- `opencast-admin`
- `opencast-presentation`
- `opencast-worker`


Upgrading
---------

Packages will automatically upgrade to the latest minor version in a release series when running `dnf update`.
They do not automatically upgrade the latest major version.
This is intentional since additional migration steps might be required.
For example, if you install Opencast 9.1, you get the latest 9.x release, but no 10 release.

These instructions will upgrade Opencast to a new version which may be incompatible with older versions.
Thus, a rollback might not be possible.
If you are performing this on a production system, please ensure you have valid backups prior to taking the next steps.

For an RPM-based upgrade, first, stop Opencast:

```sh
systemctl stop opencast.service
```

Then, replace the repository

```sh
rm -f /etc/yum.repos.d/opencast*.repo* || :
dnf install -y https://pkg.opencast.org/rpms/release/el/8/oc-09/noarch/opencast-repository-9-1.el8.noarch.rpm
```

Upgrade to the new Opencast package by running:

```sh
dnf  install opencast-<distribution>
```

At this point you must follow the relevant [upgrade instructions](../upgrade.md), prior to starting Opencast again.


Uninstall Opencast
--------------------

To uninstall Opencast, you can run:

```sh
dnf remove opencast
```

This will not touch your created media files or *modified* configuration files.
If you want to remove them as well, you have to do that by yourself.

```sh
# Remove media files (default location)
rm -rf /srv/opencast

# Remove local db, search indexes and working files
rm -rf /var/lib/opencast

# Remove configuration files
rm -rf /etc/opencast

# Remove logs
rm -rf /var/log/opencast
```
