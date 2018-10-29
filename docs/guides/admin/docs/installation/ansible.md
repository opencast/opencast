Install via Script using the Repository (Debian, Ubuntu)
===========================================================================

Installing Opencast can be complex, so we have built a set of Ansible scripts to handle the installation and 
configuration.  These scripts should handle most common installation setups, and are ideal for someone wishing to 
quickly and easily set up Opencast for testing.  These scripts are also a good base upon which to build your own, local
configuration management scripts.

The Ansible scripts are tested on Debian 8+, Ubuntu 16.04+, and CentOS 7+, but may also work on other systems.


Setup
-----

These scripts use [Ansible](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html), which
allows you to easily and automatically manage large numbers of machines via SSH.  Install Ansible before continuing
with this guide.


Registration
------------

Before you can install Opencast you need to get an account for the repository. You will need the credentials that you 
get by mail after the registration to successfully complete this manual. The placeholders `[your_username]` and 
`[your_password]` are used in this manual wherever the credentials are needed.

[Please visit https://pkg.opencast.org](https://pkg.opencast.org)


Testing Use
-----------

The scripts can be found [here](https://github.com/gregorydlogan/oc-config-management).  If you are familiar with `git`
make a local clone, otherwise download a zip file of the scripts.  Read through the documentation, which contains
additional setup steps.

Once you have set up your host configuration in `hosts`, run 
`ansible-playbook -K -i hosts opencast.yml --extra-vars "repo_username=[your_username] repo_password=[your_password]"`.
If you are uncomfortable with your username and password on the commandline, you can set those variables in 
`group_vars/all.yml`.

If you successfully install Opencast, but notice you have made a mistake in your configuration you can use the `config`
advanced option.  Read the script documentation for more detail.

Production Use
--------------

Similar to testing, you need to read the documentation and perform the additional installation steps.  The only
differences between testing and production use are:

 - Setting different passwords for all password options in `group_vars/all.yml`.  These scripts default to the common
   passwords used worldwide by Opencast.  You do not want to use these passwords in production.
 - Changing `install_mariadb` to `false`.  These scripts install MariaDB from your distribution's package manager by
   default, but some institutions may want to use an institution-wide MariaDB instance.  If this is true for you, set
   this value to `false` to prevent installation of MariaDB.  The database schema will still be imported, although you
   may need to create the schema itself manually.
 - Changing `handle_network_mounts` to `false`.  These scripts install NFS from your distribution's package manager by
   default, but some institutions may want to use an appliance, or want to manage NFS themselves.  If this is true for
   you, set this value to `false` to prevent installation of NFS on the fileserver, and the creation of mounts on the 
   Opencast nodes.

Upgrading Major Versions
------------------------

These scripts do not attempt to intelligently update your Opencast installation across major versions.  They will
however upgrade your installation to the newest *minor* version.  For example, if you have Opencast 3.6 installed,
these scripts will upgrade you to 3.7, but not 4.0.  To upgrade major versions, follow the upgrade steps listed in the
[RPM](rpm-rhel-sl-centos.md) or [Deb](debs.md) instructions, depending on your installed distribution.

