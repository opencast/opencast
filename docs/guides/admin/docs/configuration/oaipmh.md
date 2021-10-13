OAI-PMH Configuration
=====================

Overview
--------

OAI-PMH is an XML based protocol for metadata exchange using HTTP as the transport layer. An OAI-PMH system consists
of two parts, a repository on the one and the harvester on the other end. The repository is an HTTP accessible server
that exposes metadata to its client, the harvester.

OAI-PMH repositories will be accessed using URLs of the form:

    <OAI-PMH server> + <OAI-PMH mount point> + <OAI-PMH Repository>

Step 1: Configure the URL of the OAI-PMH server
-----------------------------------------------

The property to configure the OAI-PMH server URL can be found in
`etc/org.opencastproject.organization-mh_default_org.cfg`:

    prop.org.opencastproject.oaipmh.server.hosturl=http://localhost:8080

Step 2: Configure the OAI-PMH mount point
-----------------------------------------

The property to configure the OAI-PMH mount point can be found in `etc/custom.properties`:

    org.opencastproject.oaipmh.mountpoint=/oaipmh

Step 3: Configure the OAI-PMH default repository
------------------------------------------------

In case the repository is not included in the URL, the OAI-PMH default repository will be selected.

The property to configure the OAI-PMH default repository can be found in
`etc/org.opencastproject.oaipmh.server.OaiPmhServer.cfg`

    default-repository=default

Step 4: Allow access to OAI-PMH mount point
-------------------------------------------

Make sure that the OAI-PMH mount point is accessible. For example, if the OAI-PMH mount point has
been set to **/oaipmh**, the following two lines

    <sec:intercept-url pattern="/oaipmh/**" method="GET" access="ROLE_ANONYMOUS"/>
    <sec:intercept-url pattern="/oaipmh/**" method="POST" access="ROLE_ANONYMOUS"/>

should be present in `etc/security/mh_default_org.xml`.

Note that the OAI-PMH specification demands both GET and POST requests and that
it does not feature any access restrictions. If you need to restrict access
to OAI-PMH consider using Spring security or an iptables approach.

Step 5: Optionally configure OAI-PMH sets
-----------------------------------------

The OAI-PMH standard allow you to define sets. This can be used to filter data in your repository.
An OAI-PMH set will be defined by a name, unique setSpec, optional description and a filter.
The filters will be applied to the content of the published xml based elements and may contain one
or more filter criteria. You can also define more than one filter for a specific set.
Generally an OAI-PMH record is in the set if all set filters matches. A filter matches if any of
the filter criteria matches. The filter criteria may be: `contains`, `containsnot` or `match`.

Set definition configuration syntax:

```properties
    set.<set-id>.setSpec = setSpec value
    set.<set-id>.name = set name value
    set.<set-id>.description = optional set description
    set.<set-id>.filter.<filter-id>.flavor = set filter element flavor
    set.<set-id>.filter.<filter-id>.[<criteria-id>.]<criterion> = set filter criterion value
    # criteria-id should be set, if you provide more than one criteria for a filter
```

Example configuration for a set definition with one filter and one criterion:

```properties
    set.public.setSpec = public
    set.public.name = Public Recordings
    set.public.filter.1.flavor = security/xacml+episode
    set.public.filter.1.contains = >ROLE_ANONYMOUS</
```

The OAI-PMH records in the set public contain the role ROLE_ANONYMOUS in the published episode ACL.

Example configuration for a set definition with two filters and one or more criteria:

```properties
    set.openvideo.setSpec = open
    set.openvideo.name = Recordings with an open non commercial license
    set.openvideo.filter.1.flavor = security/xacml+episode
    set.openvideo.filter.1.contains = >ROLE_ANONYMOUS</
    set.openvideo.filter.2.flavor = dublincore/episode
    set.openvideo.filter.2.0.contains = license>CC0</
    set.openvideo.filter.2.1.contains = license>CC-BY</
    set.openvideo.filter.2.2.contains = license>CC-BY-SA</
    set.openvideo.filter.2.3.contains = license>CC-BY-NC</
    set.openvideo.filter.2.4.contains = license>CC-BY-NC-SA</
```

The OAI-PMH records in the set "open" contain the role ROLE_ANONYMOUS in the published episode ACL
and a CC0 or CC-BY derivate license without ND attribute.
You can also define the second filter as one `match` criterion like:

```properties
    set.openvideo.filter.2.match = license>CC[0-](?:BY(?:-(?:(?!ND)[^-<]+))*)?<
```

The `match` criterion tests are more CPU intensive as `contains` or `containsnot` criteria.
