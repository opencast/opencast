Install from Repository (Red Hat Enterprise Linux, CentOS Stream, …
===================================================================

This guide is based on an RPM software repository available for Red Hat based Linux distributions provided
by [Osnabrück University](https://uni-osnabrueck.de).
This repository provides preconfigured Opencast installations and all necessary 3rd-party-tools.

> In addition to this guide, we have also recorded [a full installation done in 30 minutes](https://vt.uos.de/71hfc)
> if you like to see how this works before you try it yourself.


Currently Supported
-------------------

We frequently test on CentOS Stream 8/9 and infrequently test on Red Hat Enterprise Linux 8/9,
trying to ensure that installations on those systems work without problems.
Since Rocky Linux and AlmaLinux OS are intended to be fully compatible to RHEL,
those distributions should work as well.
But we don't test against those.

We support only the x86\_64 architecture.


Activate Repository
-------------------

First you have to install the necessary repositories:

```sh
dnf install -y "https://pkg.opencast.org/rpms/release/el/$(rpm -E %rhel)/oc-{{ opencast_major_version() }}/noarch/opencast-repository-{{ opencast_major_version() }}-1.el$(rpm -E %rhel).noarch.rpm"
```

It might take some time after the release of a new Opencast version before the RPMs are moved to the stable repository.
Until then, you can use `/etc/yum.repos.d/opencast-testing.repo` instead to get the latest version.
Note that the testing repository is an additional repository and still requires the stable repository to be active.

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


Install Elasticsearch
---------------------

Opencast uses Elasticsearch as a search index and a cache for quick access to some data from user interfaces.
Make sure to install it on the node which also serves the admin interface.

```sh
dnf install elasticsearch-oss
```

Furthermore, the `analysis-icu` plugin for Elasticsearch is required to install. It is necessary for sorting naturally.
To install the ICU plugin, run the following:

    bin/elasticsearch-plugin install analysis-icu

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
dnf install -y \
  "https://pkg.opencast.org/rpms/release/el/$(rpm -E %rhel)/oc-{{ opencast_major_version() }}/noarch/opencast-repository-{{ opencast_major_version() }}-1.el$(rpm -E %rhel).noarch.rpm"
```

Upgrade to the new Opencast package by running:

```sh
dnf update
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
