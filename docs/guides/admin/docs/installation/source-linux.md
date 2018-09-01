Install from Source (Linux)
===========================

These instructions outline how to install an all in one Opencast system on Linux.

Preparation
-----------

Create a dedicated Opencast system user:

    useradd -r -d /opt/opencast opencast

Get Opencast source:

You can get the Opencast source code by either downloading a tarball of the source code or by cloning the Git
repository. The latter option is more flexible, it is easier to upgrade and in general preferred for developers. The
prior option, the tarball download, needs less tools and you do not have to download nearly as much as with Git.

Using the tarball:

Select the tarball for the version you want to install
from the [GitHub releases section](https://github.com/opencast/opencast/releases).

    # Download desired tarball
    curl -OL https://github.com/opencast/opencast/archive/[...].tar.gz
    tar xf [...].tar.gz
    cd opencast--[...]

Cloning the Git repository:

    git clone https://github.com/opencast/opencast.git
    cd opencast
    git tag   <-  List all available versions
    git checkout TAG   <-  Switch to desired version


Install Dependencies
--------------------

Please make sure to install the following dependencies. Note that not all dependencies are in the system repositories.

Required:

    java-1.8.0-openjdk-devel.x86_64 / openjdk-8-jdk
    ffmpeg >= 3.2.4
    maven >= 3.1
    unzip
    gcc-c++
    tar
    bzip2

Required (not necessarily on the same machine):

    ActiveMQ >= 5.10 (older versions untested)

Required for text extraction (recommended):

    tesseract >= 3

Required for hunspell based text filtering (optional):

    hunspell >= 1.2.8

Required for audio normalization (optional):

    sox >= 14.4

Required for animate service (optional):

    synfig

### Dependency Download

Pre-built versions of most dependencies that are not in the repositories can be downloaded from the respective project
website:

* [Get FFmpeg](http://ffmpeg.org/download.html)
* [Get Apache Maven](https://maven.apache.org/download.cgi)
* [Get Apache ActiveMQ](http://activemq.apache.org/download.html)


Building Opencast
-----------------

Automatically build all Opencast modules and assemble distributions for different server types:

    cd opencast-dir
    mvn clean install

Deploy all-in-one distribution:

    cd build/
    mv opencast-dist-allinone-*/ /opt/opencast

Make sure everything belongs to the user `opencast`:

    sudo chown -R opencast:opencast /opt/opencast


Configure
---------

Please follow the steps of the [Basic Configuration guide](../configuration/basic.md). It will help you to set your
hostname, login information, …


Running Opencast
------------------

To start Opencast, run `.../bin/start-opencast` as user `opencast`:

    sudo -u opencast /opt/opencast/bin/start-opencast

As soon as Opencast is completely started, browse to [http://localhost:8080](http://localhost:8080) to get to the
administration interface.


Run Opencast as a service
-------------------------

Usually, you do not want to run Opencast in interactive mode but as system service to make sure it is only running
once on a system and is started automatically.

You will find service files for Opencast in `docs/scripts/service/{opt,system}/`.

### Using Systemd

Make sure the path to Opencast is set correctly:

    vim docs/scripts/service/opencast.service

Install the unit file:

    cp docs/scripts/service/opencast.service /etc/systemd/system/
    systemctl daemon-reload

Start Opencast and make it run automatically:

    systemctl start opencast.service
    systemctl enable opencast.service

### Using SysV-Init

> Note that this option is for compatibility to older systems. If you have the choice of either using the Systemd unit
> file or the Init script, it is recommended to use the Systemd unit file.

Make sure the path to Opencast is set correctly:

    vim docs/scripts/service/etc-init.d-opencast

1. Install init script:

        cp docs/scripts/service/etc-init.d-opencast /etc/init.d/opencast

2. Enable service using `chkconfig` or `update-rc.d`

3. Start Opencast using

        service opencast start
