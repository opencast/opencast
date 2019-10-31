Install from Repository (RedHat Enterprise Linux, CentOS, Scientific Linux)
===========================================================================

There is an RPM software repository available for Red Hat-based Linux distributions provided by the University of
Osnabrück. This repository provides preconfigured Opencast installations, including all 3rd-Party-Tools. Using this
method, you do not have to compile the software by yourself.

It may also be interesting for developers as all dependencies for Opencast usage, testing and development are provided
by the RPM repository.


Availability
------------

Note that it may take some time (usually about two weeks after a new release is out) before the RPMs are available.
Watch for announcements on list or just check which versions are available in the repository.


Currently Supported
-------------------

* CentOS 7.x (x86\_64)
* Red Hat Enterprise Linux 7.x (x86\_64)
* Scientific Linux 7.x (x86\_64)

> *Other architectures like i386, i686, arm, … are not supported!*


Registration
------------

Before you can start you need to get an account for the repository. You will need the credentials that you get by mail
after the registration to successfully complete this manual. The placeholders `[your_username]` and `[your_password]`
are used in this manual wherever the credentials are needed.

[Please visit https://pkg.opencast.org](https://pkg.opencast.org)


Activate Repository
-------------------

First you have to install the necessary repositories so that your package manager can access them:

1. Add Opencast repository:

        cd /etc/yum.repos.d
        curl -O https://pkg.opencast.org/opencast.repo \
           -d os=el -d version=7 -u [YOUR_USERNAME]

    You will be asked for your password.

    It might take some time after the release of a new Opencast version before the RPMs are moved to the stable
    repository. Until then, you can use `.../opencast-testing.repo` instead to get the latest version. Note that the
    testing repository is an additional repository and still requires the stable repository to be active.

2. Add the Extra Packages for Enterprise Linux (EPEL) repository:

        yum install epel-release

    If this package is not available, please enable this repository manually. For that, follow the [instructions in the
    EPEL documentation](https://fedoraproject.org/wiki/EPEL#How_can_I_use_these_extra_packages.3F).


Install Apache ActiveMQ
-----------------------

The Apache ActiveMQ message broker is required by Opencast since version 2.0. It does not necessarily have to be
installed on the same machine as Opencast but would commonly for an all-in-one system. ActiveMQ is available from the
Opencast RPM repository as well and can be installed by running:

    yum install activemq-dist

A prepared configuration file for ActiveMQ can be found at `/usr/share/opencast/docs/scripts/activemq/activemq.xml`
*after Opencast itself has been installed* and should replace `/etc/activemq/activemq.xml`. For an all-in-one
installation the following command should suffice:

    cp /usr/share/opencast/docs/scripts/activemq/activemq.xml /etc/activemq/activemq.xml

ActiveMQ should be started *prior to* Opencast.

More information about how to properly set up ActiveMQ for Opencast can be found in the [message broker configuration
documentation](../configuration/message-broker.md).


Install Opencast
------------------

This describes a simple, single-node installation. A more complex, multi-node installation guide can be found in the
[Advanced Installation](#advanced-installation) section below and in the guide [Install Across Multiple Servers
](multiple-servers.md).

### Basic Installation

For a basic installation (All-In-One) just run:

    yum install opencast<version>-allinone

Where `<version>` is the major version number of the Opencast release you want to install.

This will install the default distribution of Opencast and all its dependencies, including 3rd-Party-Tools.

Now you can start Opencast:

* Don't forget to start configure and start ActiveMQ first as [described in the ActiveMQ installation section
  ](#install-apache-activemq).

* On a SysV-init based system

        service opencast start

* On a systemd based system

        systemctl start opencast.service

While Opencast is preconfigured, it is strongly recommended to follow at least the [Basic Configuration
guide](../configuration/basic.md). It will help you to set your hostname, login information, …


Advanced Installation
---------------------

While the basic installation will give you an all-in-one Opencast distribution which is nice for testing, you might
want to have more control over your system and deploy it over several machines by choosing which parts of Opencast you
want to install. You can list all Opencast packages with:

    yum search opencast

This will list all available Opencast distributions in the form `opencast<version>-<dist-type>`

Some available distributions are:

* opencastX-allinone
* opencastX-admin
* opencastX-presentation
* opencastX-worker

…where `X` stands for a specific Opencast version.


Install 3rd-party-tools
-----------------------

This step is optional and only recommended for those who want to build Opencast from source. If you install Opencast
from the repository, all necessary dependencies will be installed automatically.

You can install all necessary 3rd-Party-Tools for Opencast like this:

    yum install ffmpeg tesseract hunspell sox synfig nmap-ncat


Upgrading Major Versions
------------------------

While these packages will automatically upgrade you to the latest point version in a release series, they do not
automatically upgrade you to the latest major version. In other words, if you install `opencast3-admin` you get the
latest 3.x release, not the latest 4.x release. To upgrade from one version to another you first stop Opencast:

* On a SysV-init based system

        service opencast stop

* On a systemd based system

        systemctl stop opencast.service

As a reminder, these instructions will change your Opencast installation, and files to a new version which is likely
incompatible with older versions. If you are performing this on a production system, please ensure you have valid
backups prior to taking the next steps.

Uninstall your current Opencast packaging (using Opencast 3 as an example):

    yum remove opencast3-*

Then install the new version (using Opencast 4 as an example):

    yum install opencast4-allinone

At this point you must follow the relevant [upgrade](../upgrade.md) instructions, prior to starting Opencast again.

Uninstall Opencast
--------------------

To uninstall Opencast, you can run:

    yum remove 'opencast*'

This will not touch your created media files or modified configuration files.  If you want to remove them as well, you
have to do that by yourself.

    # Remove media files
    sudo rm -rf /srv/opencast

    # Remove local db, search indexes and working files
    sudo rm -rf /var/lib/opencast

    # Remove configuration files
    sudo rm -rf /etc/opencast

    # Remove logs
    sudo rm -rf /var/log/opencast


Troubleshooting
---------------

### Missing Dependencies

If you try to install Opencast but yum is complaining about missing dependencies, please check if the epel repository is
really activated on your system. Some distributions come with epel preinstalled but disabled. The installation of the
epel-release package will not fix this. You can check what repositories are installed and enabled by executing `yum
repolist enabled` which should give you a list with epel, opencast and opencast-noarch in it. To enable a repository,
edit the configuration file in `/etc/yum.repos.d/`.
