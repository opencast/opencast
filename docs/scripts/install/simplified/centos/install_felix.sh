#!/bin/bash
set -x
#
# Install felix
#
FX_VER=3.0.9
#FX_VER=3.2.2
#
URL="http://repo2.maven.org/maven2/org/apache/felix/org.apache.felix.main.distribution/$FX_VER/org.apache.felix.main.distribution-${FX_VER}.tar.gz"
PKG="felix-${FX_VER}.tar.gz"
#
if [ ! -s "$PKG" ]; then
  AGENT="Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0)"
  wget -t 5 -4 --retry-connrefused --progress=dot:mega --no-cache -U "$AGENT" "$URL" -O "$PKG"
  [ $? -ne 0 ] && exit 1
fi
#
tar -zxvf "$PKG"
[ $? -ne 0 ] && exit 1
sudo chown -R `id -u`:`id -g` "felix-framework-$FX_VER"
[ $? -ne 0 ] && exit 1
rm -fr "/opt/matterhorn/felix-framework-$FX_VER"
[ $? -ne 0 ] && exit 1
mv "felix-framework-$FX_VER" /opt/matterhorn
[ $? -ne 0 ] && exit 1
rm -fr /opt/matterhorn/felix
[ $? -ne 0 ] && exit 1
ln -s "felix-framework-$FX_VER" /opt/matterhorn/felix
[ $? -ne 0 ] && exit 1
#
echo "Felix version $FX_VER installed on /opt/matterhon/felix"
exit 0
