Install Matterhorn Core Server
==============================

Installation from Source
------------------------

These guides will help you to build Matterhorn yourself, including all
necessary third party tools. This method will most likely work on all Linux and
Unix based systems.

 - Officially Supported [^officialsupport]
    - [Ubuntu](source-debian-ubuntu.md)
    - RedHat Enterprise Linux
    - CentOS
    - Scientific Linux
 - Other Systems
    - Fedora
    - [Debian](source-debian-ubuntu.md)
    - openSUSE
    - SLES
    - Mac OS X


Installation from Repository
----------------------------

For some operating systems it is possible to install Matterhorn from an RPM
repository, providing a pre-configured Matterhorn installation neatly
integrated into the operating system.

 - Officially Supported [^officialsupport]
    - [RedHat Enterprise Linux](rpm-rhel-sl-centos.md)
    - [CentOS](rpm-rhel-sl-centos.md)
    - [Scientific Linux](rpm-rhel-sl-centos.md)
 - Other Common Systems
    - [Fedora](rpm-fedora.md)
    - SLES


Installation Across Multiple Servers
------------------------------------

For production systems it is recommended to install Matterhorn across multiple
servers to separate the processing, management and presentation layer so that
even if the processing layer is under full load people can still watch
recodings unaffected since the presentation layer is running on a separate
machine, …

 - [Installation Across Multiple Servers](multiple-servers.md)

Scripted Installation from Source
---------------------------------

There are Ansible playbooks scripts that can be used to deploy a full
Matterhorn cluster, build from source for some systems. It is basically the
installation from source without having to do the steps manually.

 - Debian
 - Ubuntu


[^officialsupport]: Official support does not mean that Matterhorn will only
work on these systems, but it means that these are the systems Matterhorn is
tested on regularly. That includes especially the QA tests done for each stable
release. Thus, if those operating systems are an option for you, take one of
them. If not, feel free to use any other Linux distribution.
