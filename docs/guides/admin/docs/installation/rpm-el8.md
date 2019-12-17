Install from Repository (Red Hat Enterprise Linux 8.x, CentOS 8.x)
==================================================================

> *This guide is for EL8 only. There is a separate [CentOS 7 and Red Hat Enterprise Linux 7 guide](rpm-el7.md).*

---

> *This installation **is still experimental**.*

---

This guide is based on a RPM software repository available for Red Hat-based Linux distributions provided by Osnabrück
University. This repository provides preconfigured Opencast installations and all necessary 3rd-Party-Tools. Using this
method, you do not have to compile the software by yourself.


Availability
------------

Note that it may take some time (usually about two weeks after a new release is out) before the RPMs are available.
Watch for announcements on [the users list](https://docs.opencast.org/#mailing-lists) or just check which versions are
available in the repository.


Currently Supported
-------------------

- CentOS 8.x (x86\_64)
- Red Hat Enterprise Linux 8.x (x86\_64)

> *Other architectures like i386, i686, arm, … are not supported!*


Registration
------------

Before you can start you need to get an account for the repository. You will need the credentials that you got via
e-mail after the registration to successfully complete this guide.

Get your account on [pkg.opencast.org](https://pkg.opencast.org).


Activate Repository
-------------------

First you have to install the Opencast repository:

    cd /etc/yum.repos.d
    curl -O https://pkg.opencast.org/opencast-testing.repo \
       -d os=el -d version=8 -u [YOUR_USERNAME]

You will be asked for your password.

While this installation is experimental, only the testing repository is available for EL8. A stable repository will be
made available once the installation has been proven to work properly in different environments.


Install Apache ActiveMQ
-----------------------

The Apache ActiveMQ message broker is required by Opencast. It can be installed on the same machine as Opencast.
ActiveMQ can be installed by running:

    dnf install activemq-dist

A prepared configuration file for ActiveMQ can be found at `/usr/share/opencast/docs/scripts/activemq/activemq.xml`
*after Opencast itself has been installed* and should replace `/etc/activemq/activemq.xml`. For an all-in-one
installation the following command should suffice:

    cp /usr/share/opencast/docs/scripts/activemq/activemq.xml /etc/activemq/activemq.xml

ActiveMQ should be started *prior to* Opencast.

More information about how to properly set up ActiveMQ for Opencast can be found in the [message broker configuration
documentation](../configuration/message-broker.md).


Install Opencast
------------------

### Basic Installation

For a basic installation (All-In-One) just run:

    dnf install opencast<version>-allinone

…where `<version>` is the major version number of the Opencast release you want to install, e.g. `opencast8-allinone`.
This will install the default distribution of Opencast and all its dependencies.

Don't forget to start configure and start ActiveMQ first as [described in the ActiveMQ installation section
](#install-apache-activemq).

Then start Opencast by running:

    systemctl start opencast.service

While Opencast is preconfigured, it is strongly recommended to follow at least the [Basic Configuration guide
](../configuration/basic.md). It will help you to set your hostname, login information, …


Advanced Installation
---------------------

The basic installation will give you an all-in-one Opencast distribution on a single server.  For productions, most
users prefer deploying Opencast over several machines, which allows for a better workload distribution.  You can list
all available Opencast packages with:

    dnf search opencast

This will list all available Opencast distributions in the form `opencast<version>-<dist-type>`. Some commonly used
distributions are:

- `opencast<version>-allinone`
- `opencast<version>-admin`
- `opencast<version>-presentation`
- `opencast<version>-worker`


Upgrading Major Versions
------------------------

Packages will automatically upgrade to the latest minor version in a release series. They do not automatically upgrade
the latest major version. This is intentional since additional migration steps might be necessary for that. For example,
if you install `opencast7-admin` you get the latest 7.x release, not the latest 8.x release. To upgrade from one major
version to another, please consult the upgrade guide for each major version. Still, here is a short overview of the
required steps:

First, stop Opencast:

    systemctl stop opencast.service

As a reminder, these instructions will upgrade your Opencast installation to a new version which is likely incompatible
with older versions, and cannot be rolled back. If you are performing this on a production system, please ensure you
have valid backups prior to taking the next steps.

Uninstall your current Opencast package:

    dnf remove opencast

Then install the new version:

    dnf install opencast<version>-<distribution>

At this point you must follow the relevant [upgrade instructions](../upgrade.md), prior to starting Opencast again.


Uninstall Opencast
--------------------

To uninstall Opencast, you can run:

    dnf remove opencast

This will not touch your created media files or *modified* configuration files.  If you want to remove them as well, you
have to do that by yourself.

    # Remove media files (default location)
    sudo rm -rf /srv/opencast

    # Remove local db, search indexes and working files
    sudo rm -rf /var/lib/opencast

    # Remove configuration files
    sudo rm -rf /etc/opencast

    # Remove logs
    sudo rm -rf /var/log/opencast
