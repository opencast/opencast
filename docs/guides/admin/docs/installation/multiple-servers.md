Install Across Multiple Servers
===============================

*Note that this is not a comprehensive guide of all possible ways to install Opencast. It is more like an overview to good
practices and presents what a lot of people are running. If you are looking for a more detailed step by step instruction, check out the [example installation](example-installation.md).*


Opencast architecture
--------------------------

Opencast consists of a large set of modules which together build the whole system. In a distributed set-up, different
kinds of nodes are basically only defined by the existence or absence of specific modules.

While it is possible to stick together a system module by module, opencast comes with a set of pre-defined distribution
which can directly be built and installed. The three most common nodes types are admin, presentation and worker which make up the three server -set-up as described further down. To build these distributions by yourself, you would compile Opencast just like it is outlined in the installation guides for the [source installation](source-linux.md) and will then find a set of different distributions, both as archive and in a
separate directory.

To list all distributions, run the following command after Opencast is built:

    % ls -1 build/*.tar.gz
    build/opencast-dist-admin-${version}.tar.gz
    build/opencast-dist-allinone-${version}.tar.gz
    build/opencast-dist-presentation-${version}.tar.gz
    build/opencast-dist-worker-${version}.tar.g
    ...


The same distributions can be found in the packages provided in the Opencast RPM repository. These packages will
automatically install all dependencies for a given node type. For example, to install an Opencast worker node, you would
install the package `opencast-distribution-worker`.

The following list describes possible set-ups:

### All-In-One

This is the default set-up for installing Opencast on a single server. It works fine for testing purposes. It should
usually not be used in production since the processing of multiple videos might slow down other parts of Opencast if everything is installed on the same server. It is not distributed but is listed here to have a comprehensive list of predefined
distributions.


### Two-Server Set-up

This set-up is the minimum set-up recommended for productive use. In contrast to the All-In-One set-up, it will separate the video processing (worker nodes) from the rest of the system (admin, presentation), making the user-facing parts of your system much less affected by heavier loads.


### Three (or more) Server Set-up

While in the last example we have created one combined node for both the administrative tools and the workers, in this
example we will split it into dedicated worker and admin nodes. Using this set-up it is easy to increase the systems
performance simply by adding further worker nodes to the system. Therefore it consists of one admin node, one presentation node and at least one worker node.


