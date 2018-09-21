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


University of Cologne
---------------------

### Common Configuration Choices

- Unattended upgrade
- CentOS Linux release 7.x

### ci.opencast.org

- Buildbot installed and managed via ansible script in [this repo](https://github.com/opencast/opencast-project-infrastructure)


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

### repo.opencast.org, pkg.opencast.org, pullrequests.opencast.org

- Same server

