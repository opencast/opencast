# Prerelease Opencast Packages and Containers

Shortly after a branch cut we start providing prerelease packages for the new branch.  In general you can expect a new
verison any time a pull request has been merged.  This may be more than once a day, or it may be very rare late in the
life cycle of a branch.

<div class=warn>
Note: These packages come with *absolutely no support*, and may be broken at any time.  Do not install these on
anything other than a testing system.  Do not feed the only copy of your recordings to these systems.  These
packages may break their host operating system, and may eat your dog.  Do not feed these packages after midnight.
</div>


Redhat Based Distributions
--------------------------

Add a repository configuration containing the following to `/etc/yum.repos.d/opencast-unstable.repo`.  Note that the `el`
version must match your distribution's version!

    [opencast-unstable-noarch]
    name = Opencast el 8 Repository - noarch
    baseurl  = http://ci.opencast.org/rpms/unstable/el/8/oc-{{ opencast_major_version() }}/noarch
    enabled  = 1
    gpgcheck = 1
    gpgkey = http://ci.opencast.org/keys/public-signing-key.gpg

Note: This repo still requires the normal Opencast repository for dependencies (notably ffmpeg).  Configuration for
these packages should follow the [standard instructions](../configuration/basic.md)

Debian Based Distributions
--------------------------

First install the repository signing key with the following commands

    curl http://ci.opencast.org/keys/public-signing-key.gpg | sudo gpg --no-default-keyring --keyring=/usr/share/keyrings/opencast-unstable.gpg --import -

The insert the following into `/etc/apt/sources.list.d/opencast-unstable.list`

    deb [signed-by=/usr/share/keyrings/opencast-unstable.gpg] http://ci.opencast.org/debian {{ opencast_major_version() }}.x unstable

Note: This repo still requires the normal Opencast repository for dependencies (notably ffmpeg).  Configuration for
these packages should follow the [standard instructions](../configuration/basic.md)

Docker Images
-------------

All container images provide a `next` tag, e.g. `quay.io/opencast/allinone:next`. This version is a nightly build of the
next upcoming major version. As such, it follows the latest release branch `r/Y.x` until `Y.0` is released. After that,
it builds from `develop` until the next release branch is cut. This change is announced on list.

Configuration for these images should follow the [standard instructions](../configuration/basic.md)
