#!/bin/sh

# apache, php5, mysql5
echo "============================"
echo "==Installing Apache, PHP, and MySql=="
echo "============================"
sudo apt-get -y update
sudo apt-get -y install apache2 php5 curl libcurl3 libcurl3-dev php5-curl mysql-server php5-mysql #user enters mysql password for root
sudo /etc/init.d/apache2 restart

# opencaps
echo "============================"
echo "==Installing OpenCaps=="
echo "============================"
echo "Creating database - please enter your mysql root password"
mysqladmin create opencaps -u root -p					
cd /var/www/
sudo wget http://opencaps.atrc.utoronto.ca/releases/opencaps.tar.gz
sudo tar -xvzf opencaps.tar.gz

cd opencaps
sudo cp install/config_template.php include/config.inc.php
sudo chmod 777 conversion_service/imported
sudo chmod 777 projects
echo "Installing database tables - please enter your mysql root password"
sudo mysql opencaps -u root -p  < install/oc_schema.sql		
sudo /etc/init.d/mysql start

# edit opencaps config

# add mysql info
echo "**** Please enter your mysql root password for the OpenCaps configuration file"
currentstate=`stty -g`
stty -echo
read pass
stty $currentstate											
sed -i "s/password/$pass/" include/config.inc.php

# add matterhorn info
MY_IP=`ifconfig | grep "inet addr:" | grep -v 127.0.0.1 | awk '{print $2}' | cut -d':' -f2`
sed -i 's/?>/\$remote_systems[0][url]="http:\/\/'${MY_IP}':8080";\n?>/' include/config.inc.php
sed -i 's/?>/\$remote_systems[0][name]="Matterhorn";\n?>/' include/config.inc.php

echo "**** OpenCaps is installed and available at $MY_IP/opencaps"
