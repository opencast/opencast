#!/bin/bash
set -x
#
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
#
# Ubuntu Matterhon installation
#
# Install java
#
./install_java.sh
[ $? -ne 0 ] && exit 1
export JAVA_HOME=`./java_home.sh`
[ -z "$JAVA_HOME" ] && exit 1
#
# Install maven
#
sudo apt-get -y install maven2
[ $? -ne 0 ] && exit 1
#
export M2_HOME=/usr/share/maven2
./set_profile_var.sh "M2_HOME" "$M2_HOME"
[ $? -ne 0 ] && exit 1
export PATH=$PATH:${M2_HOME}/bin
echo 'export PATH=$PATH:${M2_HOME}/bin' >> ~/.profile
[ $? -ne 0 ] && exit 1
#
# Install subversion
#
sudo apt-get -y install subversion
[ $? -ne 0 ] && exit 1
#
# Install matterhorn sources
#
sudo mkdir -p /opt/matterhorn
[ $? -ne 0 ] && exit 1
sudo chown `id -u`:`id -g` /opt/matterhorn
[ $? -ne 0 ] && exit 1
#
svn co http://opencast.jira.com/svn/MH/tags/1.2.0 /opt/matterhorn/1.2.0
[ $? -ne 0 ] && exit 1
rm -fr /opt/matterhorn/trunk
[ $? -ne 0 ] && exit 1
ln -s /opt/matterhorn/1.2.0 /opt/matterhorn/trunk
[ $? -ne 0 ] && exit 1
#
# Install felix
#
./get_felix.sh
[ $? -ne 0 ] && exit 1
tar -zxvf felix-3.0.9.tar.gz
[ $? -ne 0 ] && exit 1
rm -fr /opt/matterhorn/felix
[ $? -ne 0 ] && exit 1
mv felix-framework-3.0.9 /opt/matterhorn/felix
[ $? -ne 0 ] && exit 1
#
mkdir -p /opt/matterhorn/felix/load
[ $? -ne 0 ] && exit 1
cp -rf /opt/matterhorn/1.2.0/docs/felix/* /opt/matterhorn/felix
[ $? -ne 0 ] && exit 1
cp /opt/matterhorn/felix/conf/config.properties /opt/matterhorn/felix/conf/config.properties.org
[ $? -ne 0 ] && exit 1
./customize_conf.sh
[ $? -ne 0 ] && exit 1
#
# Install 3rd party tools
#
if [ ! -d /var/opencast ]; then 
  sudo mkdir -p /var/opencast
  [ $? -ne 0 ] && exit 1
  sudo chown `id -u`:`id -g` /var/opencast
  [ $? -ne 0 ] && exit 1
  chmod 1777 /var/opencast
  [ $? -ne 0 ] && exit 1
fi
#
cd /opt/matterhorn/1.2.0/docs/scripts/3rd_party
[ $? -ne 0 ] && exit 1
time ./do-all 2>&1 | tee /var/opencast/do-all.log
[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
#
# Compile matterhorn
#
cd "$HWD"
[ $? -ne 0 ] && exit 1
time ./mh_clean_install.sh -c -i -e 2>&1 | tee /var/opencast/mh_clean_install.log
[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
#
# Run matterhorn
#
#./mh_run.sh -c 2>&1 | tee /var/opencast/mh_run.log
#[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
#
exit 0
