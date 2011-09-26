#!/bin/sh
# To set variables before calling this script just include them directly on the command line.
# e.g. to override the mirror to use, you could do something like:
#    UBUNTU_MIRROR=http://aifile.usask.ca/apt-mirror/mirror/archive.ubuntu.com/ubuntu/ ./buildvm.sh

HOME=`pwd`
MATTERHORN_SVN="http://opencast.jira.com/svn/MH/trunk/"
#check for existance of mirror URL
if [ "$UBUNTU_MIRROR" = "" ];
  then
	UBUNTU_MIRROR=http://ubuntu.mirrors.tds.net/ubuntu/
fi
echo "Using ubuntu mirror at: $UBUNTU_MIRROR"

USERNAME="opencast"
PASSWORD="matterhorn"

export M2=`pwd`/m2/
export JAVA_HOME=`mvn --version | grep "Java home" | awk '{print $3}'`

#todo: overwrite parameters with passed in values
#for each param in $# evaluate $i as an item

#install extras that we need if running this script
sudo apt-get -y install ubuntu-vm-builder subversion zip git-core maven2

#check to see if the vmware disk mounting tool is there
if which vmware-mount >/dev/null; then
	echo "VMware Mounter is installed."
else
	echo "VMware Mounter is not installed!"
	exit
fi

#delete the old vm if it exists
if [ -z "$(mount | grep `pwd`/mnt)" ];
 then
	echo "Nothing mounted"
else
	sudo vmware-mount -d mnt
	sleep 2
fi


if [ "$1" = "clean" ]; then 
  echo "==============================="
  echo "====Cleaning up for release===="
  echo "==============================="
  sudo rm -rf vmbackup/
  sudo rm -rf m2/
  sudo rm -rf matterhorn_trunk/
  sudo rm -rf red5-*
  sudo rm -rf matterhorn-engage-streaming/
  sudo rm -rf felix-framework*
fi
sudo rm -rf ubuntu-vmw6/
sudo rm -rf mnt

echo "=========================="
echo "========Building VM======="
echo "========Please Wait======="
echo "=========================="

#set the mirror that the vm should be using to download sources, making sure multiverse is in there for aac/etc
echo "deb $UBUNTU_MIRROR karmic main restricted universe multiverse" > sources.list
echo "deb $UBUNTU_MIRROR karmic-updates main restricted universe multiverse" >> sources.list
echo "deb http://archive.canonical.com/ubuntu karmic partner" >> sources.list
echo "deb http://security.ubuntu.com/ubuntu karmic-security main restricted universe multiverse" >> sources.list

if [ ! -e vmbackup ]; then
  #build the ubuntu vm
  sudo ubuntu-vm-builder vmw6 karmic --arch 'i386' --mem '512' --cpus 1 \
  --rootsize '12288' --swapsize '1024' --kernel-flavour='virtual' \
  --hostname 'opencast' --mirror $UBUNTU_MIRROR \
  --components 'main,universe,multiverse' \
  --name 'opencast' --user $USERNAME \
  --pass $PASSWORD --tmpfs - --addpkg zlib1g-dev --addpkg patch \
  --addpkg byacc --addpkg libcv1 --addpkg libcv-dev --addpkg opencv-doc \
  --addpkg build-essential --addpkg locate --addpkg git-core \
  --addpkg checkinstall --addpkg yasm --addpkg texi2html  --addpkg libsdl1.2-dev \
  --addpkg libtheora-dev --addpkg libx11-dev \
  --addpkg zlib1g-dev --addpkg libpng12-dev --addpkg libjpeg62-dev \
  --addpkg libtiff4-dev --addpkg ssh --addpkg maven2 --addpkg subversion \
  --addpkg wget --addpkg curl --addpkg update-motd --addpkg ntp \
  --addpkg expect-dev --addpkg expect --addpkg vim --addpkg nano \
  --addpkg gstreamer0.10-plugins* --addpkg gstreamer0.10-ffmpeg --addpkg gstreamer-tools \
  --addpkg acpid --exec $HOME/postinstall.sh

  echo "change the vm to use nat networking instead of bridged"
  sed -i 's/bridged/nat/g' ubuntu-vmw6/opencast.vmx
  cp -r ubuntu-vmw6 vmbackup
else 
  #restore from copy
  cp -r vmbackup ubuntu-vmw6
fi

echo "change matterhorn_setup.sh to use the correct svn repo path"
sed -i "s'OC_URL=.*$'OC_URL=$MATTERHORN_SVN'" matterhorn_setup.sh

#mount the vm image
mkdir mnt
sudo vmware-mount ubuntu-vmw6/disk0.vmdk 1 mnt
if [ $? -ne 0 ]
 then
	echo "Unable to mount drive, fatal error!"
	sudo vmware-mount -d mnt
	exit
else
	echo "Drive mounted."
fi

echo "=========================="
echo "==Copying Setup Scripts==="
echo "=========================="


#set the mirror that the vm should be using to download sources, making sure multiverse is in there for aac/etc
#echo "deb $UBUNTU_MIRROR karmic main restricted universe multiverse" >> sources.list
#echo "deb $UBUNTU_MIRROR karmic-updates main restricted universe multiverse" >> sources.list
#echo "deb http://archive.canonical.com/ubuntu karmic partner" >> sources.list
#echo "deb http://security.ubuntu.com/ubuntu karmic-security main restricted universe multiverse" >> sources.list

#copy sources list into the vm image
#sudo mv sources.list mnt/etc/apt/sources.list

#copy config scripts into vm
sudo cp matterhorn_setup.sh mnt/etc/profile.d/matterhorn_setup.sh
sudo chmod 755 mnt/etc/profile.d/matterhorn_setup.sh
sudo cp rc.local mnt/etc/rc.local
sudo chmod 755 mnt/etc/rc.local
sudo cp opencaps.sh mnt/home/opencast/opencaps.sh
sudo chmod 755 mnt/home/opencast/opencaps.sh
sudo cp opencaps.sh mnt/home/opencast/opencaps_matterhorn_only.sh
sudo chmod 755 mnt/home/opencast/opencaps_matterhorn_only.sh

sudo ln -s /opt/matterhorn/felix/bin/start_matterhorn.sh mnt/home/opencast/startup.sh
sudo ln -s /opt/matterhorn/felix/bin/shutdown_matterhorn.sh mnt/home/opencast/shutdown.sh
sudo ln -s /opt/matterhorn/felix/bin/update_matterhorn.sh mnt/home/opencast/update_matterhorn.sh
sudo ln -s /opt/matterhorn/felix/bin/matterhorn_init_d.sh mnt/etc/init.d/matterhorn

sudo mkdir mnt/opt/matterhorn

echo "============================"
echo "==Installing Apache Felix==="
echo "============================"

if [ ! -e felix-framework-2.0.1 ]; then
  # This one failed ... wget http://apache.linux-mirror.org/felix/felix-framework-2.0.1.tar.gz
  wget http://archive.apache.org/dist/felix/felix-framework-2.0.1.tar.gz
  tar -xzf felix-framework-2.0.1.tar.gz
fi 

#copy felix files to vm
sudo cp -r felix-framework-2.0.1 mnt/opt/matterhorn/felix
#create needed dirs
sudo mkdir mnt/opt/matterhorn/felix/load
sudo chown -R 1000:1000 mnt/opt/matterhorn/felix/
sudo chmod -R 777 mnt/opt/matterhorn/felix/

echo "===================="
echo "==Installing Red5==="
echo "===================="

if [ ! -e red5-0.9.1 ]; then
wget http://www.red5.org/downloads/0_9/red5-0.9.1.tar.gz
tar -xzf red5-0.9.1.tar.gz
fi

sudo cp -r red5-0.9.1 mnt/opt/matterhorn/red5
sudo cp red5 mnt/etc/init.d/
sudo ln -s /etc/init.d/red5 mnt/etc/rc0.d/S87red5
sudo ln -s /etc/init.d/red5 mnt/etc/rc1.d/S87red5
sudo ln -s /etc/init.d/red5 mnt/etc/rc2.d/S87red5
sudo ln -s /etc/init.d/red5 mnt/etc/rc3.d/S87red5
sudo ln -s /etc/init.d/red5 mnt/etc/rc4.d/S87red5
sudo ln -s /etc/init.d/red5 mnt/etc/rc5.d/S87red5

echo "=========================="/
echo "=====Fetching Opencast===="
echo "=========================="

#temporary fix for weird svn up issue, checkout instead
#check out svn
if [ -e matterhorn_trunk ]; then
  cd matterhorn_trunk
  svn up
  cd ..
else
  svn co $MATTERHORN_SVN matterhorn_trunk
fi

sudo cp -r matterhorn_trunk mnt/opt/matterhorn/

sudo cp -rf matterhorn_trunk/docs/felix/* mnt/opt/matterhorn/felix/
sudo sed -i "s/\$MATTERHORN_HOME/\/opt\/matterhorn/g" mnt/opt/matterhorn/felix/bin/matterhorn_init_d.sh
sudo sed -i "s/\$FELIX_HOME/\$MATTERHORN\/felix\//g" mnt/opt/matterhorn/felix/bin/matterhorn_init_d.sh
sudo sed -i "s/\$USERNAME/$USERNAME/g" mnt/opt/matterhorn/felix/bin/matterhorn_init_d.sh
sudo sed -i "s/conf\/security.xml/\/opt\/matterhorn\/felix\/conf\/security.xml/" mnt/opt/matterhorn/felix/conf/config.properties

export OC_REV=`svn info matterhorn_trunk | awk /Revision/ | cut -d " " -f 2`

echo "=========================="
echo "=====Building Opencast===="
echo "=========================="

#get maven to update whatever dependancies we might have for opencast
pwd
cd matterhorn_trunk
export MAVEN_OPTS='-Xms256m -Xmx960m -XX:PermSize=64m -XX:MaxPermSize=150m'
mvn install -fn -DskipTests -Dmaven.repo.local=$M2/repository -DdeployTo=$HOME/mnt/opt/matterhorn/felix/load
cd ..

#copy the maven repo across
sudo cp -r $M2 mnt/home/opencast/.m2

echo "===================================="
echo "=====Building red5 streaming app===="
echo "===================================="


export RED5_HOME="`pwd`/red5-0.9.1"
if [ -e matterhorn-engage-streaming ]; then
  cd matterhorn-engage-streaming
  svn up
  cd ..
else
  svn checkout http://opencast.jira.com/svn/MH/contrib/matterhorn-engage-streaming
fi

cd matterhorn-engage-streaming
ant
sudo cp dist/*.war ../mnt/opt/matterhorn/red5/webapps/
sudo chown 1000:1000 ../mnt/opt/matterhorn/red5/webapps/*.war
cd ..

echo "=========================="
echo "========Final Setup======="
echo "=========================="

# copy mediainfo 0.7.19

sudo cp mediainfo mnt/usr/local/bin/
sudo cp libmediainfo.a mnt/usr/local/lib/
sudo cp libmediainfo.la mnt/usr/local/lib/

#create directory for log files
sudo mkdir mnt/opt/matterhorn/log
sudo chown -R 1000:1000 mnt/opt/matterhorn/log

#create directory for capture agent
sudo mkdir mnt/opencast
sudo chown -R 1000:1000 mnt/opencast
sudo chmod 777 mnt/opencast

#give opencast user rights for /opt/matterhorn
sudo chown -R 1000:1000 mnt/opt/matterhorn
sudo chmod -R 777 mnt/opt/matterhorn


#write environment variables to login file
echo "export OC=/opt/matterhorn" >> mnt/home/opencast/.bashrc
echo "export FELIX_HOME=/opt/matterhorn/felix" >> mnt/home/opencast/.bashrc
echo "export RED5_HOME=/opt/matterhorn/red5" >> mnt/home/opencast/.bashrc
echo "export M2_REPO=/home/opencast/.m2/repository" >> mnt/home/opencast/.bashrc
echo "export OC_URL=http://opencast.jira.com/svn/MH/trunk/" >> mnt/home/opencast/.bashrc
echo "export FELIX_URL=http://apache.mirror.iweb.ca/felix/felix-framework-2.0.1.tar.gz" >> mnt/home/opencast/.bashrc
echo "export JAVA_HOME=/usr/lib/jvm/java-6-sun" >> mnt/home/opencast/.bashrc
echo "export MAVEN_OPTS=\"-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m\"" >> mnt/home/opencast/.bashrc

#lets set opencast to own her files
sudo chown -R 1000:1000 mnt/home/opencast/*
sudo chmod -R 777 mnt/home/opencast/*
sudo chmod -R 777 mnt/home/opencast/.m2/

#unmount the vm disk image and cleanup
sudo vmware-mount -d mnt
sleep 2
sudo rm -rf mnt

echo "================================="
echo "=====Image Built, compressing===="
echo "================================="

#archive it all for download
echo "Building archive opencast-$OC_REV.zip."
sed -i "s/ide0:0.fileName =.*/ide0:0.fileName = \"disk0.vmdk\"/" ubuntu-vmw6/opencast.vmx
mv ubuntu-vmw6 opencast-$OC_REV
zip -db -r -9 opencast-$OC_REV.zip opencast-$OC_REV
rm -rf opencast-$OC_REV

#copy it to the web
#scp opencast-$OC_REV.zip cab938@aries:/var/www/opencast/unofficial-vms/

