#!/bin/bash
set -x
VER=6
UPD=31
BLD=04
#
# Install java on CentOS
#
install_java_centos() {
  set -x
  #
  case `./arch.sh` in
    i386 )
      PKG=jdk-${VER}u${UPD}-linux-i586-rpm.bin
      ARCH=i386
      ;;
    x86_64 )
      PKG=jdk-${VER}u${UPD}-linux-x64-rpm.bin
      ARCH=amd64
      ;;
    * ) 
      echo "Unknown architecture: `./arch.sh`" 1>&2
      return 1 ;;
  esac
  #
  JDK=`rpm -q jdk 2>/dev/null`
  if [ $? -eq 0 ]; then
    sudo rpm -e "$JDK" # erase previous version of java
    [ $? -ne 0 ] && return 1
  fi
  #
  if [ ! -s $PKG ]; then
    wget -t 5 -4 --load-cookies java_cookie.txt http://download.oracle.com/otn-pub/java/jdk/${VER}u${UPD}-b${BLD}/$PKG -O $PKG
    [ $? -ne 0 ] && return 1
  fi
  chmod +x $PKG
  [ $? -ne 0 ] && return 1
  sudo ./$PKG
  [ $? -ne 0 ] && return 1
  #
  JDK=`\ls -d1 /usr/java/jdk*${UPD}/jre/bin/java 2>/dev/null | awk 'END{print}'`
  JDK="${JDK%/jre/bin/java}"
  #
  sudo /usr/sbin/alternatives --install /usr/bin/java java $JDK/jre/bin/java 30000
  [ $? -ne 0 ] && return 1
  sudo /usr/sbin/alternatives --auto java
  [ $? -ne 0 ] && return 1
  #
  sudo /usr/sbin/alternatives --install /usr/bin/javaws javaws $JDK/jre/bin/javaws 30000
  [ $? -ne 0 ] && return 1
  sudo /usr/sbin/alternatives --auto javaws
  [ $? -ne 0 ] && return 1
  #
  sudo /usr/sbin/alternatives --install /usr/bin/javac javac $JDK/bin/javac 30000
  [ $? -ne 0 ] && return 1
  sudo /usr/sbin/alternatives --auto javac
  [ $? -ne 0 ] && return 1
  #
  sudo /usr/sbin/alternatives --install /usr/bin/javadoc javadoc $JDK/bin/javadoc 30000
  [ $? -ne 0 ] && return 1
  sudo /usr/sbin/alternatives --auto javadoc
  [ $? -ne 0 ] && return 1
  #
  sudo /usr/sbin/alternatives --install /usr/bin/javah javah $JDK/bin/javah 30000
  [ $? -ne 0 ] && return 1
  sudo /usr/sbin/alternatives --auto javah
  [ $? -ne 0 ] && return 1
  #
  sudo /usr/sbin/alternatives --install /usr/bin/javap javap $JDK/bin/javap 30000
  [ $? -ne 0 ] && return 1
  sudo /usr/sbin/alternatives --auto javap
  [ $? -ne 0 ] && return 1
  #
  sudo /usr/sbin/alternatives --install /usr/lib/mozilla/plugins/libjavaplugin.so libjavaplugin.so $JDK/jre/lib/$ARCH/libnpjp2.so 30000
  [ $? -ne 0 ] && return 1
  sudo /usr/sbin/alternatives --auto libjavaplugin.so
  [ $? -ne 0 ] && return 1
  #
  return 0
}
#
# Install java on Ubuntu
#
install_java_ubuntu() {
  set -x
  #
  case `./arch.sh` in
    i386 )
      PKG=jdk-${VER}u${UPD}-linux-i586.bin
      ARCH=i386
      ;;
    x86_64 )
      PKG=jdk-${VER}u${UPD}-linux-x64.bin
      ARCH=amd64
      ;;
    * ) 
      echo "Unknown architecture: `./arch.sh`" 1>&2
      return 1 ;;
  esac
  #
  ./enable_partner.sh
  [ $? -ne 0 ] && return 1
  sudo apt-get update
  [ $? -ne 0 ] && return 1
  #
  if [ ! -s $PKG ]; then
    wget -t 5 -4 --load-cookies java_cookie.txt http://download.oracle.com/otn-pub/java/jdk/${VER}u${UPD}-b${BLD}/$PKG -O $PKG
    [ $? -ne 0 ] && return 1
  fi
  chmod +x $PKG
  [ $? -ne 0 ] && return 1
  sudo ./$PKG
  [ $? -ne 0 ] && return 1
  sudo mkdir -p /usr/lib/jvm
  [ $? -ne 0 ] && return 1
  sudo mv jdk*${UPD} /usr/lib/jvm
  [ $? -ne 0 ] && return 1
  #
  JDK=`\ls -d1 /usr/lib/jvm/jdk*${UPD}/jre/bin/java 2>/dev/null | awk 'END{print}'`
  JDK="${JDK%/jre/bin/java}"
  #
  sudo update-alternatives --install /usr/bin/java java $JDK/jre/bin/java 30000
  [ $? -ne 0 ] && return 1
  sudo update-alternatives --auto java
  [ $? -ne 0 ] && return 1
  #
  sudo update-alternatives --install /usr/bin/javaws javaws $JDK/jre/bin/javaws 30000
  [ $? -ne 0 ] && return 1
  sudo update-alternatives --auto javaws
  [ $? -ne 0 ] && return 1
  #
  sudo update-alternatives --install /usr/bin/javac javac $JDK/bin/javac 30000
  [ $? -ne 0 ] && return 1
  sudo update-alternatives --auto javac
  [ $? -ne 0 ] && return 1
  #
  sudo update-alternatives --install /usr/bin/javadoc javadoc $JDK/bin/javadoc 30000
  [ $? -ne 0 ] && return 1
  sudo update-alternatives --auto javadoc
  [ $? -ne 0 ] && return 1
  #
  sudo update-alternatives --install /usr/bin/javah javah $JDK/bin/javah 30000
  [ $? -ne 0 ] && return 1
  sudo update-alternatives --auto javah
  [ $? -ne 0 ] && return 1
  #
  sudo update-alternatives --install /usr/bin/javap javap $JDK/bin/javap 30000
  [ $? -ne 0 ] && return 1
  sudo update-alternatives --auto javap
  [ $? -ne 0 ] && return 1
  #
  sudo update-alternatives --install /usr/lib/mozilla/plugins/libjavaplugin.so mozilla-javaplugin.so $JDK/jre/lib/$ARCH/libnpjp2.so 30000
  [ $? -ne 0 ] && return 1
  sudo update-alternatives --auto mozilla-javaplugin.so
  [ $? -ne 0 ] && return 1
  #
  return 0
}
#
# Cookie is needed for downloading jdk from Oracle!
#
cat > java_cookie.b64 << 'EOF'
H4sICPLRck8CA2phdmFfY29va2llLnR4dACFU12PojAUfS6/ookxM/sASBFl543xY7Obye5Gx2dT
y1UZS8vSIvrvp3y5uoluQqD39txz4JzSwxMpDwkovJU5jmVKE6FwDpxqiLGWWOaUcXCYTB2rh9/3
icJMCg1C45Se8QZwRlWFTYRBU7NZ0zn6pPE24YCpiHGhDGBzxuUOtGGZnWiacXjBdQPbJ2zbXNLY
bodvSPZaZy+uW5al8/ddXA1sL0CXMj+4H/RI65sCN5alqKiU+xEf7HHh213H9oKBNyIjZ69TbvWs
W0I0j96WM/R/3hY4QBqUXoJSiRSNhWgm6IZDbDkQA0+OkJ+vBd4XK8PfjnujYUjIIBwEKFqs1m/R
z29otbTu44Nw/NUL/GGI1FrkyPN9Eo5IULF498cGBs0Y0nkBd773Amy2eMJAKOi8k3pv//i9QJQx
yLStgBU5bCg7FFmb1WNt9afllVpUjvTJpKl3XG4o7/vTPgnIKEti8/SnBlXVgR+BsAvVrusHmVdB
XC0VtMUlmW7zUe6dnK71vLqUrfq2EEybNKVgxobDMxzNIf9Ss45fGweiXQ6QmvZzmQgj4Cjg20Z3
8vSPaU/NqP/aMEw7rUaaNGVTLKLp918Pww/8cOybw7LLyjWQIap+in5lzdxct8maxtUh7nfGXUy7
NuyxWdYnXKcvDh0EAAA=
EOF
[ $? -ne 0 ] && exit 1
base64 -d --ignore-garbage java_cookie.b64 > java_cookie.txt.gz
[ $? -ne 0 ] && exit 1
gzip -df java_cookie.txt.gz
[ $? -ne 0 ] && exit 1
rm -f java_cookie.b64
[ $? -ne 0 ] && exit 1
exit 0
#
case `./os.sh` in
  CentOS | RHEL )
    install_java_centos
    [ $? -ne 0 ] && exit 1
    #
    JAVA_VERSION=`./java_version.sh`
    [ -z "$JAVA_VERSION" ] && exit 1
    export JAVA_HOME="/usr/java/jdk${JAVA_VERSION}"
    ./set_profile_var.sh "JAVA_HOME" "$JAVA_HOME"
    [ $? -ne 0 ] && exit 1
    ;;
  Ubuntu )
    install_java_ubuntu
    [ $? -ne 0 ] && exit 1
    #
    JAVA_VERSION=`./java_version.sh`
    [ -z "$JAVA_VERSION" ] && exit 1
    #JAVA_VERSION=${JAVA_VERSION//_/.}
    export JAVA_HOME="/usr/lib/jvm/jdk${JAVA_VERSION}"
    ./set_profile_var.sh "JAVA_HOME" "$JAVA_HOME"
    [ $? -ne 0 ] && exit 1
    ;;
  * )
    echo "Unsupported operating system: `./os.sh`" 1>&2
    exit 1
    ;;
esac
#
echo "Java version $JAVA_VERSION installed on $JAVA_HOME"
exit 0
