Install Opencast
================

Installation from Source
------------------------

These guides will help you to build Opencast, including all necessary third party tools. This method will most likely
work on all Unix-like systems.

 - [RedHat Enterprise Linux](source-rhel-sl-centos.md)
 - [CentOS](source-rhel-sl-centos.md)
 - [Scientific Linux](source-rhel-sl-centos.md)
 - [Fedora](source-rhel-sl-centos.md)
 - [Debian](source-debian-ubuntu.md)
 - [Ubuntu](source-debian-ubuntu.md)

Building on most other Unix-like operating systems should be very much alike.


Installation from Repository
----------------------------

There is an RPM repository available for some operating systems. It provides packages containing pre-configured and
pre-built Opencast installations.

 - [RedHat Enterprise Linux](rpm-rhel-sl-centos.md)
 - [CentOS](rpm-rhel-sl-centos.md)
 - [Scientific Linux](rpm-rhel-sl-centos.md)
 - [Fedora](rpm-fedora.md)


Installation Across Multiple Servers
------------------------------------

For production systems, it is recommended to install Opencast across multiple servers to separate the processing,
management and presentation layer, so that, for example, even if the processing layer is under full load, users can
still watch recordings unaffected since the presentation layer is running on a separate machine.

 - [Installation Across Multiple Servers](multiple-servers.md)
