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
  6.* )
    ;;
  * )
    echo "Unsupported version of CentOS/RHEL: `os_ver`" 1>&2
    exit 1
    ;;
esac
#
# List of rpms and their dependencies
#
url_i386[0]="http://mirror.centos.org/centos/6/os/i386/Packages ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[0]=libdvdread-4.1.4-0.3.svn1183.el6.i686.rpm
url_x86_64[0]="http://mirror.centos.org/centos/6/os/x86_64/Packages ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[0]=libdvdread-4.1.4-0.3.svn1183.el6.x86_64.rpm
#
url_i386[1]="http://mirror.centos.org/centos/6/os/i386/Packages ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_i386[1]=libid3tag-0.15.1b-11.el6.i686.rpm
url_x86_64[1]="http://mirror.centos.org/centos/6/os/x86_64/Packages ${USASK_MIRROR} ${ETH_MIRROR}"
pkg_x86_64[1]=libid3tag-0.15.1b-11.el6.x86_64.rpm
#
# Download rpms
#
arch_list=`arch`
[ "$arch_list" = "x86_64" ] && arch_list="i386 x86_64"
typeset -i ii=0
while [ $ii -lt 2 ]; do
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
    install_rpm libdvdread-4.1.4-0.3.svn1183.el6.i686.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libid3tag-0.15.1b-11.el6.i686.rpm
    [ $? -ne 0 ] && exit 1
    sudox yum -y install gstreamer-plugins-ugly
    [ $? -ne 0 ] && exit 1
    ;;
  x86_64 )
    install_rpm libdvdread-4.1.4-0.3.svn1183.el6.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    install_rpm libid3tag-0.15.1b-11.el6.x86_64.rpm
    [ $? -ne 0 ] && exit 1
    sudox yum -y install gstreamer-plugins-ugly
    [ $? -ne 0 ] && exit 1
    ;;
esac
#
exit 0
