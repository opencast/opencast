OAI-PMH
=======

Overview
--------

OAI-PMH is an XML based protocol for metadata exchange using HTTP as the transport layer. An OAI-PMH system consists
of two parts, a repository on the one and the harvester on the other end. The repository is an HTTP accessible server
that exposes metadata to its client, the harvester. A repository is required to deliver metadata following the
[DublinCore element set version 1.1](http://dublincore.org/documents/dces/) metadata scheme. Additionally it may
deliver metadata in any arbitrary format, that can be encoded as XML.

For a more detailed introduction please see the
[OAI-PMH specification](http://www.openarchives.org/OAI/openarchivesprotocol.html).

Metadata Prefixes
-----------------

The Opencast OAI-PMH server supports three metadata prefixes:

|Prefix             |Description                                                         |
|-------------------|--------------------------------------------------------------------|
|oai_dc             |Dublin core element set 1.1 as required by the OAI-PMH specification|
|matterhorn         |Opencast media package representation                               |
|matterhorn-inlined |Opencast media package representation with embedded catalogs        |

Note that the metadata prefix **oai_dc** is a standard metadata representation supported by all OAI-PMH servers and
harverster, while the other prefixes are specific to Opencast.
If you use an harvester in your third-party application, the havester will therefore need to be extended to
support the Opencast-specific metadata prefixes.

Glossary
--------

|Term            |Description                                                                                         |
|----------------|----------------------------------------------------------------------------------------------------|
|Repository      |An entity that holds a set of items to be disseminated via the OAI-PMH protocol. Different repositories may hold different sets of items. |
|Channel         |The client's perspective on a repository. Each channel is backed by a single repository (1:1 relationship) so these terms may be used synonymously depending on the perspective. |
|Item            |The base entity of a repository. In Opencast an item is equal to a mediapackage. |
|Metadata format |OAI-PMH repositories disseminate their content in various formats. The oai_dc format is mandatory to each OAI-PMH repository. Formats are expressed in XML. |
|Metadata prefix |The prefix identifies a metadata format. The terms are often used synonymously. |