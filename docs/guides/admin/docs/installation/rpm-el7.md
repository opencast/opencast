Install from Repository (Red Hat Enterprise Linux 7.x, CentOS 7.x, Scientific Linux 7.x)
========================================================================================


<div class=warn>
While we  continue n to continue support for EL7 until the CentOS 7 EOL in mid 2024,
most testing happens on more modern platforms and we recommend using EL8 or EL9 instead.
</div>

This guide is based on an RPM software repository available for Red Hat based Linux distributions provided by [Osnabrück
University](https://uni-osnabrueck.de). This repository provides preconfigured Opencast installations and all necessary
3rd-party-tools.


Currently Supported
-------------------

- CentOS 7.x (x86\_64)
- Red Hat Enterprise Linux 7.x (x86\_64)
- Scientific Linux 7.x (x86\_64)

Other architectures like i386, i686, arm, … are not supported.


Activate Repository
-------------------

First you have to install the necessary repositories:

```sh
yum install -y https://pkg.opencast.org/rpms/release/el/7/oc-{{ opencast_major_version() }}/noarch/opencast-repository-{{ opencast_major_version() }}-1.el7.noarch.rpm
```

It might take some time after the release of a new Opencast version before the RPMs are moved to the stable repository.
Until then, you can use `/etc/yum.repos.d/opencast-testing.repo` instead to get the latest version.
Note that the testing repository is an additional repository and still requires the stable repository to be active.

You can check if the repositories were successfully enabled using:

```
yum repolist enabled
```


Install Opencast
------------------

For a basic all-in-one installation just run:

```sh
yum install opencast-allinone
```

This will install the default distribution of Opencast and all its dependencies.
For more options, see the [advanced installation section below](#advanced-installation).


Install Elasticsearch
---------------------

Opencast uses Elasticsearch as a search index and a cache for quick access to some data from user interfaces.
Make sure to install it on the node which also serves the admin interface.

```sh
yum install elasticsearch-oss
```

Opencast automatically configures the search index once it is connected.
The default configuration will work for a local Elasticsearch with no modifications.
The only exception for this is to add a configuration to mitigate Log4Shell.
For this, add a file `/etc/elasticsearch/jvm.options.d/log4shell.options` with the content:

```
-Dlog4j2.formatMsgNoLookups=true
```

Finally, make sure to start and enable the service:

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
yum search opencast
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
For example, if you install Opencast {{ opencast_major_version() }}.1,
you get the latest {{ opencast_major_version() }}.x release,
but no {{ opencast_major_version() | int + 1 }}.x release.

These instructions will upgrade Opencast to a new version which may be incompatible with older versions.
Thus, a rollback might not be possible.
If you are performing this on a production system, please ensure you have valid backups prior to taking the next steps.

For an RPM-based upgrade, first, stop Opencast:

```sh
systemctl stop opencast.service
```

Then, update the repository:

```sh
yum install -y https://pkg.opencast.org/rpms/release/el/7/oc-{{ opencast_major_version() }}/noarch/opencast-repository-{{ opencast_major_version() }}-1.el7.noarch.rpm
```

Upgrade to the new Opencast package by running:

```sh
yum update
```

At this point you must follow the relevant [upgrade instructions](../upgrade.md), prior to starting Opencast again.


Uninstall Opencast
--------------------

To uninstall Opencast, you can run:

```sh
yum remove opencast
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


Troubleshooting
---------------

### Missing Dependencies

If you try to install Opencast but yum is complaining about missing dependencies, please check if the epel repository is
really activated on your system. Some distributions come with epel preinstalled but disabled. The installation of the
epel-release package will not fix this. You can check what repositories are installed and enabled by executing
`yum repolist enabled` which should give you a list with epel, opencast and opencast-noarch in it. To enable a
repository, edit the configuration file in `/etc/yum.repos.d/`.
