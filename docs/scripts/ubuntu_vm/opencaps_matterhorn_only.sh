#!/bin/sh

# apache, php5, mysql5
echo "============================"
echo "==Installing Apache, PHP=="
echo "============================"
sudo apt-get -y update
sudo apt-get -y install apache2 php5 curl libcurl3 libcurl3-dev php5-curl
sudo /etc/init.d/apache2 restart

# opencaps
echo "============================"
echo "==Installing OpenCaps=="
echo "============================"                   
cd /var/www/
sudo wget http://opencaps.atrc.utoronto.ca/releases/opencaps.tar.gz
sudo tar -xvzf opencaps.tar.gz

cd opencaps
sudo cp install/config_template.php include/config.inc.php
sudo chmod 777 conversion_service/imported
sudo chmod 777 projects

# disable local access in config
sed -i "s/\'DISABLE_LOCAL\',	\tfalse/\'DISABLE_LOCAL\',\ttrue/g" include/config.inc.php

# add matterhorn info to config
MY_IP=`ifconfig | grep "inet addr:" | grep -v 127.0.0.1 | awk '{print $2}' | cut -d':' -f2`
sed -i 's/?>/\$remote_systems[0][url]="http:\/\/'${MY_IP}':8080";\n?>/' include/config.inc.php
sed -i 's/?>/\$remote_systems[0][name]="Matterhorn";\n?>/' include/config.inc.php

echo "**** OpenCaps is installed and available at $MY_IP/opencaps"