#!/bin/bash

# Initialization
INST_DIR=/home/opencast
CONF_DIR=/opt/matterhorn/felix/conf
MOTD_FILE=/etc/motd.tail
MY_OS=`uname -sr`

# if "a" is entered by user, we will install ALL tools
inst_all=N

show_stat()
{
cat >&1 <<END
********************************************
******** Starting Matterhorn Setup ********
********************************************
** Installing third party components
**
** VM OS: $MY_OS
** VM IP: $MY_IP
**
** Matterhorn is installed in:
**    Home:    /usr/local/felix-framework-2.0.1
**    Bundles: /usr/local/felix-framework-2.0.1/load
**    Config:  /usr/local/felix-framework-2.0.1/conf
**
** For further information, please visit
**   http://www.opencastproject.org
********************************************

For a complete list of 3rd party tools, please visit:
  http://wiki.opencastproject.org/iconfluence/display/open/3rd+Party+Licensesi+and+Software

** Install process may take 40-60 minutes, depending on computer configuration and network speed.

** PLEASE NOTE: You may be prompted for the login password to authorize the installation script.
END
}

start_mh ()
{
  echo "Starting Matterhorn..."

  FELIX=felix
  FELIX_DIR=/opt/matterhorn/$FELIX

  export OC=/opt/matterhorn
  export FELIX_HOME=/opt/matterhorn/felix
  export RED5_HOME=/opt/matterhorn/red5
  export M2_REPO=/home/opencast/.m2/repository
  export OC_URL=http://opencast.jira.com/svn/MH/trunk/
  export FELIX_URL=http://apache.mirror.iweb.ca/felix/felix-framework-2.0.1.tar.gz
  export JAVA_HOME=/usr/lib/jvm/java-6-sun
  export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"

  cd $INST_DIR
  sudo update-rc.d matterhorn defaults
  sudo service matterhorn start

  echo "" | sudo tee -a $MOTD_FILE
  echo "********************************************" | sudo tee -a $MOTD_FILE
  echo "** Matterhorn console is at http://$MY_IP:8080" | sudo tee -a $MOTD_FILE
  echo "**" | sudo tee -a $MOTD_FILE
  echo "** Matterhorn is installed in:" | sudo tee -a $MOTD_FILE
  echo "**    Home:    /opt/matterhorn/felix" | sudo tee -a $MOTD_FILE
  echo "**    Bundles: /opt/matterhorn/felix/load" | sudo tee -a $MOTD_FILE
  echo "**    Config:  /opt/matterhorn/felix/conf" | sudo tee -a $MOTD_FILE
  echo "********************************************" | sudo tee -a $MOTD_FILE

  # remove matterhorn setup script
  sudo mv /etc/profile.d/matterhorn_setup.sh /home/opencast/.
}

############################### START HERE ###############################
# Turn off screensaver
setterm -blank 0 

MY_OS=`uname`

# Wait for network connection
for ntime in 1 2 3 4 5 6 7 8 9 10
do
  MY_IP=`ifconfig | grep "inet addr:" | grep -v 127.0.0.1 | awk '{print $2}' | cut -d':' -f2`
  if [ ! -z $MY_IP ]; then
    break;
  fi
  echo "Waiting for network connection..."
  sleep 5
done

# Did we get connected?
if [ -z $MY_IP ]; then
  echo "** ERROR: Could not acquire IP address for this VM."
  echo "Edit file /etc/udev/rules.d/70-persistent-net.rules and remove all uncommented"
  echo "lines, and restart the VM.  For more info see Functionality section of FAQ at"
  echo "http://opencast.jira.com/wiki/display/MH/Release+0.5+FAQ"
else
  cd /tmp
  rm -f index.html
  wget -q http://www.opencastproject.org
  if [ -f index.html ]; then
    echo "Network connection verified"
    rm -f index.html
  else
    echo "**** ERROR Could not verify network connectivity.  Please check and then"
    echo "restart the VM."
    exit
  fi

  # connected, start main task
  show_stat
  echo "******** OPTIONS HAVE CHANGED, PLEASE READ CAREFULLY *********"

  # Need to get a server name, not just y/n
  proxsrv=y
  while [ ${#proxsrv} -gt 0 ] && [ ${#proxsrv} -lt 8 ]
  do
    echo "**** To set up a proxy server, please enter the URL or press enter []?"
    read proxsrv
  done

  # proxy server?
  if [ ${#proxsrv} -gt 7 ]; then
    echo "http_proxy=$proxsrv" | sudo tee /etc/profile.d/httpproxy.sh
    echo "export http_proxy" | sudo tee -a /etc/profile.d/httpproxy.sh
    sudo chown opencast /etc/profile.d/httpproxy.sh
    sudo chgrp opencast /etc/profile.d/httpproxy.sh
    sudo chmod 755 /etc/profile.d/httpproxy.sh
    export http_proxy=$proxsrv
  else
    echo "No proxy server specified."
  fi

  echo "**** Default keyboard is US; Do you want to reconfigure? [y/N]"
  read kbresp

  # Need to get a server name, not just y/n
  ntpsrv=y
  while [ ${#ntpsrv} -gt 0 ] && [ ${#ntpsrv} -lt 8 ]
  do
    echo "**** To set up a different NTP server, please enter the URL or press enter [ntp.ubuntu.com]?"
    read ntpsrv
  done

  echo "**** Do you want to change the timezone on this VM? [y/N]"
  read changetz

  echo "**** Do you want to install 3rd party tools? [Y/n]"
  read p3resp

#  echo "**** Do you want to install OpenCaps? [y/N]"
#  read opencaps
#  if [ "$opencaps" = "y" ] || [ "$opencaps" = "Y" ]; then
#    echo "**** Install OpenCaps as Matterhorn plugin only? [Y/n]"
#    read opencapsminimal
#  fi


  # update felix config (url)
  sed -i "s/http:\/\/localhost:8080/http:\/\/$MY_IP:8080/" $CONF_DIR/config.properties
  sed -i "s/rtmp:\/\/localhost\/matterhorn-engage/rtmp:\/\/$MY_IP\/matterhorn-engage/" $CONF_DIR/config.properties
  sed -i 's/\${org.opencastproject.storage.dir}\/streams/\/opt\/matterhorn\/red5\/webapps\/matterhorn\/streams/' $CONF_DIR/config.properties

  # update capture properties
  # sed -i "s/http:\/\/localhost:8080/http:\/\/$MY_IP:8080/" /opencast/config/capture.properties

  # Reconfigure Keyboard?
  if [ "$kbresp" = "y" ] || [ "$kbresp" = "Y" ]; then
    sudo dpkg-reconfigure console-setup
  else
    echo "Keeping default keybord configuration."
  fi
  
  echo "Installation wget, subversion and git."
  sudo apt-get -y --force-yes install wget subversion git-core

  # Change Timezone?
  if [ "$changetz" = "y" ] || [ "$changetz" = "Y" ]; then
    tzselect
  else
    echo "Timezone will NOT be changed."
  fi

  # Install 3P tools?
  if [ "$p3resp" != "n" ] && [ "$p3resp" != "N" ]; then

    if [ "$MY_OS" = "Darwin" ]; then
      echo "Mac OS"
      /opt/matterhorn/matterhorn_trunk/docs/scripts/3rd_party_tools/mac/preinstall_mac.sh
    elif [ "$MY_OS" = "Linux" ]; then
      if [ -f /usr/bin/apt-get ]; then
        echo "Ubuntu"
        /opt/matterhorn/matterhorn_trunk/docs/scripts/3rd_party_tools/linux/preinstall_debian.sh
      else
        echo "Redhat"
        /opt/matterhorn/matterhorn_trunk/docs/scripts/3rd_party_tools/linux/preinstall_redhat.sh
      fi
    fi

  else
    echo "3rd party tools will NOT be installed."
  fi

  # Install opencaps?
  if [ "$opencaps" = "y" ] || [ "$opencaps" = "Y" ]; then
    if [ "$opencapsminimal" = "n" ] || [ "$opencapsminimal" = "N" ]; then
      /home/opencast/opencaps.sh
    else
      /home/opencast/opencaps_matterhorn_only.sh
    fi
  else
    echo "opencaps will NOT be installed."
  fi


  # doing some additional setups
  sudo update-java-alternatives -s java-6-sun
  sudo chown -R 1000:1000 /home/opencast

  # ntp server?
  if [ ${#ntpsrv} -gt 7 ]; then
    sudo sed -i "s/ntp.ubuntu.com/$ntpsrv/" /etc/ntp.conf
  else
    echo "Using default NTP server: ntp.ubuntu.com"
  fi

  # restart ntp, just to make sure that time will be synchronized
  sudo /etc/init.d/ntp restart

  start_mh

  echo "done."
fi

