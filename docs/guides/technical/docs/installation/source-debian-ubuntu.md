Install from Source (Debian, Ubuntu)
====================================

These instructions outline how to install an all in one Matterhorn system on Ubuntu 12.04 with LTS.

Preparatiom
-----------

Create a dedicated Matterhorn user.

    useradd -d /opt/matterhorn matterhorn

Get Matterhorn source:

You can get the Matterhorn source code by either downloading a tarball of the source code or by cloning the Git
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
    cd matterhorn
    git tag   <-  List all available versions
    git checkout TAG   <-  Switch to desired version


Install Dependencies
--------------------

Please make sure to install the following dependencies:

Required:

    openjdk-7-jdk or openjdk-8-jdk
    ffmpeg >= 1.1
    maven >= 3

Required for text extraction (recommended):

    tesseract >= 3

Required for the video editor (recommended):

    gnonlin0.10
    gstreamer
    gstreamer-ffmpeg
    gstreamer0.10-plugins-base
    gstreamer0.10-plugins-good
    gstreamer0.10-plugins-ugly
    gstreamer0.10-plugins-bad
    gstreamer0.10-gnonlin

*Note: Make sure to install Gstreamer 0.10*

Required for hunspell based text filtering (optional):

    hunspell >= 1.2.8

Required for audio normalization (optional):

    sox >= 14


Configure
---------

Please follow the steps of the [Basic Configuration guide](../configuration/basic.md). It will help you to set your
hostname, login information, …


Building Matterhorn
-------------------

Make sure everything belongs to the user `matterhorn`:

    sudo chown -R matterhorn:matterhorn /opt/matterhorn

Switch to user `matterhorn`:

    sudo su - matterhorn

Compile the source code:

    cd /opt/matterhorn
    mvn clean install -DdeployTo=/opt/matterhorn


Running Matterhorn
------------------

Install Matterhorn start script and man-page for installations in /opt:

    cd /opt/matterhorn/docs/scripts/init/opt
    sudo ./install.sh

This will install the start script along with either a SysV-Init script or a
systemd unit file.

Now you can start Matterhorn by running

    sudo matterhorn --interactive

Browse to [http://localhost:8080] to get to the admin interface.


Run Matterhorn as Service
-------------------------

Usually, you do not want to run Matterhorn in interactive mode but as system
service to make sure matterhorn is run only once on a system and is started
automatically.

SysV-Init:

    # Start Matterhorn
    sudo service matterhorn start
    # Autostart after reboot
    sudo chkconfig --level 345 matterhorn on

Systemd:

    # Start Matterhorn
    sudo systemctl start matterhorn
    # Autostart after reboot
    sudo systemctl enable matterhorn
