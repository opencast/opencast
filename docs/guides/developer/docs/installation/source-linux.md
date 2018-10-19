Developer installation guide
===========================

These instructions outline how to install an all in one Opencast system on Linux.


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

Please make sure to install the following dependencies.

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
    
Useful Commands
--------
If you need a quick build of Opencast for test purposes, you can also use the following command to skip the Opencast building tests.

    cd opencast-dir
    mvn clean install -DskipTests=true

To see the whole Stacktrace of the installation you can use the following command to disable the trimming.

    cd opencast-dir
    mvn clean install -DtrimStackTrace=false
    
In addition, you can use the -Pdev argument to increase the build time and skip the creation of multiple packages by summarizing them into one.

Deploy all-in-one distribution:
--------

    cd build/
    mv opencast-dist-allinone-*/ /opt/opencast

ActiveMQ Configuration
---------

Opencast comes with a basic configuration for Activemq. Please follow the first Step to copy the XML file.[Message Broker Configuration](../configuration/message-broker.md).


Running Opencast
------------------

To start Opencast, run .../bin/start-opencast

    /../opencast/bin/start-opencast

As soon as Opencast is completely started, browse to [http://localhost:8080](http://localhost:8080) to get to the
administration interface.


