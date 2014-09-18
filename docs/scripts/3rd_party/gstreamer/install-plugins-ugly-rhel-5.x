#!/bin/bash
echo "------------------------------------------------------------------------"
echo `cd "${0%/*}" 2>/dev/null; echo $PWD/${0##*/}`
echo "------------------------------------------------------------------------"
set -x
source "${HOME3P}/utilx"
[ $? -ne 0 ] && exit 1
#
install_rpm() {
  name=`echo "$1" | awk '{sub("-[0-9.]+.*", "", $1); print $1; exit 0}'`
  name_ver=`echo "$1" | awk '{
    sub("\\\.(i386|i686|x86_64|noarch)\\\.rpm$", "", $1); print $1; exit 0}'`
  installed=`rpm -q --last "$name" | awk '{
    sub("\\\.(i386|i686|x86_64|noarch)$", "", $1); print $1; exit 0}'`
  if [ ! "$installed" = "$name_ver" ]; then
    # Convert file names in "$@" to full path
    IFS='
'
    rpms=(`fullpath "$@"`)
    unset IFS
    sudox rpm -iv --nosignature "${rpms[@]}"
    [ $? -ne 0 ] && return 1
  fi
  return 0
}
#
case `os_ver` in
  5.* )
    ;;
  * )
    echo "Unsupported version of CentOS/RHEL: `os_ver`" 1>&2
    exit 1
    ;;
esac
#
# List of rpms and their dependencies
#
url_i386[0]="ftp://ftp.ntua.gr/pub/linux/rpmfusion/free/el/updates/5/i386 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[0]=a52dec-0.7.4-13.el5.i386.rpm
url_x86_64[0]="ftp://ftp.ntua.gr/pub/linux/rpmfusion/free/el/updates/5/x86_64 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[0]=a52dec-0.7.4-13.el5.x86_64.rpm
#
url_i386[1]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/i386/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[1]=amrnb-7.0.0.2-1.el5.rf.i386.rpm
url_x86_64[1]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/x86_64/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[1]=amrnb-7.0.0.2-1.el5.rf.x86_64.rpm
#
url_i386[2]="ftp://mirror.switch.ch/pool/1/mirror/epel/5/i386 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[2]=libcdio-0.78.2-6.el5.i386.rpm
url_x86_64[2]="ftp://mirror.switch.ch/pool/1/mirror/epel/5/x86_64 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[2]=libcdio-0.78.2-6.el5.x86_64.rpm
#
url_i386[3]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/i386/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[3]=libdvdcss-1.2.10-1.el5.rf.i386.rpm
url_x86_64[3]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/x86_64/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[3]=libdvdcss-1.2.10-1.el5.rf.x86_64.rpm
#
#url_i386[4]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/i386/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
#pkg_i386[4]=libdvdread-0.9.7-1.el5.rf.i386.rpm
#url_x86_64[4]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/x86_64/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
#pkg_x86_64[4]=libdvdread-0.9.7-1.el5.rf.x86_64.rpm
#
url_i386[4]=repository
pkg_i386[4]=libdvdread-old-0.9.7-1.el5.rf.i386.rpm
url_x86_64[4]=repository
pkg_x86_64[4]=libdvdread-old-0.9.7-1.el5.rf.x86_64.rpm
#
url_i386[5]="ftp://mirror.switch.ch/pool/1/mirror/epel/5/i386 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[5]=libid3tag-0.15.1b-5.el5.i386.rpm
url_x86_64[5]="ftp://mirror.switch.ch/pool/1/mirror/epel/5/x86_64 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[5]=libid3tag-0.15.1b-5.el5.x86_64.rpm
#
url_i386[6]="ftp://ftp.ntua.gr/pub/linux/rpmfusion/free/el/updates/5/i386 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[6]=libmad-0.15.1b-8.el5.i386.rpm
url_x86_64[6]="ftp://ftp.ntua.gr/pub/linux/rpmfusion/free/el/updates/5/x86_64 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[6]=libmad-0.15.1b-8.el5.x86_64.rpm
#
url_i386[7]="ftp://ftp.ntua.gr/pub/linux/rpmfusion/free/el/updates/5/i386 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[7]=libmpeg2-0.5.1-3.el5.i386.rpm
url_x86_64[7]="ftp://ftp.ntua.gr/pub/linux/rpmfusion/free/el/updates/5/x86_64 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[7]=libmpeg2-0.5.1-3.el5.x86_64.rpm
#
url_i386[8]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/i386/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[8]=libsidplay-1.36.60-1.el5.rf.i386.rpm
url_x86_64[8]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/x86_64/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[8]=libsidplay-1.36.60-1.el5.rf.x86_64.rpm
#
url_i386[9]="ftp://mirror.switch.ch/pool/1/mirror/epel/5/i386 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[9]=libsndfile-1.0.17-5.el5.i386.rpm
url_x86_64[9]="ftp://mirror.switch.ch/pool/1/mirror/epel/5/x86_64 ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[9]=libsndfile-1.0.17-5.el5.x86_64.rpm
#
url_i386[10]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/i386/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[10]=twolame-0.3.13-1.el5.rf.i386.rpm
url_x86_64[10]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/x86_64/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[10]=twolame-0.3.13-1.el5.rf.x86_64.rpm
#
url_i386[11]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/i386/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[11]=lame-3.99.5-1.el5.rf.i386.rpm
url_x86_64[11]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/x86_64/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[11]=lame-3.99.5-1.el5.rf.x86_64.rpm
#
url_i386[12]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/i386/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[12]=gstreamer-plugins-ugly-0.10.11-2.el5.rf.i386.rpm
url_x86_64[12]="ftp://ftp.univie.ac.at/systems/linux/dag/redhat/el5/en/x86_64/dag/RPMS ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[12]=gstreamer-plugins-ugly-0.10.11-2.el5.rf.x86_64.rpm
#
# Download rpms
#
arch_list=`arch`
[ "$arch_list" = "x86_64" ] && arch_list="i386 x86_64"
typeset -i ii=0
while [ $ii -lt 13 ]; do
  for arch in $arch_list; do
    url_var="\${url_$arch[\$ii]}"; url=`eval echo "$url_var"`
    pkg_var="\${pkg_$arch[\$ii]}"; pkg=`eval echo "$pkg_var"`
    if [ ! -s "$pkg" ]; then
      if [ "$url" = "repository" ]; then
        cp "${HOME3P}/repository/$pkg" .
        [ $? -ne 0 ] && exit 1
      else
        copypkg "$pkg"
        if [ $? -ne 0 ]; then
          ok=1
          for URL in $url; do
            wget -t 5 -4 -O "$pkg" "$URL/$pkg"
            ok=$?
            [ $ok -eq 0 ] && break
          done
          if [ $ok -ne 0 ]; then
            rm -f "$pkg"
            exit 1
          fi
        fi
      fi
    fi
  done
  let ii=ii+1
done
#
# Install rpms
#
case `arch` in
  i386 )
    install_rpm a52dec-0.7.4-13.el5.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm amrnb-7.0.0.2-1.el5.rf.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libcdio-0.78.2-6.el5.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libdvdcss-1.2.10-1.el5.rf.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libdvdread-old-0.9.7-1.el5.rf.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libid3tag-0.15.1b-5.el5.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libmad-0.15.1b-8.el5.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libmpeg2-0.5.1-3.el5.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libsidplay-1.36.60-1.el5.rf.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libsndfile-1.0.17-5.el5.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm twolame-0.3.13-1.el5.rf.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm lame-3.99.5-1.el5.rf.i386.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm gstreamer-plugins-ugly-0.10.11-2.el5.rf.i386.rpm
    [ $? -ne 0 ] && exit 1
    ;;
  x86_64 )
    install_rpm a52dec-0.7.4-13.el5.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm amrnb-7.0.0.2-1.el5.rf.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libcdio-0.78.2-6.el5.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libdvdcss-1.2.10-1.el5.rf.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libdvdread-old-0.9.7-1.el5.rf.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libid3tag-0.15.1b-5.el5.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libmad-0.15.1b-8.el5.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libmpeg2-0.5.1-3.el5.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libsidplay-1.36.60-1.el5.rf.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libsndfile-1.0.17-5.el5.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm twolame-0.3.13-1.el5.rf.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm lame-3.99.5-1.el5.rf.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm gstreamer-plugins-ugly-0.10.11-2.el5.rf.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    ;;
esac
#
exit 0
