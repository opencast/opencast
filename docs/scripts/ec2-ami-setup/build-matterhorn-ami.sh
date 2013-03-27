#!/bin/bash
# this script builds a functional matterhorn instance as an Amazon EC2 AMI
# the starting point needs to be a running instance of a matterhorn-util AMI
# that has the necessary prerequisites already installed, e.g. ec2 ami-d7663792
# aka 990500589184/matterhorn-util-new
# prereq's include subversion zip git-core maven2 ruby and an opencast user 
# in the sudoers list.
# the script build-matterhorn-util-ami.sh to build a 
# matterhorn-util remains to be written.


#create directory for log filesmkdir 
sudo mkdir -v /opt/matterhorn /opt/matterhorn/log

#give opencast user rights for /opt/matterhorn
sudo chown -R opencast:ubuntu /opt/matterhorn
sudo chmod -R 777 /opt/matterhorn


#create directory for capture agent
sudo mkdir /opencast
sudo chown -R opencast:ubuntu /opencast
sudo chmod 777 /opencast

echo "installing sun-java6-jdk"
sudo apt-get install sun-java6-jdk 
sudo update-java-alternatives -s java-6-sun

#write environment variables to login file
echo "export OC=/opt/matterhorn" >> /home/opencast/.bashrc
echo "export FELIX_HOME=/opt/matterhorn/felix" >> /home/opencast/.bashrc
echo "export MATTERHORN_SRC_HOME=/home/opencast/matterhorn_trunk" >> /home/opencast/.bashrc
echo "export M2_REPO=/home/opencast/.m2/repository" >> /home/opencast/.bashrc
echo "export OC_URL=https://opencast.jira.com/svn/MH/trunk/" >> /home/opencast/.bashrc
echo "export FELIX_URL=http://apache.mirror.iweb.ca/felix/felix-framework-2.0.1.tar.gz" >> /home/opencast/.bashrc
echo "export JAVA_HOME=/usr/lib/jvm/java-6-sun" >> /home/opencast/.bashrc
echo "export MAVEN_OPTS=\"-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m\"" >> /home/opencast/.bashrc

echo "checking out source tree into " "$MATTERHORN_SRC_HOME"
mkdir $MATTERHORN_SRC_HOME
svn co https://opencast.jira.com/svn/MH/trunk $MATTERHORN_SRC_HOME

#install everything necessary to build third party tools
sudo $MATTERHORN_SRC_HOME/docs/scripts/3rd_party_tools/linux/preinstall_debian.sh 2>&1

# do we want to use these on the ec2 instances or $FELIX_HOME/bin/start_matterhorn.sh ?
sudo cp -v $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/matterhorn_setup.sh /etc/profile.d/matterhorn_setup.sh
sudo chmod 755 /etc/profile.d/matterhorn_setup.sh
sudo cp -v $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/startup.sh /home/opencast/startup.sh
sudo chmod 755 /home/opencast/startup.sh
sudo cp -v $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/shutdown.sh /home/opencast/shutdown.sh
sudo chmod 755 /home/opencast/shutdown.sh
sudo cp -v $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/update-matterhorn.sh /home/opencast/update-matterhorn.sh
sudo chmod 755 /home/opencast/update-matterhorn.sh
# do we want to start server at startup?
#sudo cp -v $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/rc.local /etc/rc.local
#sudo chmod 755 /etc/rc.local

cd ~

sudo apt-get --install-recommends install libcv1  libcv-dev  opencv-doc  build-essential  locate  \
 checkinstall  yasm  texi2html   libsdl1.2-dev libtheora-dev libx11-dev  \
 ssh  update-motd expect-dev  expect  cpid
 
if [ ! -e felix-framework-2.0.1 ]; then
  # This one failed ... wget http://apache.linux-mirror.org/felix/felix-framework-2.0.1.tar.gz
  wget http://archive.apache.org/dist/felix/felix-framework-2.0.1.tar.gz
  tar -xzf felix-framework-2.0.1.tar.gz
fi 

sudo cp $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/mediainfo /usr/local/bin/
sudo cp $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/libmediainfo.a /usr/local/lib/
sudo cp $MATTERHORN_SRC_HOME/docs/scripts/ubuntu_vm/libmediainfo.la /usr/local/lib/
 
cd $MATTERHORN_SRC_HOME
export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"
mvn install -DskipTests -DdeployTo=/opt/matterhorn/felix/load

rsync -avC $MATTERHORN_SRC_HOME/docs/felix/conf/* $FELIX_HOME/conf/

$MATTERHORN_SRC_HOME/docs/scripts/ec2-ami-setup/set-runtime-config $@

cd ~
