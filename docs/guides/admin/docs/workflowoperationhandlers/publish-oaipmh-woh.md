# PublishOaiPmhWorkflowOperation


## Description

The `PublishOaiPmhWorkflowOperation` exposes your media's metadata in a OAI-PMH repository for harvesting by OAI-PMH aware applications.


## Parameter Table

|Configuration Keys |Description                                                                                   |
|-------------------|----------------------------------------------------------------------------------------------|
|download-flavors   |Distribute any mediapackage elements with one of these (comma separated) flavors to download  |
|download-tags      |Distribute any mediapackage elements with one of these (comma separated) tags to download     |
|streaming-flavors  |Distribute any mediapackage elements with one of these (comma separated) flavors to streaming |
|streaming-tags     |Distribute any mediapackage elements with one of these (comma separated) tags to streaming    |
|check-availability |Check if the distributed download artifact is available at its URL (default: true)            |
|repository         |The name of the OAI-PMH repository where the media should be published to                     |
|external-template  |The optional URL template for URL the OAI-PMH publication element                             |
|external-channel   |The optional channel name for the OAI-PMH publication element                                 |
|external-mime-type |The optional mime type for the OAI-PMH publication element                                    |

Note: The all or none of the configuration keys `external-template`, `external-channel` and `external-mime-type` must to be set.

## Customizing the OAI-PMH Publication Element

If the configuration keys `external-template`, `external-channel` and `external-mime-type` are not set, the publication
element will use the following default values:

|Field        |Default Value                                                                                        |
|-------------|-----------------------------------------------------------------------------------------------------|
|url          | prop.org.opencastproject.oaipmh.server.hosturl + org.opencastproject.oaipmh.mountpoint + repository |
|mime type    | "text/xml"                                                                                          |
|channel name | "oaipmh-" + repository                                                                              |

Note that `org.opencastproject.oaipmh.server.hosturl` is defined in
`etc/org.opencastproject.organization-mh_default_org.cfg` and `org.opencastproject.oaipmh.mountpoint` is defined in
`custom.properties` and defaults to `/oaipmh`.

Example:

    http://localhost:8080/oaipmh/default

The OAI-PMH publication element can be customized by setting the configuration keys `external-template`,
`external-channel` and `external-mime-type`.

The URL of the publication element can be set by using `external-template`. The following variables can be used in the
template:

|Variable Name |Description                      |
|--------------|---------------------------------|
|event         |ID of the event being published  |
|series        |ID of the series being published |

Example:

    https://www.externalURL.com/watch.html?series={series}&id={event}

The configuration key `external-mime-type` is used to set the mime type of the content return when accessing the
URL of the publication element.

The configuration key 'external-channel' is used to set the name of the publication channel.

## Operation Example

    <operation
        id="publish-oaipmh"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Publish event to the OAI-PMH repository">
        <configurations>
            <configuration key="download-tags">oaipmh-download</configuration>
            <configuration key="streaming-tags">oaipmh-streaming</configuration>
            <configuration key="check-availability">true</configuration>
            <configuration key="repository">default</configuration>
        </configurations>
    </operation>
