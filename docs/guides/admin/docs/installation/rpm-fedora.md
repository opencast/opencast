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


Install Opencast
----------------

For the installation of Opencast, please have a look at the [installation from source documentation
](source-linux.md).
