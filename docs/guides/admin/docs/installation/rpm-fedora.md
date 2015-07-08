Install from Repository (Fedora)
================================

There is an RPM software repository available for RedHat-based Linux distributions provided by the University of
Osnabrück. This repository provides preconfigured Opencast installations, including all 3rd-Party-Tools. Using this
method, you do not have to compile the software by yourself.

It is also interesting for developers as all dependencies for Opencast usage, testing and development are provided by
the RPM repository.


Supported Versions
------------------

For Fedora usually the latest two versions are supported, meaning that the support is dependent on the status of the
Fedora release. For architectures, *only* `x86_64` is supported. 32bit architectures are *not* supported.


Registration
------------

Before you can start you need to get an account for the repository. You will need the credentials that you get by mail
after the registration to successfully complete this manual. The placeholders `[your_username]` and `[your_password]`
are used in this manual wherever the credentials are needed.

 - http://repo.virtuos.uos.de


Activate Repository
-------------------

First you have to install the necessary repositories so that your package manager can access them:

1. Add matterhorn repository:

    ```
    cd /etc/yum.repos.d
    curl -O http://repo.virtuos.uos.de/matterhorn-testing.repo \
      -d 'version=$releasever' -d os=fc \
      -u [your_username]:[your_password]
    ```

2. Add RPMfusion repository:

    dnf install --nogpgcheck \
      http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm \
      http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonf

Install 3rd-party-tools
-----------------------

This step is optional and only recommended for those who want to build Opencast from source. If you install Opencast
from the repository, all necessary dependencies will be installed automatically.

You can install all necessary 3rd-Party-Tools for matterhorn like this:

    sudo dnf install opencast20-third-party-tools

Or manually:

    sudo dnf install ffmpeg qt_sbtl_embedder tesseract mediainfo


Install Apache ActiveMQ
-----------------------

The Apache ActiveMQ message broker is required by Opencast since version 2.0. It does not necessarily have to be
installed on the same machine as Opencast but would commonly for an all-in-one system. ActiveMQ is available from the
Opencast RPM repository as well and can be installed by running:

    dnf install activemq-dist

A prepared configuration file for ActiveMQ can be found at `/usr/share/matterhorn/docs/scripts/activemq/settings.xml`
*after Opencast itself has been installed* and should replace `/etc/activemq/settings.xml`. For an all-in-one
installation the following command should suffice:

    sudo cp /usr/share/matterhorn/docs/scripts/activemq/settings.xml /etc/activemq/settings.xml

ActiveMQ should be started before starting Opencast.

More information about how to properly set up ActiveMQ for Opencast can be found in the [message broker configuration
documentation](../configuration/message-broker.md).


Install Opencast
------------------

For this guide, `opencast20` is used as placeholder for the package name. It will install the latest version of the
Opencast 2.0.x branch. If you want to install another version, please change the name accordingly.

> *Notice: Since the name `matterhorn` was dropped between version 1.6 and 2.0, old packages were named
> `opencast-matterhornXX`.*


### Basic Installation

For a basic installation (All-In-One) just run:

    sudo dnf install opencast20

This will install the default distribution of matterhorn and all its dependencies, including the 3rd-Party-Tools.

Now you can start Opencast:

    sudo systemctl start matterhorn.service

While Opencast is preconfigured, it is strongly recommended to follow at least the [Basic Configuration
guide](../configuration/basic.md). It will help you to set your hostname, login information, …


Advanced Installation
---------------------

While the basic installation will give you an all-in-one Opencast distribution which is nice for testing, you might
want to have more control over your system and deploy it over several machines by choosing which parts of Opencast you
want to install. You can list all Opencast packages with:

    dnf search opencast

This will list four kinds of packages:

`opencastXX` is the package that was used for the basic installation. It represents a default Opencast
distribution.  This is what you would get if you built Opencast from source and do not change any options.

The `opencastXX-distribution-...` packages will install preconfigured Opencast distributions. Have a look at
the Opencast Distribution section below for more information about the different distributions.

`opencastXX-profile-...` are the Opencast profiles from the main pom.xml. Each profile keeps track of a
couple of modules.  You should only install these if you know what you are doing.

`opencastXX-module-...` are the Opencast modules itself. It should only be necessary to install these
directly in special cases.  And you should only do that if you know what you are doing.

Normally you would either install the main package or a distribution package.


Pre-built Opencast Distributions
--------------------------------

The following list provides an overview of the currently available pre-built Opencast distributions. Each distribution
should keep track of all its dependencies.

### Admin Opencast distribution

`opencastXX-distribution-admin`

Install this package for an Opencast admin server. On this server, the Administrative services are hosted. You would usually
select this package for one node if you are running Opencast across three or more servers.

### Admin/Worker Opencast distribution

`opencastXX-distribution-admin-worker`

Combined Admin/Worker Opencast distribution. This will install both the modules and profiles for the Administrative
Tools and the Worker. This package is targeted at medium-sized installations, where you want to separate the "backend"
server that the admin accesses from the "frontend" server that the viewers use.

### Default Opencast distribution

`opencastXX-distribution-default`

This is the default package containing all 3 main profiles (Admin, Worker, Engage) in one. This installation is only
recommended if you do not have many videos that you want to ingest and you do not expect many viewers. This is perfect
for a first test and to get an impression of Opencast, as it works out of the box and does not need much configuration.

### Engage Opencast distribution

`opencastXX-distribution-engage`

This is the package for the Opencast Engage Modules, which are the front-end to the viewer of your videos. It is always
highly recommended to keep these separated from the rest of your system.

### Worker Opencast distribution

`opencastXX-distribution-worker`

This is the worker package that contains the modules that create the most CPU load (encoding, OCR, etc). So it is
recommended to deploy this on a more powerful machine.


Uninstall Opencast
--------------------

Sometimes you want to uninstall Opencast. For example to do a clean reinstall. You can do that by executing:

    sudo dnf remove 'opencast*'

This will not touch your created media files or modified configuration files.  If you want to remove them as well, you
have to to that by yourself.

    # Remove media files
    sudo rm -rf /srv/matterhorn
 
    # Remove configuration files
    sudo rm -rf /etc/matterhorn

    # Remove system logfiles
    sudo rm -rf /var/log/matterhorn
