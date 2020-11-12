Install from Repository (Debian, Ubuntu)
===========================================================================

There is a Debian software repository (DEB) available for Debian-based Linux distributions provided by Greg Logan, and
hosted at University of Osnabrück. This repository provides prebuilt Opencast installations, including all
3rd-Party-Tools. Using this method, you do not have to compile the software by yourself, but you still need to configure
it.

It may also be interesting for developers as all dependencies for Opencast usage, testing and development are provided
by the Debian repository.


Availability
------------

Note that it may take some time (usually about two weeks after a new release is out) before the DEBs are available.
Watch for announcements on list or just check which versions are available in the repository.


Currently Supported
-------------------

* Debian 9 and newer amd64
* Ubuntu 18.04 amd64


Java version support
--------------------

The only supported Java version for Opencast 8 is JDK 8.  Newer versions will not work with Opencast 8, nor will older versions.


OpenJDK 8 on Debian 10
----------------------

**Debian 10 and newer requires a manual OpenJDK install**

* Add Debian unstable to your sources, replacing the mirror URL with your local mirror:

        echo "deb http://[YOUR_MIRROR]/debian unstable main" | sudo tee /etc/apt/sources.list.d/debian-unstable.list

* Ensure you don't accidentally upgrade your entire Debian install to unstable

        echo "APT::Default-Release \"`dpkg --status tzdata|grep Provides|cut -f2 -d'-'`\";" | sudo tee /etc/apt/apt.conf.d/99defaultrelease

* Update your package listing

        apt-get update

* Install OpenJDK 8 from the backports

        apt-get install -t unstable openjdk-8-jre

> *Other architectures like i386, i686, arm, … are not supported!*


Activate Repository
-------------------

First you have to install the necessary repositories so that your package manager can access them:

* Ensure https repositories are supported:

        apt-get install apt-transport-https ca-certificates sudo wget gnupg2

* Add Opencast repository:

        cd /etc/apt/sources.list.d/
        echo "deb https://pkg.opencast.org/debian stable/" | sudo tee opencast.list

    It might take some time after the release of a new Opencast version before the Debs are moved to the stable
    repository. If you need the new release prior to its promotion to stable you can use the testing repository.
    Note that the testing repository is an additional repository and still requires the stable repository to be active.

        cd /etc/apt/sources.list.d/
        echo "deb https://pkg.opencast.org/debian testing/" | sudo tee opencast.list

* Add the repository key to your apt keyring:

        wget -qO - https://pkg.opencast.org/gpgkeys/opencast-deb.key | sudo apt-key add -

* Update your package listing

        apt-get update


Install Apache ActiveMQ
-----------------------

The Apache ActiveMQ message broker is required by Opencast since version 2.0. It does not necessarily have to be
installed on the same machine as Opencast but would commonly for an all-in-one system. ActiveMQ is available from the
the normal software repositories for your distribution:

    apt-get install activemq-dist


A prepared configuration file for ActiveMQ can be found at `/usr/share/opencast/docs/scripts/activemq/activemq.xml`
*after Opencast itself has been installed* and should replace `/etc/activemq/activemq.xml`. For an all-in-one
installation the following command should suffice:

    cp /usr/share/opencast/docs/scripts/activemq/activemq.xml /etc/activemq/activemq.xml

ActiveMQ must be started *prior to* Opencast startup.

More information about how to properly set up ActiveMQ for Opencast can be found in the [message broker configuration
documentation](../configuration/message-broker.md).

Note that most Debian based distributions also have activemq packaged by upstream package maintainers. These packages
will work, however the ActiveMQ configuration file will require modification to function correctly.

Install Opencast
------------------

For this guide we will be installing the latest released version of Opencast 3.x, however if you wish to install another
version please change the name accordingly.


### Basic Installation

For a basic installation (All-In-One) just run:

    apt-get install opencast-3-allinone

This will install the default distribution of Opencast and all its dependencies, including the 3rd-Party-Tools.  Note
that while the repository provides a packaged version of ffmpeg, your distribution may have a version which is
pre-installed or otherwise takes precedence.  This version may work, however Opencast only formally supports the
version(s) in the repository.

At this point Opencast is installed and will work locally, but it is not completely configured.  Please follow the
[Basic Configuration guide](../configuration/basic.md) from here.  Once you are ready, start Opencast:

* On a SysV-init based system

        service opencast start

* On a Systemd based system

        systemctl start opencast.service


Advanced Installation
---------------------

While the basic installation will give you an all-in-one Opencast distribution which is nice for testing, you might
want to have more control over your system and deploy it over several machines by choosing which parts of Opencast you
want to install. You can list all Opencast packages with:

    apt-cache search opencast

This will list all available Opencast distributions in the form `opencast-<version>-<dist-type>`

Some available distributions are:

* opencast-X-allinone
* opencast-X-admin
* opencast-X-presentation
* opencast-X-worker

…where `X` stands for a specific Opencast version. These packages will install the latest release for a given version,
so opencast-3-admin will install the admin profile for Opencast 3.x (currently 3.3). Once Opencast 3.4 has been packaged
and made available your system will automatically update to Opencast 3.4 using the standard `apt-get` tools.

To list all available packages and versions, use:

    apt list 'opencast*'


Point Revisions (Experts only)
------------------------------

If for some reason you wish to install a specific point revision of Opencast, and the repository still hosts that point
revision, you can select it by adding it, and the packaging build, to your `apt-get install` line.  For example:

    apt-get install opencast-3-admin=3.2-2

Installs an Opencast 3.2 admin node, using the second build of that series.  Not all series have more than a single build,
and older point revisions may be removed once superceded, so please explore the repository prior to attempting this.


Install 3rd-party-tools
-----------------------

This step is optional and only recommended for those who want to build Opencast from source. If you install Opencast
from the repository, all necessary dependencies will be installed automatically.

You can install all necessary 3rd-Party-Tools for Opencast like this:

    apt-get install ffmpeg-dist tesseract-ocr sox hunspell synfig netcat


Upgrading Major Versions
------------------------

While these packages will automatically upgrade you to the latest point version in a release series, they do not
automatically upgrade you to the latest major version. In other words, if you install `opencast3-admin` you get the
latest 3.x release, not the latest 4.x release. To upgrade from one version to another you first stop Opencast:

* On a SysV-init based system

        service opencast stop

* On a Systemd based system

        systemctl stop opencast.service

As a reminder, these instructions will change your Opencast installation, and files to a new version which is likely
incompatible with older versions. If you are performing this on a production system, please ensure you have valid
backups prior to taking the next steps.

Uninstall your current Opencast packaging (using Opencast 3 as an example):

    apt-get remove opencast-3-*

Then install the new version (using Opencast 4 as an example):

    apt-get install opencast-4-allinone

At this point you must follow the relevant [upgrade](../upgrade.md) instructions, prior to starting Opencast again.


Uninstall Opencast
--------------------

To uninstall Opencast, you can run:

    apt-get remove 'opencast*'

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

This repository expects that the `stable` section is always available, regardless of which version of Opencast you have
installed.  The 3rd party tools (ActiveMQ, FFmpeg) may or may not be in the other sections, but if they are there it is
only during a testing period for a new version.  For day-to-day use, please install them from `stable`!
