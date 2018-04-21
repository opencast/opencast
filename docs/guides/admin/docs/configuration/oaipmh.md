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
