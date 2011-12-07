1. install Ubuntu from ubuntu-xx.yy-desktop-i386.iso (10.04 and 11.04)
   (with default parameters)
2. enable root login and login as root
   $ sudo passwd root
   $ su - root
3. unzip attached mh12install-ubuntu.zip
   $ unzip mh12install-ubuntu.zip
4. install everything needed for Matterhorn
   $ ./install_all.sh
   This can take up to an hour or more, depending on your host. In the beginning of this process you'll have to agree to Sun's Java license, so don't go away until then.
5. run Matterhorn
   $ ./mh_run.sh
   You will get running Matterhorn on http://<yourhost.domain>:8000
