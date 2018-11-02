Install from Repository (Fedora)
================================

The Opencast RPM repository for Fedora has been discontinued since Fedora with RPMfusion now provides nearly all
necessary dependencies for Opencast. Use the following steps to install them, then continue with the [installation from
source](source-linux.md).

*This guide is to be merged into the guide for the installation from source.*


Add RPMfusion repository
------------------------

[RPMFusion](https://rpmfusion.org/) is a community-driven RPM repository for Fedora. It provides tools like FFmpeg. You
can activate it using:

    dnf install --nogpgcheck \
      http://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm \
      http://download1.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm


Install 3rd-party-tools
-----------------------

You can install all necessary 3rd-Party-Tools for Opencast like this:

    dnf install maven ffmpeg tesseract hunspell sox synfig nmap-ncat

For additional Unicode tests run during the build process, you can also install:

    dnf install hunspell-de tesseract-langpack-deu


Install Apache ActiveMQ
-----------------------

The Apache ActiveMQ message broker is commonly installed on the same machine as Opencast for an all-in-one system. The
version of ActiveMQ shipped with Fedora is too old but you can use the [ActiveMQ-dist Copr RPM repository
](https://copr.fedoraproject.org/coprs/lkiesow/apache-activemq-dist/)

Make sure it is properly configured for Opencast. For more information about the setup, have a look at the
[message broker configuration documentation](../configuration/message-broker.md).


Install Opencast
----------------

For the installation of Opencast, please have a look at the [installation from source documentation
](source-linux.md).
