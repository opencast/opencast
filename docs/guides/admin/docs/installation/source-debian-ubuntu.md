Install from Source (Debian, Ubuntu)
====================================

These instructions outline how to install an all in one Opencast system on Ubuntu 12.04 with LTS.

Preparation
-----------

Create a dedicated Opencast user.

    useradd -d /opt/matterhorn matterhorn

Get Opencast source:

You can get the Opencast source code by either downloading a tarball of the source code or by cloning the Git
repository. The latter option is more flexible, it is easier to upgrade and in general preferred for developers. The
prior option, the tarball download, needs less tools and you do not have to download nearly as much as with Git.

Using the tarball:

Select the tarball for the version you want to install from
https://bitbucket.org/opencast-community/matterhorn/downloads#tag-downloads

    # Download desired tarball
    curl -O https://bitbucket.org/opencast-community/matterhorn/...
    tar xf develop.tar.gz
    mv opencast-community-matterhorn-* /opt/matterhorn/

Cloning the Git repository:

    git clone https://bitbucket.org/opencast-community/matterhorn.git
    cd opencast
    git tag   <-  List all available versions
    git checkout TAG   <-  Switch to desired version


Install Dependencies
--------------------

Please make sure to install the following dependencies. Note that not all dependencies are in the system repositories.

Required:

    openjdk-7-jdk or openjdk-8-jdk
    ffmpeg >= 2.5
    maven >= 3.1

> *Note that by default Ubuntu and Debian ship Libav and installing the `ffmpeg` package from the default repository
> will not get you FFmpeg. If you are unsure about where to get FFmpeg, please refer to the [FFmpeg
> website](http://ffmpeg.org).*

Required (not necessarily on the same machine):

    ActiveMQ >= 5.10 (older versions untested)

Required for text extraction (recommended):

    tesseract >= 3

Required for hunspell based text filtering (optional):

    hunspell >= 1.2.8

Required for audio normalization (optional):

    sox >= 14.4

### Dependency Download

Pre-built versions of most dependencies that are not in the repositories can be downloaded from the respective project
website:

 - [Get FFmpeg](http://ffmpeg.org/download.html)
 - [(Get Apache Maven](https://maven.apache.org/download.cgi)
 - [Get Apache ActiveMQ](http://activemq.apache.org/download.html)


Building Opencast
-----------------

Make sure everything belongs to the user `matterhorn` (you may choose a different name):

    sudo chown -R matterhorn:matterhorn /opt/matterhorn

Switch to user `matterhorn`:

    sudo su - matterhorn

Compile the source code:

    cd /opt/matterhorn
    mvn clean install -DdeployTo=/opt/matterhorn


Configure
---------

Please follow the steps of the [Basic Configuration guide](../configuration/basic.md). It will help you to set your
hostname, login information, …


Running Opencast
----------------

Install Opencast start script and man-page for installations in `/opt`:

    cd /opt/matterhorn/docs/scripts/init/opt
    sudo ./install.sh

This will install the start script along with either a SysV-Init script or a
systemd unit file.

Now you can start Opencast by running

    sudo matterhorn --interactive

Browse to [http://localhost:8080] to get to the admin interface.


Run Opencast as Service
-----------------------

Usually, you do not want to run Opencast in interactive mode but as system service to make sure Opencast is run only
once on a system and is started automatically.

SysV-Init:

    # Start Opencast
    sudo service matterhorn start
    # Autostart after reboot
    sudo chkconfig --level 345 matterhorn on

Systemd:

    # Start Opencast
    sudo systemctl start matterhorn
    # Autostart after reboot
    sudo systemctl enable matterhorn
