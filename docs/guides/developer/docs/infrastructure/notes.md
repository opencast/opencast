Infrastructure Notes
====================

This page contains notes about the current configuration of the Opencast servers around the world


Harvard DCE
-----------

### Common Configuration Choices

- Unattended upgrade
- CentOS Linux release 7.x

### nexus.dcex.harvard.edu

- Using [packaged Nexus](https://copr.fedorainfracloud.org/coprs/lkiesow/nexus-oss/)


ETH
---

### opencast-nexus.ethz.ch

- Unattended upgrade
- RHEL 7.x
- Using [packaged Nexus](https://copr.fedorainfracloud.org/coprs/lkiesow/nexus-oss/)


SWITCH
------

### Common Configuration Choices

- Unattended upgrade
- CentOS Linux release 7.x

### Test Cluster (\*.oc-test.switch.ch)

- Rebuilt weekly via cron + shell, manual branch selection


University of Osnabrück
-----------------------

### Common Configuration Choices

- Unattended upgrade
- Scientific Linux 7.x

### build.opencast.org

- Builds are triggered by cron, manual branch selection

### nexus.opencast.org, nexus.virtuos.uos.de

- GeoIP based redirect for all Nexus servers
- Using [packaged Nexus](https://copr.fedorainfracloud.org/coprs/lkiesow/nexus-oss/)

### octestallinone.virtuos.uos.de

- Using tarballs build from build.opencast.org

### pullrequests.opencast.org

- Scientific Linux 6.x
- Due for retirement, services will be moved to repo.opencast.org VM and DNS updated
- Merge ticket list needs to be set manually

### repo.opencast.org and pkg.opencast.org

- Same server


University of Saskatchewean
---------------------------

### Common Configuration Choices

- Debian 8.x
- Unattended upgrades

### Testing Cluster (test\*.usask.ca)

- Using Debian packages for Opencast
- Nightly reset and upgrade

### oc-cache.usask.ca

- Using [Docker Nexus](https://hub.docker.com/r/lkiesow/opencast-nexus-oss/)
