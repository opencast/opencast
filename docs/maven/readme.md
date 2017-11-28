Special Maven Settings
======================

The Maven settings file in this repository shows how to configure an
alternative primary and secondary repository for Opencast. This example just
switches the primary repository from the one hosted by Harvard DCE to the
repository hosted the University of Osnabrück.

To use this setting file either use the maven command line options:

    mvn -s settings.xml …

…or copy the file to your local maven folder:

    cp settings.xml ~/.m2/settings.xml

Note that manually selecting a repository nearby your location can speed up
dependency fetching dramatically. For more information, take a look at
Opencast's developer documentation at:

  https://docs.opencast.org/develop/developer/
