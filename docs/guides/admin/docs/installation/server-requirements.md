Hardware Requirements
=====================

The resources Opencast needs highly depend on what and how much material you process on your cluster
and how you configured your Opencast.
Thus, treat this as a sensible starting point for testing out Opencast, nothing more.


Sensible requirements for an Opencast server:

- Virtual machine
- Four cores
- 8GB of memory

For a production ready Opencast cluster it is generally recommended to have at least three VMs with the following specs in addition to a dedicated NFS storage:

Admin node:
- Four cores
- 8GB of memory

Worker node:
- Four cores
- 8GB of memory

Presentation node:
- Four cores
- 4GB of memory

NFS share:
- 5TB disk space

You can have a look at the [overview](multiple-servers.md) and decide which setup is most suited for your use-case.

Additional notes about resources:

- Video processing is hard work and mostly profits from more CPU power
- More memory is only necessary for very large installations or with some special use-cases
