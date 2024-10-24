Install from Repository (Debian, Ubuntu)
===========================================================================

There is a Debian software repository (DEB) available for Debian-based Linux distributions provided by Greg Logan, and
hosted at University of Osnabrück. This repository provides prebuilt Opencast installations, including all
3rd-Party-Tools. Using this method, you do not have to compile the software by yourself, but you still need to configure
it.

It may also be interesting for developers as all dependencies for Opencast usage, testing and development are provided
by the Debian repository.


Availability
------------

Note that it may take some time (usually about a week after a new release is out) before the Debian packages are available.
Watch for announcements on list or just check which versions are available in the repository.


Currently Supported
-------------------

* Debian 10 and newer amd64
* Ubuntu 18.04 and newer amd64


Supported JDKs
--------------

For Opencast 10 and newer we support JDK 11 only.


Activate Repository
-------------------

First you have to install the necessary repositories so that your package manager can access them:

* Ensure https repositories are supported:

        apt-get install apt-transport-https ca-certificates sudo wget gnupg2

* Add Opencast repository:

        echo "deb https://pkg.opencast.org/debian {{ opencast_major_version() }}.x stable" | sudo tee /etc/apt/sources.list.d/opencast.list

    It might take some time after the release of a new Opencast version before the Debs are moved to the stable
    repository. If you need the new release prior to its promotion to stable you can use the testing repository.
    Note that the testing repository is an additional repository and still requires the stable repository to be active.

        echo "deb https://pkg.opencast.org/debian {{ opencast_major_version() }}.x stable testing" | sudo tee /etc/apt/sources.list.d/opencast.list

* Add the repository key to your apt keyring:

        wget -qO - https://pkg.opencast.org/gpgkeys/opencast-deb.key | sudo apt-key add -

    On latest Debian based systems (Debian 11+, Ubuntu 22.04+) importing gpg keys with `apt-key` is deprecated. You can use an alternative step:

        wget -qO - https://pkg.opencast.org/gpgkeys/opencast-deb.key | gpg --dearmor | sudo dd of=/etc/apt/trusted.gpg.d/opencast-deb.gpg

* Update your package listing

        apt-get update


Install OpenSearch
------------------

Starting with Opencast 14, OpenSearch is now a dependency.  Our packages do not explicitly depend on OpenSearch
because it runs externally to Opencast.  By default we expect OpenSearch to be running on the admin node, however
you can configure the URL in Opencast's configuration files.

In our repository we provide validated OpenSearch packages copied from the upstream repository.  Installation can be
accomplished by running the following:

    apt-get install opensearch

Furthermore, the `analysis-icu` plugin for OpenSearch is required to install. It is necessary for sorting naturally.
To install the ICU plugin, run the following:

    bin/opensearch-plugin install analysis-icu

If you wish to use the upstream OpenSearch repository directly be aware that Opencast only supported with OpenSearch 1.x
and will not work with OpenSearch 2.x yet.  Future support for this is forthcoming.



Configure OpenSearch
--------------------

After installing OpenSearch please make sure to follow their
[configuration documentation](https://opensearch.org/docs/1.3/install-and-configure/install-opensearch/debian/)
to ensure that your OpenSearch instance is set up correctly and securely.  Once that setup is complete, ensure that
your Opencast install matches your configured OpenSearch settings.  Notably, Opencast's current default assumes
non-secured http rather than https, without a username and password.  Read the
[Opencast OpenSearch Documentation](../configuration/searchindex/elasticsearch.md) to correctly configure Opencast's connection
once Opencast has been installed below.

After installing and configuring make sure to start and enable OpenSearch:

```sh
systemctl restart opensearch
systemctl enable opensearch
```


Install Opencast
------------------

### Basic Installation

For a basic installation (All-In-One) just run:

    apt-get install opencast-{{ opencast_major_version() }}-allinone opensearch

This will install the default distribution of Opencast and all its dependencies, including the 3rd-Party-Tools.  Note
that while the repository provides a packaged version of FFmpeg, your distribution may have a version which is
pre-installed or otherwise takes precedence.  This version may work, however Opencast only formally supports the
version(s) in the repository.  To install the Opencast version of ffmpeg add `ffmpeg-dist` to the end of the command above.
For more options, see the [advanced installation section below](#advanced-installation).


Configuration
-------------

Make sure to set your hostname, login information and other configuration details by following the

- [Basic Configuration guide](../configuration/basic.md)


Start Opencast
--------------

Finally, start and enable Opencast by running:

```sh
systemctl start opencast.service
systemctl enable opencast.service
```


Advanced Installation
---------------------

While the basic installation will give you an all-in-one Opencast distribution which is nice for testing, you might
want to have more control over your system and deploy it over several machines by choosing which parts of Opencast you
want to install. You can list all Opencast packages with:

    apt-cache search opencast

This will list all available Opencast distributions in the form `opencast-<version>-<dist-type>`

Some available distributions are:

* opencast-X-allinone
* opencast-X-admin
* opencast-X-presentation
* opencast-X-worker

…where `X` stands for a specific Opencast version. These packages will install the latest release for a given version,
so opencast-{{ opencast_major_version() }}-admin will install the admin profile for Opencast {{ opencast_major_version() }}.x.
Once an update to Opencast {{ opencast_major_version() }} has been released an `apt-get update` followed by `apt-get upgrade`
will upgrade you to the latest Opencast {{ opencast_major_version() }} release.

To list all available packages and versions, use:

    apt list 'opencast*'


Point Revisions (Experts only)
------------------------------

If for some reason you wish to install a specific point revision of Opencast, and the repository still hosts that point
revision, you can select it by adding it, and the packaging build, to your `apt-get install` line.  For example:

    apt-get install opencast-{{ opencast_major_version() }}-admin={{ opencast_major_version() }}.0-2

Installs an Opencast {{ opencast_major_version() }}.0 admin node, using the second build of that series.  Not all series have more than a single build,
and older point revisions may be removed once superceded, so please explore the repository prior to attempting this.


Install 3rd-party-tools
-----------------------

This step is optional and only recommended for those who want to build Opencast from source. If you install Opencast
from the repository, all necessary dependencies will be installed automatically.

You can install all necessary 3rd-Party-Tools for Opencast like this:

    apt-get install ffmpeg-dist tesseract-ocr sox hunspell netcat


Upgrading Major Versions
------------------------

Note: All upgrade between major versions are required.  If you want to upgrade Opencast 8 to Opencast 10 you have to
first upgrade to Opencast 9.

While these packages will automatically upgrade you to the latest point version in a release series, they do not
automatically upgrade you to the latest major version. In other words, if you install `opencast-9-admin` you get the
latest 9.x release, not the latest 10.x release. To upgrade from one version to another you first stop Opencast:

        systemctl stop opencast.service

As a reminder, these instructions will change your Opencast installation, and files to a new version which is likely
incompatible with older versions. If you are performing this on a production system, please ensure you have valid
backups prior to taking the next steps.

Uninstall your current Opencast packaging (using Opencast 8 as an example):

    apt-get remove opencast-8-*

Then install the new version (using Opencast 9 as an example):

    apt-get install opencast-9-allinone

At this point you must follow the relevant [upgrade](../upgrade.md) instructions, prior to starting Opencast again.


Uninstall Opencast
--------------------

To uninstall Opencast, you can run:

    apt-get remove 'opencast*'

This will not touch your created media files or modified configuration files.  If you want to remove them as well, you
have to do that by yourself.

    # Remove media files
    sudo rm -rf /srv/opencast

    # Remove local db, search indexes and working files
    sudo rm -rf /var/lib/opencast

    # Remove configuration files
    sudo rm -rf /etc/opencast

    # Remove logs
    sudo rm -rf /var/log/opencast


Troubleshooting
---------------

### Missing Dependencies

This repository expects that the `stable` section is always available, regardless of which version of Opencast you have
installed.  The 3rd party tools (e.g. FFmpeg) may or may not be in the other sections, but if they are there it is
only during a testing period for a new version.  For day-to-day use, please install them from `stable`!
