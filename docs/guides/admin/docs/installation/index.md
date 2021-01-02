Install Opencast
================


Installation from Repository
----------------------------

There are package repositories available for multiple operating systems. It provides packages containing pre-configured and
pre-built Opencast installations.

* [RedHat Enterprise Linux 8](rpm-el8.md)
* [CentOS 8](rpm-el8.md)
* [RedHat Enterprise Linux 7](rpm-el7.md)
* [CentOS 7](rpm-el7.md)
* [Scientific Linux 7](rpm-el7.md)
* [Fedora](rpm-fedora.md)
* [Debian](debs.md)
* [Ubuntu](debs.md)


Installation from Source
------------------------

These guides will help you to build Opencast, including all necessary third party tools. This method will most likely
work on all Unix-like systems.

* [RedHat Enterprise Linux](source-linux.md)
* [CentOS](source-linux.md)
* [Scientific Linux](source-linux.md)
* [Fedora](source-linux.md)
* [Debian](source-linux.md)
* [Ubuntu](source-linux.md)
* [Mac OS X](source-macosx.md)

Building on most other Unix-like operating systems should be very much alike.


Installation via Script
-----------------------

We provide configuration scripts to install and configure Opencast automatically.  These scripts rely on the
packages from the repository above.

* [Ansible](ansible.md)


Installation with Docker
----------------------------

You can also use Docker to quickly install or test Opencast. There are multiple Docker images available for installing
Opencast on either a single or multiple server.

* [Testing Locally with Docker](docker-local.md)


Installation Across Multiple Servers
------------------------------------

For production systems, it is recommended to install Opencast across multiple servers to separate the processing,
management and presentation layer, so that, for example, even if the processing layer is under full load, users can
still watch recordings unaffected since the presentation layer is running on a separate machine.

* [Installation Across Multiple Servers](multiple-servers.md)
