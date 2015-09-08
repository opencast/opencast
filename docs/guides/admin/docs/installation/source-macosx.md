Install from Source (Mac OS X)
====================================

This document will help you install and run Matterhorn on the Mac OS X operating system.
Tested on OS X 10.9 Mavericks.

> *The installation on Mac OS X is not officially supported. Use this at your own risk.*


Preparation
-----------

### Create Matterhorn installation directory

    sudo mkdir -p /opt/matterhorn
    sudo chown $USER:$GROUPS /opt/matterhorn

### Check out Matterhorn source

You can get the Matterhorn source code by either downloading a tarball of the source code or by cloning the Git repository. The latter option is more flexible, it is easier to upgrade and in general preferred for developers. The prior option, the tarball download, needs less tools and you don't have to download nearly as much as with Git.

Using the tarball:

Select the tarball for the version you want to install from
https://bitbucket.org/opencast-community/matterhorn/downloads#tag-downloads

    # Download desired tarball
    curl -O https://bitbucket.org/opencast-community/matterhorn/...
    tar xf develop.tar.gz
    mv opencast-community-matterhorn-* /opt/matterhorn/

Cloning the Git repository:
    
    cd /opt
    git clone https://bitbucket.org/opencast-community/matterhorn.git
    cd opencast
    git checkout r/2.0.x


Install Dependencies
--------------------

Please make sure to install the following dependencies.

Required:
    
    Xcode
    jdk 7 or jdk 8
    ffmpeg >= 2.5
    maven >= 3.1

Required (not necessarily on the same machine):

    ActiveMQ >= 5.10 (older versions untested)

Required for text extraction (recommended):

    tesseract >= 3

Required for hunspell based text filtering (optional):

    hunspell >= 1.2.8

Required for audio normalization (optional):

    sox >= 14.4

### Dependency Download

You can download Xcode in the Mac App Store. JDK 8 for OS X is available from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

#### Using Homebrew

Homebrew is a package manager for OS X. For installation instruction see [their website](http://brew.sh/).

    brew install maven
    brew install ffmpeg
    brew install apache-activemq
    # Optional
    brew install tesseract
    brew install hunspell
    brew install sox

#### Using pre-built binaries

Pre-built versions of most dependencies can be downloaded from the respective project website:

 - [Get Apache Maven](https://maven.apache.org/download.cgi)
 - [Get FFmpeg](http://ffmpeg.org/download.html)
 - [Get Apache ActiveMQ](http://activemq.apache.org/download.html)


Building Opencast
-----------------

Configure environment

    export MAVEN_OPTS="-Xms256m -Xmx960m -XX:PermSize=64m -XX:MaxPermSize=256m" >> ~/.bash_profile
    echo "export DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib" >> ~/.bash_profile
    source ~/.bash_profile

Compile the source code:

    cd /opt/matterhorn
    mvn clean install -DdeployTo=/opt/matterhorn

> *Please be patient, as building matterhorn for the first time will take quite long.*

Configure
---------

Please follow the steps of the [Basic Configuration guide](../configuration/basic.md). It will help you to set your host name, login information, etc.
As specified in the guide, make sure you replace the default ActiveMQ configuration with the one provided in `docs/scripts/activemq/activemq.xml`. If you installed ActiveMQ using homebrew, you can find the installation path with `brew info activemq`.

Running Opencast
----------------

Make sure you have ActiveMQ running (unless you're running it on a different machine). Then you can start Opencast using the start-matterhorn script.

    activemq start
    cd /opt/matterhorn
    sudo ./bin/start-matterhorn
