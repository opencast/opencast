Install from Source (macOS)
====================================

These instructions outline how to install an all in one Opencast system on the macOS operating system.
Tested on OS X 10.14.1 Mojave.

<div class=warn>
The installation on macOS is not officially supported.
Use this at your own risk.
</div>


Preparation
-----------

Open a Terminal and switch to the directory, in which the Opencast installation should be placed, e.g. `/opt/`,
`~/develop/` or whatever you prefer.

### Get Opencast source

You can get the Opencast source code by either downloading a tarball of the source code or by cloning the Git
repository. The latter option is more flexible, it is easier to upgrade and in general preferred for developers. The
prior option, the tarball download, needs less tools and you don't have to download nearly as much as with Git.

Cloning the Git repository:

    git clone https://github.com/opencast/opencast.git
    cd opencast
    git tag   <-  List all available versions
    git checkout TAG   <-  Switch to desired version

Using the tarball:

Select the tarball for the version you want to install
from the [GitHub releases section](https://github.com/opencast/opencast/releases) under the "Tags" tab and download it
directly from there or with the curl command specified below.

    # Download desired tarball, replace [...] with the desired version
    curl -OL https://github.com/opencast/opencast/archive/[...].tar.gz
    tar xf [...].tar.gz


Install Dependencies
--------------------

Please make sure to install the following dependencies.

Required:

    Xcode
    jdk 11
    ffmpeg >= 3.2.4
    maven >= 3.6
    python >= 2.6

(If you are using [jEnv](http://www.jenv.be/) to set up your environment, make sure to [enable the maven plugin
](https://stackoverflow.com/a/37466252).)

Required (not necessarily on the same machine):

    ActiveMQ >= 5.10 (older versions untested)
    Elasticsearch 7.9.x

Required for text extraction:

    tesseract >= 3

Required for hunspell based text filtering:

    hunspell >= 1.2.8

Required for audio normalization:

    sox >= 14.4 (with MP3, FLAC and OGG support)

Required for animate service:

    synfig

### Dependency Download

You can download Xcode in the Mac App Store. JDK 8 for OS X is available from
[Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

#### Using Homebrew

Homebrew is a package manager for OS X. For installation instruction see [their website](http://brew.sh/).

    brew install maven
    brew install ffmpeg
    brew install apache-activemq

    brew install tesseract
    brew install hunspell
    brew install sox
    brew install synfig

#### Elasticsearch on macOS

If you want to install Elasticsearch in the same machine run Elasticsearch as a Docker container

    docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e 'discovery.type=single-node' elasticsearch:7.9.3
#### Using pre-built binaries

Pre-built versions of most dependencies can be downloaded from the respective project website:

* [Get Apache Maven](https://maven.apache.org/download.cgi)
* [Get FFmpeg](http://ffmpeg.org/download.html)
* [Get Apache ActiveMQ](http://activemq.apache.org/download.html)


Building Opencast
-----------------

Switch to the opencast folder. If you downloaded the tarball, this is the folder you just unpacked (called something
like `opencast-community-opencast-[…]`). If you chose to download via git, use `cd opencast`. You can proceed by
building opencast (depending on the folder permissions, you might need to start the command with `sudo`):

    mvn clean install -Pdev

Please be patient, as building Opencast for the first time will take quite long.

Configure
---------

Please follow the steps of the [Basic Configuration guide](../configuration/basic.md). It will help you to set your host
name, login information, etc. Be aware that the config files now reside in the build folders for the desired
distribution. For the all-in-one distribution, this would be
`/your/path/to/opencast/build/opencast-dist-allinone-[…]/etc/`, again with `[…]` representing the selected version.

As specified in the guide, make sure you replace the default ActiveMQ configuration with the one provided in
`docs/scripts/activemq/activemq.xml`. If you installed ActiveMQ using homebrew, you can find the installation path with
`brew info activemq`. The configuration is probably located in `/usr/local/Cellar/activemq/<version>/libexec/conf/`.

ffprobe is used to analyze new videos. It is installed with FFmpeg but usually not on the path to be automatically
executed. You have to link the ffprobe to `/usr/local/bin/`. You can find the FFmpeg install directory with
`brew info ffmpeg`. Usually you would link the file with
`ln -s /usr/local/Cellar/ffmpeg/<version>/bin/ffprobe /usr/local/bin/ffprobe`.

Running Opencast
----------------

Make sure you have ActiveMQ running (unless you're running it on a different machine). Then you can start Opencast using
the start-opencast script:

    activemq start
    cd /your/path/to/opencast/
    cd build/opencast-dist-allinone-[…]
    ./bin/start-opencast

As soon as Opencast is completely started, browse to [localhost:8080](http://localhost:8080) to get to the
administration interface.
