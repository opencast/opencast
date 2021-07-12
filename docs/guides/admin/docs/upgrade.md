Upgrading Opencast from 9.x to 10.x
===================================

This guide describes how to upgrade Opencast 9.x to 10.x.
In case you need information about how to upgrade older versions of Opencast,
please refer to [older release notes](https://docs.opencast.org).

1. Stop your current Opencast instance
2. [Upgrade Java](#upgrade-java)
3. Replace Opencast with the new version
4. [Review the configuration changes and adjust your configuration accordingly](#configuration-changes)
5. Start Opencast


Upgrade Java
------------

While Opencast 9 worked with both Java 8 and Java 11, Opencast 10 requires Java 11.
Trying to start Opencast 10 with Java 8 will fail.

Either install both java versions and set Java 11 as default using something like:

```
update-alternatives --config java
```

Or replace the old Java version like this:

```
% dnf shell
> remove 'java-1.8.0*'
> install java-11-openjdk.x86_64
> run
```

Note that these commands can differ based on your distribution and your set-up.
Make sure to adjust them accordingly.

Finally, check you have the correct version using:

```
% java --version
openjdk 11.0.11 2021-04-20
OpenJDK Runtime Environment 18.9 (build 11.0.11+9)
OpenJDK 64-Bit Server VM 18.9 (build 11.0.11+9, mixed mode, sharing)
```


Configuration Changes
---------------------

Note that this section will only highlight a few important changes.
Please make sure to compare your configuration against the current configuration files.

### LDAP

A new configuration option `org.opencastproject.userdirectory.ldap.groupcheckprefix` was added.
The option affects the `org.opencastproject.userdirectory.ldap.roleattributes` and
`org.opencastproject.userdirectory.ldap.extra.roles` configuration options and may need to be adjusted.

### Static File Delivery

Opencast 9.2 came with a [completely new system for securing static file content](configuration/serving-static-files.md)
which is now active by default in Opencast 10. If you are deferring the file access authorization to another system
using Opencast's [security token mechanism](configuration/stream-security.md), you need to deactivate this protection
in:

```
etc/org.opencastproject.fsresources.StaticResourceServlet.cfg
```
