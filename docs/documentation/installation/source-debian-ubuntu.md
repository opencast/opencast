Install from Source (Debian, Ubuntu)
====================================

These instructions outline how to install an all in one Matterhorn system on Ubuntu 12.04 with LTS.

Preparatiom
-----------

Create Matterhorn installation directory

    sudo mkdir -p /opt/matterhorn
    sudo chown $USER:$GROUPS /opt/matterhorn

Get Matterhorn source:

You can get the Matterhorn source code by either downloading a tarball of the source code or by cloning the Git
repository. The latter option is more flexible, it is easier to upgrade and in general preferred for developers. The
prior option, the tarball download, needs less tools and you do not have to download nearly as much as with Git.

Using the tarball:

 - Download desired tarball from https://bitbucket.org/opencast-community/matterhorn/downloads#tag-downloads
 - Extract the tarball
   ```
   tar xf develop.tar.gz
   ```
 - Move the source to `/opt/matterhorn`
   ```
   mv opencast-community-matterhorn-* /opt/matterhorn/
   ```

Cloning the Git repository:

    git clone https://bitbucket.org/opencast-community/matterhorn.git
    cd matterhorn
    git tag   <-  List all available versions
    git checkout TAG   <-  Switch to desired version


Install
-------

### Java:

    sudo apt-get install openjdk-7-jdk

Make sure that openjdk ≥ 7 is the prefered Java version (`java -version`). Otherwise run
`sudo update-alternatives --config java`.

### Apache Maven:

    sudo apt-get install maven

### Gstreamer:

    sudo apt-get install gstreamer0.10-plugins-base
    sudo apt-get install gstreamer0.10-plugins-good
    sudo apt-get install gstreamer0.10-gnonlin
    sudo apt-get install gstreamer0.10-ffmpeg


Configure
---------

Please follow the steps of the Basic Configuration guide. It will help you to set your hostname, login information, …


Build
-----

### Matterhorn

    export MAVEN_OPTS='-Xms256m -Xmx960m -XX:PermSize=64m -XX:MaxPermSize=256m'
    cd /opt/matterhorn
    mvn clean install -DdeployTo=/opt/matterhorn

### Third-party tools

    cd /opt/matterhorn/docs/scripts/3rd_party

Read README file for additional instructions


Run
---

Export environment variables

    echo "export M2_REPO=/home/$USER/.m2/repository" >> ~/.bashrc
    echo "export FELIX_HOME=/opt/matterhorn" >> ~/.bashrc
    echo "export JAVA_OPTS='-Xms1024m -Xmx1024m -XX:MaxPermSize=256m'" >> ~/.bashrc
    source ~/.bashrc

Run Matterhorn

This method is intended for testing, debugging and development. The start script might pose security risks for public
systems like enabled debugging, JMX, etc. For production use, please have a look at the service scripts in the next
section.

    sh /opt/matterhorn/bin/start_matterhorn.sh

Browse http://localhost:8080


Run Matterhorn as Service
-------------------------

Edit the SysV-init script and configure it for your system. Then copy it to `/etc/init.d`

    cp /opt/matterhorn/docs/scripts/init/old/etc-init.d-matterhorn /etc/init.d/matterhorn

When you run an init script on Ubuntu, the shell variables from .bashrc are unfortunately ingnored and you need to set
the user that runs Matterhorn (in this example "matterhorn"). So you need to edit /etc/init.d/matterhorn and set
`FELIX_HOME`:

    /etc/init.d/matterhorn
    FELIX_HOME=/opt/matterhorn
    ...
    MATTERHORN_USER="matterhorn"

Then start Matterhorn as service:

  service matterhorn start
