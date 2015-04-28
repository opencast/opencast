Install from Repository (Fedora)
================================

There is a RPM software repository available for RedHat based Linux distributions provided by the University of
Osnabrück. This repository provides preconfigured Matterhorn installations, including all 3rd-Party-Tools. Using this
method, you do not have to compile the software by yourself.

It is also interesting for developers as all dependencies for Matterhorn use, testing and development are provided by
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

First you have to install the necessary repositories so that you package manager can access them:

1. Add matterhorn repository:

    ```
    cd /etc/yum.repos.d
    curl -O http://repo.virtuos.uos.de/matterhorn-testing.repo \
      -d 'version=$releasever' -d os=fc \
      -u [your_username]:[your_password]
    ```

2. Add RPMfusion repository:

    ```
    yum localinstall --nogpgcheck \
      http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm \
      http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonf
    ```

Install 3rd-party-tools
-----------------------

This step is optional and only recommended for those who want to build
Matterhorn from source. If you install Matterhorn from the repository, all
necessary dependencies will be installed automatically.

You can install all necessary 3rd-Party-Tools for matterhorn like this:

```
sudo yum install opencast-matterhorn16-third-party-tools
```

Or manually:

```
sudo yum install ffmpeg qt_sbtl_embedder tesseract mediainfo
```


Install Apache ActiveMQ
-----------------------

The Apache ActiveMQ message broker is required by Matterhorn since version 2.0. It does not necessary have to be
installed on the same machine as Matterhorn but would commonly for an all-in-one system. ActiveMQ is available from the
Matterhorn RPM repository as well and can be installed by running:

    yum install activemq-dist

A prepared configuration file for ActiveMQ can be found at `docs/scripts/activemq/settings.xml` and should replace
`/etc/activemq/settings.xml`. ActiveMQ should be started before starting Matterhorn.


Install Matterhorn
------------------

For this guide `opencast-matterhorn16` is used as placeholder for the package name. It will the latest version of the
Matterhorn 1.6.x branch. If you want to install another version, please change the name accordingly. To install the
latest version from the 1.5.x branch you would for example install `opencast-matterhorn15`.


### Basic Installation

For a basic installation (All-In-One) just run:

    ```
    sudo yum install opencast-matterhorn16
    ```

This will install the default distribution of matterhorn and all its dependencies, including the 3rd-Party-Tools.

Now you can start Matterhorn:

```
sudo systemctl start matterhorn.service
```

While Matterhorn is preconfigured, it is strongly recommended to follow at last the Basic Configuration guide. It will
help you to set your hostname, login information, …


Advanced Installation
---------------------

While the basic installation will give you an all-in-one matterhorn distribution which is nice for testing, you might
want to have more control over your system and deploy it over several machines by choosing which parts of Matterhorn you
want to install. You can list all Matterhorn packages with:

```
yum search opencast-matterhorn
```

This will list four kinds of packages:

`opencast-matterhornXX` is the package that was used for the basic installation. It is a default Matterhorn
distribution.  This is what you would get, if you built Matterhorn from source and do not change any options.

The `opencast-matterhornXX-distribution-...` packages will install preconfigured Matterhorn distribution. Have a look at
the Matterhorn Distribution section below for more information about the different distributions.

`opencast-matterhorn14-profile-...` are the Matterhorn profiles from the main pom.xml. Each profile keeps track of a
couple of modules.  You should only install these if you know what you are doing.

`opencast-matterhorn14-module-...` are the Matterhorn modules itself. It should only be necessary to install these
directly in special cases.  And you should only do that if you know what you are doing.

Normally you would either install the main package or a distribution package.


Prebuild Matterhorn Distributions
---------------------------------

The following list provides an overview of the currently available pre-build Matterhorn distributions. Each distribution
should keep track of all its dependencies.

### Admin Matterhorn distribution

`opencast-matterhorn14-distribution-admin`

Install this package for a Matterhorn admin server. On this server the Administrative services are hosted etc. You
usually have 3+ servers on which you run matterhorn if you select this package.

### Admin/Worker Matterhorn distribution

`opencast-matterhorn14-distribution-admin-worker`

Combined Admin/Worker Matterhorn distribution. This will install both the modules and profiles for the Administrative
Tools and the Worker. This package is targeted at medium sized installations, where you want to seperate the "backend"
server that the admin accesses from the "frontend" server that the viewers use.

### Capture-Agent Matterhorn distribution

`opencast-matterhorn14-distribution-capture-agent`

This is a package installing the Matterhorn reference Capture Agent with remote service registry.

### Default Matterhorn distribution

`opencast-matterhorn14-distribution-default`

This is the default package containing all 3 main profiles (Admin, Worker, Engage) in one. This installation is only
recommended if you dont have many videos that you want to ingest and you do not expect many many viewers. This perfect
for first test and to get an impression of Matterhorn as it works out of the box and does not need much configuration.

### Engage Matterhorn distribution

`opencast-matterhorn14-distribution-engage`

This is the package for the Matterhorn Engage Modules which are the front-end to the viewer of your videos. It is always
highly recommended to keep these separated from the rest of your system.

### Worker Matterhorn distribution

`opencast-matterhorn14-distribution-worker`

This is the worker package that contains the modules that create the most CPU load (encoding, OCR, etc). So it is
recommended to deploy this on a more
powerful machine.


Uninstall Matterhorn
--------------------

Sometimes you want to uninstall Matterhorn. For example to do a clean reinstall. You can do that by executing:

```
sudo yum remove 'opencast-matterhorn*'
```

This will not touch your created media files or modified configuration files.  If you want to remove them as well, you
have to to that by yourself.

```
# Remove media files
sudo rm -rf /srv/matterhorn
 
# Remove configuration files
sudo rm -rf /etc/matterhorn

# Remove system logfiles
sudo rm -rf /var/log/matterhorn
```
