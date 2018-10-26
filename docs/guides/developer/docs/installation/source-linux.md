Developer installation guide
===========================

These instructions outline how to install Opencast on a Linux system.


1.Get Opencast source:
--------------------

You can get the Opencast source code  by cloning the Git
repository.

Cloning the Git repository:

    git clone https://github.com/opencast/opencast.git
    cd opencast
    git checkout develop

2.Install Dependencies
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

Required as a service for running Opencast:

    ActiveMQ >= 5.10 (older versions untested)

Required for some services. Some tests may be skipped and some features
may not be usable if they are not installed. Hence, it's generally a good idea to
install them.

    tesseract >= 3
    hunspell >= 1.2.8
    sox >= 14.4
    synfig

### Dependency Download

Pre-built versions of most dependencies that are not in the repositories can be downloaded from the respective project
website:

* [Get FFmpeg](http://ffmpeg.org/download.html)
* [Get Apache Maven](https://maven.apache.org/download.cgi)
* [Get Apache ActiveMQ](http://activemq.apache.org/download.html)

3.ActiveMQ Configuration
--------------------

Opencast comes with a basic configuration for Activemq. Please follow the first
configuration step to copy the XML file. [Message Broker Configuration](https://docs.opencast.org/develop/admin/configuration/message-broker/).

4.Build and start Opencast
--------------------

Automatically build Opencast and how to start it.
The -Pdev argument decreases the build time and skips the creation of multiple
distribution tarballs by summarizing them into one.

    cd opencast-dir
    mvn clean install -Pdev
    cd build/opencast-*
    ./bin/start-opencast

### Useful Commands for Building Opencast

If you need a quick build of Opencast for test purposes, you can also use the
following command to skip the Opencast building tests.

    cd opencast-dir
    mvn clean install -DskipTests=true

To see the whole stacktrace of the installation you can use the following command
to disable the trimming.

    cd opencast-dir
    mvn clean install -DtrimStackTrace=false

5.Modify code and build changes
--------------------
After you modified your code you can go back to step 4 to rebuild Opencast.
