Install from Source (RedHat Enterprise Linux, CentOS, Scientific Linux, Fedora)
===============================================================================

Preparation
-----------

Create a dedicated Opencast user:

    useradd -d /opt/matterhorn opencast

Get Opencast source:

You can get the Opencast source code by either downloading a tarball of the source code or by cloning the Git
repository. The latter option is more flexible, it is easier to upgrade and in general preferred for developers. The
prior option, the tarball download, needs less tools and you do not have to download nearly as much as with Git.

Using the tarball:

Select the tarball for the version you want to install from [https://bitbucket.org/opencast-community/matterhorn/downloads](https://bitbucket.org/opencast-community/matterhorn/downloads)

    # Download desired tarball
    curl -O https://bitbucket.org/opencast-community/matterhorn/...
    tar xf develop.tar.gz
    mv opencast-community-matterhorn-* /tmp/matterhorn/

Cloning the Git repository:

    cd /tmp
    git clone https://bitbucket.org/opencast-community/matterhorn.git
    cd matterhorn
    git tag   <-  List all available versions
    git checkout TAG   <-  Switch to desired version


Install Dependencies
--------------------

Please make sure to install the following dependencies. Note that not all dependencies are in the system repositories.

Required:

    java-devel >= 1:1.7.0
    ffmpeg >= 2.5
    maven >= 3.1

Required (not necessarily on the same machine):

    ActiveMQ >= 5.10 (older versions untested)

Required for text extraction (recommended):

    tesseract >= 3

Required for hunspell based text filtering (optional):

    hunspell >= 1.2.8

Required for audio normalization (optional):

    sox >= 14

### Dependency Download

Pre-built versions of most dependencies that are not in the repositories can be downloaded from the respective project
website:

 - [Get FFmpeg](http://ffmpeg.org/download.html)
 - [Get Apache Maven](https://maven.apache.org/download.cgi)
 - [Get Apache ActiveMQ](http://activemq.apache.org/download.html)


Building Opencast
-------------------

Switch to user `opencast`:

    sudo su - opencast

Compile the source code:

    cd /tmp/matterhorn
    mvn clean install

Create the Karaf distribution:

    cd assemblies/karaf-dist-allinone
    mvn clean install

Extract the all-in-one distribution

    tar xf karaf-dist-allinone/target/opencast-karaf-dist-allinone-${VERSION}.tar.gz
    mv opencast-karaf-dist-allinone-${VERSION} /opt/matterhorn

Make sure everything belongs to the user `opencast`:

    sudo chown -R opencast:opencast /opt/matterhorn


Configure
---------

Please follow the steps of the [Basic Configuration guide](../configuration/basic.md).
It will help you to set your hostname, login information, …


Running Opencast
------------------

Opencast is running on top of Apache Karaf. Please refer to the [Karaf documentation](http://karaf.apache.org/manual/latest-3.0.x/users-guide/start-stop.html)
for further information about the different start modes.

As soon as Opencast is completely started, browse to [http://localhost:8080](http://localhost:8080) to get to the administration interface.


Run Opencast as a service
-------------------------

Usually, you do not want to run Opencast in interactive mode but as system service to make sure it is only running
once on a system and is started automatically.

Karaf comes with built-in support for wrapping an installation as a system service. Please take note of the further
instructions which can be found in the [Karaf documentation](http://karaf.apache.org/manual/latest-3.0.x/users-guide/wrapper.html).


Customizing the installation
----------------------------

The Opencast installation can easily be further customized. With Karaf as a management layer, it is very easy to install
additional bundles via the [Karaf Console](http://karaf.apache.org/manual/latest-3.0.x/users-guide/console.html).

    bundle:install -s mvn:<package>/<identifier>/<version>

For more advanced scenarios, creating a customized distribution would be another option. The existing Opencast distributions
found in the `assemblies` serve as a good starting point for doing this.

