1. install CentOS from CentOS-x.y-i386-bin-DVD.iso (5.6 and 6.0)
   (as software development workstation)
2. enable root login and login as root
   $ sudo passwd root
   $ su - root
3. unzip attached mh12install-centos.zip
   $ unzip mh12install-centos.zip
4. install everything needed for Matterhorn
   $ ./install_all.sh
   This can take up to an hour or more, depending on your host. In the beginning of this process you'll have to agree to Sun's Java license, so don't go away until then.
5. run Matterhorn
   $ ./mh_run.sh
   You will get running Matterhorn on http://<yourhost.domain>:8000
