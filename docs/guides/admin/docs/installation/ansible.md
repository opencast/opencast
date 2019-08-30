Install from Repository via Ansible (Debian, and RedHat based distros)
===========================================================================

We provide [Ansible](https://ansible.com) installation and configuration scripts which drastically reduce the time
required to set up an Opencast installation, and help you manage the configuration across your entire cluster.  Whether
you are installing a single machine, or dozens, these scripts should do the basic setup of your Opencast install.  These
scripts use the package repository, so the first step is to register.

Note that these scripts are meant as a basis to get you started and you will likely need to adjust and extend them to
fit your local environment for which you need to have some basic knowledge of Ansible. Please review the scripts before
using them to make sure they fit your needs.

Examples in this document refer to `7.x` and `r/7.x`.  You should replace those with the version you want to install!

Registration
------------

Before you can start you need to get an account for the repository. You will need the credentials that you get by mail
after the registration to successfully complete this manual. The placeholders `[your_username]` and `[your_password]`
are used in this manual wherever the credentials are needed.

[Please visit pkg.opencast.org](https://pkg.opencast.org)


Install Ansible
---------------

Ansible may be available from your distribution's packaging manager, or you may need to install it manually. See the
[Ansible installation documentation](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html)
for more details.


Script Setup
------------

The next step is getting the scripts themselves.  Go to the [list of branches](https://github.com/opencast/oc-config-management/branches)
and then click on the branch version you want.  This *must* match the desired Opencast version, so if you want the
latest Opencast 7 release click on `r/7.x`.  Then click on `Clone or download`, then `Download Zip`.  You will need
to decompress this file someplace handy, and then run terminal commands from inside that directory.

Alternatively, if you familiar with `git` you can just clone the repository like this

    git clone -b r/7.x https://github.com/opencast/oc-config-management


Install Opencast
------------------

To complete your install, please follow the instructions in the README.md documentation included in the scripts.
