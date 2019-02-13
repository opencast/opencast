Install via Script using the Repository
===========================================================================

Installing Opencast can be complex, so we have built a set of Ansible scripts to handle the installation and 
configuration.  These scripts should handle most common installation setups, and are ideal for someone wishing to 
quickly and easily set up Opencast for testing.  These scripts are also a good base upon which to build your own, local
configuration management scripts.

The Ansible scripts are tested on long term stable releases of Debian, Ubuntu, and CentOS.  Currently this is Debian 8,
Ubuntu 16.04, and 18.04, and CentOS 7.  Other releases, and distributions should work, but are untested.


Setup
-----

These scripts use [Ansible](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html), which
allows you to easily and automatically manage large numbers of machines via SSH.  Install Ansible before continuing
with this guide.

The scripts can be found [here](https://github.com/opencast/oc-config-management).  If you are familiar with `git`
make a local clone, otherwise download a zip file of the scripts.  Read through the documentation, which contains
additional setup steps.


Registration
------------

Before you can install Opencast you need to get an account for the repository. You will need the credentials that you 
get by mail after the registration to successfully complete this manual. The placeholders `[your_username]` and 
`[your_password]` are used in this manual wherever the credentials are needed.

[Please visit https://pkg.opencast.org](https://pkg.opencast.org)


Host Setup
----------

All of the machines defined in your `hosts` file must have a user (by default here, named `ansible`) who matches all of
the following conditions

- Accepts your SSH key.  You should be able to run `ssh ansible@$hostname` and be logged in without a password.
- Has sudo access, and has the same password on each machine.

Each host must also have Python installed.  This is an Ansible requirement, but on some variants (Ubuntu) it is missing
for some installs.


Authentication Setup
--------------------

Open `group_vars/all.yml` in your favorite text editor, and fill in the `repo_username` and `repo_password` fields with
your credentials for the package repository.  Ensure the `ansible_user` value matches the username for the user you
configured above.  The rest of the keys are pre-populated with the common passwords used by default with Opencast.  For
testing purposes these are fine, but you are encouraged to change them anyway. Once you have changed them use
[Ansible Vault](https://docs.ansible.com/ansible/2.7/user_guide/vault.html) to protect the authentication data!  The
most basic way is to run `ansible-vault encrypt group_vars/all.yml`.  It will prompt you for a password which will be
required to decrypt this data for use at runtime.  To avoid the prompt for the vault password please consult the
[Ansible Vault documentation](https://docs.ansible.com/ansible/2.7/user_guide/vault.html#providing-vault-passwords),
and modify the `ansible-playbook` command below accordingly.


Deploying Opencast
------------------

To deploy Opencast after protecting your authentication data with Ansible Vault run
`ansible-playbook -K -i hosts --vault-id @prompt opencast.yml`.  This will prompt you for the `sudo` password for the
cluster, and the vault password for your authentication data.

Alternatively, if you want to just start testing without protecting your credentials, or changing the default logins
you can skip the Authentication Setup section above and instead run
`ansible-playbook -K -i hosts opencast.yml --extra-vars "repo_username=[your_username] repo_password=[your_password]"`
instead.  This will deploy Opencast with the default credentials, using your repository username and password.  This
method runs the risk of exposing your repository credentials on a multi-user system however, so it is not recommended.


Reconfiguring Opencast
----------------------
If you successfully install Opencast, but notice you have made a mistake in your configuration you can use the `config`
advanced option.  Read the script documentation for more detail.


Production Use
--------------

Similar to testing, you need to read the documentation and perform the additional installation steps.  The only
differences between testing and production use are:

 - Setting different passwords for all password options in `group_vars/all.yml`.  These scripts default to the common
   passwords used by default by Opencast.  You do not want to use these passwords in production.  Remember to use
   `ansible-vault`, or some other secret management system to protect your authorization data!
 - Changing `install_mariadb` to `false`.  These scripts install MariaDB from your distribution's package manager by
   default, but some institutions may want to use an institution-wide MariaDB instance.  If this is true for you, set
   this value to `false` to prevent installation of MariaDB.  The database tables will still be imported, although you
   may need to create the schema itself manually.
 - Changing `handle_network_mounts` to `false`.  These scripts install NFS from your distribution's package manager by
   default, but some institutions may want to use an appliance, or want to manage NFS themselves.  If this is true for
   you, set this value to `false` to prevent installation of NFS on the fileserver, and the creation of mounts on the 
   Opencast nodes.
 - Reconfiguring most things (ie, using the `config` advanced option) aside from the authentication data will likely
   break your install.  These playbooks target basic installation rather than major changes.  Test first, then
   transition to a new instance for production.


Upgrading Major Versions
------------------------

These scripts do not attempt to intelligently update your Opencast installation across major versions.  They will
however upgrade your installation to the newest *minor* version.  For example, if you have Opencast 3.6 installed,
these scripts will upgrade you to 3.7, but not 4.0.  To upgrade major versions, follow the upgrade steps listed in the
[RPM](rpm-rhel-sl-centos.md) or [Deb](debs.md) instructions, depending on your installed distribution.

