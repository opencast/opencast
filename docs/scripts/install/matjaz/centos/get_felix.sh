#!/bin/bash
AGENT="Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET CLR 1.1.4322; .NET4.0C; .NET4.0E)"
set -x
#
URL=http://repo2.maven.org/maven2/org/apache/felix/org.apache.felix.main.distribution/3.0.9/org.apache.felix.main.distribution-3.0.9.tar.gz
PKG=felix-3.0.9.tar.gz
#
#URL=http://repo2.maven.org/maven2/org/apache/felix/org.apache.felix.main.distribution/3.2.2/org.apache.felix.main.distribution-3.2.2.tar.gz
#PKG=felix-3.2.2.tar.gz
#
wget -t 5 -4 --retry-connrefused --progress=dot:mega -U "$AGENT" --no-cache "$URL" -O "$PKG"
[ $? -ne 0 ] && exit 1
exit 0
