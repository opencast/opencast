1. install Ubuntu from ubuntu-xx.yy-desktop-i386.iso (10.04 or 12.04)
   (with default parameters) on a new machine
2. enable root login and login as root on a new machine
   $ sudo passwd root
   $ su - root
   If you still want to install as a normal user, in which case you'll
   be asked for sudo password from time to time, you can either:
   - temporarily disable sudo password timeout by adding the following line
     to /etc/sudoers:
     Defaults  passwd_timeout = 0
   - or completely disable asking for sudo password in /etc/sudoers:
     %wheel  ALL=(ALL)  NOPASSWD: ALL
3. copy all files from this SVN directory to some local directory
   and cd to that directory
4. install everything needed for Matterhorn
   $ ./install_all.sh 2>&1 | tee -a install.log
   This can take up to an hour or more, depending on your host. In the beginning
   of this process you'll have to agree to Sun's Java license, so don't go away
   until then.
5. run Matterhorn
   $ ./mh_run.sh 2>&1 | tee -a run.log
   You will get running Matterhorn on http://<yourhost.domain>:8000
