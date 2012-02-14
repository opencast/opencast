#!/bin/bash
set -x
#
MH_VER=1.3.0
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
#
# CentOS Matterhorn installation
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
wget -t 5 -4 http://mirror.cc.columbia.edu/pub/software/apache/maven/binaries/apache-maven-2.2.1-bin.tar.gz -O apache-maven-2.2.1-bin.tar.gz
[ $? -ne 0 ] && exit 1
tar -zxvf apache-maven-2.2.1-bin.tar.gz
[ $? -ne 0 ] && exit 1
sudo rm -fr /usr/share/apache-maven-2.2.1
[ $? -ne 0 ] && exit 1
sudo mv apache-maven-2.2.1 /usr/share
[ $? -ne 0 ] && exit 1
sudo rm -fr /usr/share/maven2
[ $? -ne 0 ] && exit 1
sudo ln -s /usr/share/apache-maven-2.2.1 /usr/share/maven2
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
case `./os_ver.sh` in
  5.* )
    URL=http://apt.sw.be/redhat/el5/en/i386/extras/RPMS
    PKG=subversion-1.6.15-0.1.el5.rfx.i386.rpm
    ;;
  6.* )
    URL=http://apt.sw.be/redhat/el6/en/i386/extras/RPMS
    PKG=subversion-1.6.15-0.1.el6.rfx.i686.rpm
    sudo yum -y install perl-CGI
    [ $? -ne 0 ] && exit 1
    ;;
  * )
    echo "Unsupported CentOS version: `./os_ver.sh`" 1>&2
    exit 1 ;;
esac
wget -t 5 -4 $URL/$PKG -O $PKG
[ $? -ne 0 ] && exit 1
sudo rpm -ivh --force --nosignature $PKG
[ $? -ne 0 ] && exit 1
#
# Install matterhorn sources
#
sudo mkdir -p /opt/matterhorn
[ $? -ne 0 ] && exit 1
sudo chown `id -u`:`id -g` /opt/matterhorn
[ $? -ne 0 ] && exit 1
#
#svn co http://opencast.jira.com/svn/MH/tags/$MH_VER /opt/matterhorn/$MH_VER
svn co http://opencast.jira.com/svn/MH/branches/1.3.x /opt/matterhorn/$MH_VER
[ $? -ne 0 ] && exit 1
rm -fr /opt/matterhorn/trunk
[ $? -ne 0 ] && exit 1
ln -s /opt/matterhorn/$MH_VER /opt/matterhorn/trunk
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
cp -rf /opt/matterhorn/${MH_VER}/docs/felix/* /opt/matterhorn/felix
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
cd /opt/matterhorn/${MH_VER}/docs/scripts/3rd_party
[ $? -ne 0 ] && exit 1
time ./do-all 2>&1 | tee /var/opencast/do-all.log
[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
#
# Install gstreamer for demo capture agent
#
sudo yum -y install gstreamer-plugins-base
[ $? -ne 0 ] && exit 1
sudo yum -y install gstreamer-plugins-good
[ $? -ne 0 ] && exit 1
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
