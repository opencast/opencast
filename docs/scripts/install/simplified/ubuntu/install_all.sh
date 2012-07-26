#!/bin/bash
HWD=`cd "${0%/*}" 2>/dev/null; echo $PWD`
set -x
#
# Ubuntu Matterhon installation
#
MH_VER=`cat "$HWD/version" 2>/dev/null`
while [ $# -gt 0 ]; do
  case "$1" in
    -? ) echo "Usage: ${0##*/} <MH_ver>" 1>&2; exit 1 ;;
    * ) export MH_VER="$1"; shift ;;
  esac
done
[ -z "$MH_VER" ] && MH_VER=trunk
case "$MH_VER" in
  1.3.0 | 1.3.1 ) ;;
  1.3.x | 1.4.x ) ;;
  trunk ) ;;
  * ) echo "Unsupported version $MH_VER" 1>&2; exit 1 ;;
esac
echo $MH_VER > "$HWD/version"
[ $? -ne 0 ] && exit 1
#
# Install java
#
which javac
if [ $? -ne 0 ]; then
  "$HWD/install_java.sh"
  [ $? -ne 0 ] && exit 1
fi
export JAVA_HOME=`"$HWD/java_home.sh"`
[ -z "$JAVA_HOME" ] && exit 1
#
# Install maven
#
sudo apt-get -y install maven2
[ $? -ne 0 ] && exit 1
#
export M2_HOME=/usr/share/maven2
"$HWD/set_profile_var.sh" "M2_HOME" "$M2_HOME"
[ $? -ne 0 ] && exit 1
#
"$HWD/check_path.sh" "$M2_HOME/bin"
if [ $? -ne 0 ]; then
  export PATH="$PATH:$M2_HOME/bin"
  echo 'export PATH="$PATH:$M2_HOME/bin"' >> ~/.profile
  [ $? -ne 0 ] && exit 1
fi
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
sudo chown -R `id -u`:`id -g` /opt/matterhorn
[ $? -ne 0 ] && exit 1
#
case $MH_VER in
  1.3.0 | 1.3.1 )
    MH_URL=https://opencast.jira.com/svn/MH/tags/$MH_VER
    MH_DIR=release_$MH_VER
    ;;
  1.3.x | 1.4.x )
    MH_URL=https://opencast.jira.com/svn/MH/branches/$MH_VER
    MH_DIR=branch_$MH_VER
    ;;
  trunk )
    MH_URL=https://opencast.jira.com/svn/MH/trunk
    MH_DIR=matterhorn_trunk
    ;;
esac
svn co $MH_URL /opt/matterhorn/$MH_DIR
[ $? -ne 0 ] && exit 1
rm -fr /opt/matterhorn/trunk
[ $? -ne 0 ] && exit 1
ln -s $MH_DIR /opt/matterhorn/trunk
[ $? -ne 0 ] && exit 1
#
# From now on always refer to trunk!
#
# Install & configure felix
#
case $MH_VER in
  1.3.* )
    "$HWD/install_felix.sh"
    [ $? -ne 0 ] && exit 1
    cp -rf /opt/matterhorn/trunk/docs/felix/* /opt/matterhorn/felix
    [ $? -ne 0 ] && exit 1
    cp /opt/matterhorn/felix/conf/config.properties /opt/matterhorn/felix/conf/config.properties.org
    [ $? -ne 0 ] && exit 1
    ;;
  1.4.* )
    cp /opt/matterhorn/trunk/etc/config.properties /opt/matterhorn/trunk/etc/config.properties.org
    [ $? -ne 0 ] && exit 1
    ;;
  trunk )
    cp /opt/matterhorn/trunk/etc/config.properties /opt/matterhorn/trunk/etc/config.properties.org
    [ $? -ne 0 ] && exit 1
    ;;
esac
"$HWD/customize_conf.sh" $MH_VER
[ $? -ne 0 ] && exit 1
#
# Install 3rd party tools
#
cd /opt/matterhorn/trunk/docs/scripts/3rd_party
[ $? -ne 0 ] && exit 1
time ./do-all 2>&1 | tee do-all.log
[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
cd -
[ $? -ne 0 ] && exit 1
#
# Install gstreamer for demo capture agent
#
sudo apt-get -y install gstreamer0.10-plugins-base
[ $? -ne 0 ] && exit 1
sudo apt-get -y install gstreamer0.10-plugins-good
[ $? -ne 0 ] && exit 1
#
# Compile matterhorn
#
if [ -n "$M2_REPO" ]; then
  if [ ! -d "$M2_REPO" ]; then
    mkdir -p "$M2_REPO"
    [ $? -ne 0 ] && exit 1
  fi
  cd "$M2_REPO"
  [ $? -ne 0 ] && exit 1
else
  if [ ! -d ~/.m2/repository ]; then
    mkdir -p ~/.m2/repository
    [ $? -ne 0 ] && exit 1
  fi
  cd ~/.m2/repository
  [ $? -ne 0 ] && exit 1
fi
tar -zxvf "$HWD/jersey.tar.gz"
[ $? -ne 0 ] && exit 1
cd -
[ $? -ne 0 ] && exit 1
#
time "$HWD/mh_clean_install.sh" -c -i -e $MH_VER 2>&1 | tee mh_clean_install.log
[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
#
# Run matterhorn
#
#"$HWD/mh_run.sh" -c $MH_VER 2>&1 | tee mh_run.log
#[ ${PIPESTATUS[0]} -ne 0 -o ${PIPESTATUS[1]} -ne 0 ] && exit 1
#
exit 0
