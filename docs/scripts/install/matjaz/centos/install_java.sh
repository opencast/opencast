#!/bin/bash
set -x
#
# Install java on CentOS
#
install_java_centos() {
  set -x
  #
  case `./arch.sh` in
    i386 )
      PKG=jdk-6u27-linux-i586-rpm.bin
      ARCH=i386
      ;;
    x86_64 )
      PKG=jdk-6u27-linux-x64-rpm.bin
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
    wget -t 5 -4 http://oracleotn.rd.llnwd.net/otn-pub/java/jdk/6u27-b07/$PKG -O $PKG
    [ $? -ne 0 ] && return 1
  fi
  chmod +x $PKG
  [ $? -ne 0 ] && return 1
  sudo ./$PKG
  [ $? -ne 0 ] && return 1
  #
  JDK=`\ls -d1 /usr/java/jdk*/jre/bin/java 2>/dev/null | awk 'END{print}'`
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
      ARCH=i386
      ;;
    x86_64 )
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
  sudo apt-get -y install sun-java6-jdk
  [ $? -ne 0 ] && return 1
  sudo apt-get -y install sun-java6-plugin
  [ $? -ne 0 ] && return 1
  #
  JDK=`\ls -d1 /usr/lib/jvm/java-6-sun-*/jre/bin/java 2>/dev/null | awk 'END{print}'`
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
    JAVA_VERSION=${JAVA_VERSION//_/.}
    export JAVA_HOME="/usr/lib/jvm/java-6-sun-${JAVA_VERSION}"
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
