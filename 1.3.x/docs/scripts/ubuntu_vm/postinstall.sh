#!/bin/bash

function exec_jail() {
  cmd="$*"
  echo "Executing command in jail '${jail}': ${cmd}"
  chroot $jail $cmd
}

function sun-java() {
  #unattended vmbuilder java install
  exec_jail apt-get update
  exec_jail debconf-set-selections <<<'sun-java6-jre sun-java6-jre/jcepolicy note'
  exec_jail debconf-set-selections <<<'sun-java6-jre sun-java6-jre/stopthread boolean true'
  exec_jail debconf-set-selections <<<'sun-java6-jre shared/accepted-sun-dlj-v1-1 boolean true'
  exec_jail debconf-set-selections <<<'sun-java6-bin shared/accepted-sun-dlj-v1-1 boolean true'
  exec_jail debconf-set-selections <<<'sun-java6-jre shared/present-sun-dlj-v1-1 note'
  exec_jail debconf-set-selections <<<'sun-java6-bin shared/present-sun-dlj-v1-1 note'
  exec_jail apt-get install -y sun-java6-jdk
#  exec_jail update-java-alternatives -s java-6-sun
}

echo "#########################"
echo "###Post-Install script###"
echo "#########################"
jail=$1
sun-java
