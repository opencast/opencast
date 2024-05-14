Packaging Guidelines
====================

This page is intended as a guideline for packagers. It may help to figure out where to place parts of Opencast.  The
locations, etc. proposed here should never overrule the official packaging guides for a specific operating system or
distribution.  If in doubt follow the guides for your distribution like for example the [Fedora Packaging
Guidelines](http://fedoraproject.org/wiki/Packaging:Guidelines)


Introduction
------------

In a Unix file system there are different places for different types of data. Executables, for example, may be placed in
`/usr/bin`, libraries in `/usr/lib`, etc. These places are defined by the operating system distributor and the
[Filesystem Hierarchy Standard](http://www.pathname.com/fhs/pub/fhs-2.3.html). Latter is followed by almost every major
distributor, but not everything in there is clearly defined.

Especially software which is installed automatically–for example software from RPM or DEB repositories–should follow
these rules so conflicts are minimized and the user will have one place to look for one kind of data. For example if you
are searching for a system-wide configuration file for any software on Linux every user will always look in `/etc`.

If you want to package Opencast use the following documentations to decide where to place files:

* Distribution guidelines like the [Fedora Packaging Guidelines](http://fedoraproject.org/wiki/Packaging:Guidelines)
* [Filesystem Hierarchy Standard](http://www.pathname.com/fhs/pub/fhs-2.3.html)
* This Guide


Locations To Use
----------------

The following locations should be used for Opencast and its related data:

* `/usr/share/opencast`:
  Software and data not modified by Opencast. This includes felix, the Opencast modules and external libraries.
* `/etc/opencast`:
  Opencast related configuration files (Felix and service configuration, workflows, encoding profiles, etc.)
* `/var/log/opencast`:
  The Opencast logfiles. Consider to enable logrotate for this directory.
* `/srv/opencast` or `/var/lib/opencast`: Opencast storage, including the recordings, the archive, the Solr indexes,
  etc. You may use one of these directories or both. For more details have a look at the explanation below and the
  discussion in the comments.
* `/tmp/opencast`:
  Temporary data which are not necessarily preserved between reboots. This includes the felix-cache and other temporary
  data.
* `/usr/sbin/opencast`:
  Opencast startscript
* `/etc/init.d/opencast`
  SysV-Initscript (if necessary)


Reasoning for these Locations
-----------------------------

### /usr/share/opencast – Opencast Software Components

The Filesystem Hierarchy Standard states that “*The /usr/share hierarchy is for all read-only architecture independent
data files.*” and that “*Any program or package which contains or requires data that does not need to be modified should
store that data in /usr/share*”.  It is also used for this purpose by cups, emacs, cmake, pulseaudio, gimp, … It sould
be used for felix.jar and all the modules (lib directory)

### /etc/opencast – Opencast Configuration

The Filesystem Hierarchy Standard states that “*The /etc hierarchy contains configuration files. A "configuration file"
is a local file used to control the operation of a program; it must be static and cannot be an executable binary.*”

### /var/log/opencast/ – Opencast Logs

The Filesystem Hierarchy Standard states that “*This directory contains miscellaneous log files. Most logs must be
written to this directory or an appropriate subdirectory.*”

### /srv/opencast and/or /var/lib/opencast/ – Data modified by Opencast

About this the Filesystem Hierarchy Standard says that “*This hierarchy holds state information pertaining to an
application or the system. State information is data that programs modify while they run, …*” also “*/var/lib/<name> is
the location that must be used for all distribution packaging support…*”


Why Not Use /opt For Packages
-----------------------------

While it is ok to place software in `/opt` if you install the manually as `/opt` is intended to be used for “*Add-on
application software*” by the Filesystem Hierarchy Standard, it should never be used for automatic installations (RPMs
Debian packages, …).. The Fedora Packaging Guidelines for example are pretty clear about this:

“*No Files or Directories under /srv, /opt, or /usr/local […] In addition, no Fedora package can have any files or
directories under /opt or /usr/local, as these directories are not permitted to be used by Distributions in the FHS.

The reason for this is that the FHS is handing control of the directory structure under /opt to the system administrator
by stating that “*Distributions […] must not modify or delete software installed by the local system administrator …*”.

That is something you cannot guarantee with automatic installations. For example if you use RPMs, the only way to do
this would be to mark every single file (binaries, modules, assets, …) as configuration files which are not to be
replaced in case they are modified. It is quite obvious that this would be a a really bad idea leading to a number of
further problems.


Notice For System Operators
---------------------------

This guide is supposed to defines default locations for an Opencast system. It does not restrict your own system
configuration.

For a Opencast system it is for example quite common to mount an external storage (NFS, …) and use it as storage for
Opencast. You do not have to mount it to `/var/lib/opencast` if you do not want to. Instead, mount it in /media or
wherever you want–it is your system afterall–and either change the Opencast configuration to use the directory of your
directly, or put appropriate symlinks in `/var/lib/opencast`. This is, however, system specific and should not be done
for packages.
