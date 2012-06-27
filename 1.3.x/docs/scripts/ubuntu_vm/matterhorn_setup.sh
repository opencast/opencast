#!/bin/bash

# Initialization
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
**    Home:    /opt/matterhorn/felix
**    Bundles: /opt/matterhorn/felix/matterhorn
**    Config:  /opt/matterhorn/felix/conf
**
** For further information, please visit
**   http://www.opencastproject.org
********************************************

For a complete list of 3rd party tools, please visit:
  http://opencast.jira.com/wiki/display/MH/3rd+Party+Licenses+and+Software

** Install process may take 40-60 minutes, depending on computer configuration and network speed.

** PLEASE NOTE: You may be prompted for the login password to authorize the installation script.
END
}

start_mh ()
{
  echo "Starting Matterhorn..."

  FELIX=felix
  FELIX_DIR=/opt/matterhorn/$FELIX

  cd
  sudo update-rc.d matterhorn defaults
  sudo service matterhorn start

  echo "" | sudo tee -a $MOTD_FILE
  echo "********************************************" | sudo tee -a $MOTD_FILE
  echo "** Matterhorn console is at http://$MY_IP:8080" | sudo tee -a $MOTD_FILE
  echo "**" | sudo tee -a $MOTD_FILE
  echo "** Matterhorn is installed in:" | sudo tee -a $MOTD_FILE
  echo "**    Home:    /opt/matterhorn/felix" | sudo tee -a $MOTD_FILE
  echo "**    Bundles: /opt/matterhorn/felix/matterhorn" | sudo tee -a $MOTD_FILE
  echo "**    Config:  /opt/matterhorn/felix/conf" | sudo tee -a $MOTD_FILE
  echo "********************************************" | sudo tee -a $MOTD_FILE

}

############################### START HERE ###############################
# Turn off screensaver
setterm -blank 0 

MY_OS=`uname`

# Wait for network connection
for ntime in 1 2 3 4 5 6 7 8 9 10
do
  MY_IP=`sudo ifconfig | grep "inet addr:" | grep -v 127.0.0.1 | awk '{print $2}' | cut -d':' -f2`
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
  echo "lines, and restart the VM."
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

  # Reconfigure Keyboard?
  if [ "$kbresp" = "y" ] || [ "$kbresp" = "Y" ]; then
    sudo dpkg-reconfigure console-setup
  else
    echo "Keeping default keybord configuration."
  fi

  # Need to get a server name, not just y/n
  ntpsrv=y
  while [ ${#ntpsrv} -gt 0 ] && [ ${#ntpsrv} -lt 8 ]
  do
    echo "**** To set up a different NTP server, please enter the URL or press enter [ntp.ubuntu.com]?"
    read ntpsrv
  done

  echo "**** Do you want to change the timezone on this VM? [y/N]"
  read changetz

  # Change Timezone?
  if [ "$changetz" = "y" ] || [ "$changetz" = "Y" ]; then
    sudo tzselect
  else
    echo "Timezone will NOT be changed."
  fi

  echo "**** Do you want to install 3rd party tools? [Y/n]"
  read p3resp

  # update felix config (url)
  sed -i "s/http:\/\/localhost:8080/http:\/\/$MY_IP:8080/" $CONF_DIR/config.properties

  # Install 3P tools?
  if [ "$p3resp" != "n" ] && [ "$p3resp" != "N" ]; then
    #cd to script directory, otherwise the 3rd party script fails...
    cd /opt/matterhorn/matterhorn_source/docs/scripts/3rd_party/
    ./do-all
    if [ $? -ne 0 ]; then
      echo "===================================================="
      echo "3rd party tools failed to install correctly"
      echo "These tools are required for the VM to function"
      echo "Please check to make sure the VM has internet access"
      echo " and rerun matterhorn_setup.sh"
      echo "===================================================="
      exit 1 
    fi
    #cd to the home directory
    cd 
  else
    echo "3rd party tools will NOT be installed."
  fi

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
  rm -f `readlink -f $0`
  exit 0
fi

