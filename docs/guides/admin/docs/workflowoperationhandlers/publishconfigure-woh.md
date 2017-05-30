# ConfigurablePublishWorkflowOperationHandler


## Description

The ConfigurablePublishWorkflowOperationHandler will distribute the given elements and create a publication element for it. In default it will retract all publications before publishing the new.
 

## Parameter Table

These are the keys that are configured through the workflow definition (The files located in /opt/matterhorn/etc/workflows used to process files). At least one mediapackage element must match the supplied source-flavors or source-tags or else the operation won't know what to publish. The channel-id and url-pattern are also mandatory.

|Configuration Key       |Description                                                                                             |Example                                        |
|------------------------|--------------------------------------------------------------------------------------------------------|-----------------------------------------------|
|channel-id              |Is the id of the channel that this publication will be published to.                                    |internal                                       |
|mimetype                |The Mimetype (file type) of the published element. If missing it will use the last distributed element. |text/html                                      |
|source-flavors          |The flavors of the mediapackage elements to publish.                                                    |*/trimmed                                      |
|source-tags             |The tags of the mediapackage elements to publish.                                                       |engage-download                                |
|url-pattern             |The pattern to insert the variables into to create the uri for the published element.                   |http://api.opencast.org/api/events/${event_id} |
|with-published-elements |Use the current contents of the mediapackage instead of publishing elements to a channel                |true                                           |
|check-availability      |Check if the media if reachable (default: false)                                                        |true                                           |
|strategy                |Strategy if there is allready published material default is retract                                     |fail                                           |
|mode                    |Control how elements are distributed: single, mixed or bulk (default: bulk)                             |mixed                                          |

## Mode

The configuration key `mode` can be used to control how media package elements are being distributed:

|Mode   |Description                                                                      |
|-------|---------------------------------------------------------------------------------|
|single |For each media package element, a job is created                                 |
|mixed  |One job for all media package elements that are not tracks and one job per track |
|bulk   |One job for all media package elements                                           |

This allows you to choose a lot of jobs and parallelism (`single`), just one job and no parallelism (`bulk`)
or something in between (`mixed`). The best choice depends on your setup.

## Url Pattern Variables

These are the variables that are available when you define the url-pattern configuration above. They will be replaced with the value during the running of the workflow operation.

|Variable          |Description                                                                              |Example                              |
|------------------|-----------------------------------------------------------------------------------------|-------------------------------------|
|${element_uri}    |Substitutes the URI of the last mediapackage element if there is more than one, for example if it is a track this will be the URI that matches the internal location of the file. |http://adminnode.com/files/mediapackage/18633e04-1a3f-4bbb-a72a-99c15deba1b9/cec1f067-9470-4f44-9d5b-9dc0f24df04c/short.mp4 |
|${event_id}       |Substitutes the event's (also known as the mediapackage) id.                             |18633e04-1a3f-4bbb-a72a-99c15deba1b9 |
|${player_path}    |Substitutes the player path for the current organization that is running the workflow.   |/engage/theodul/ui/core.html?id=     |
|${publication_id} |The id of this publication.                                                              |54f6c12d-8e68-4ec8-badf-cd045b33d01e |
|${series_id}      |The id of the series if available, empty if not.                                         |36f3c5d8-ad4d-4dab-beb1-1400ffab4a69 |

## Publication Channel Labels and Icons

Using this workflow operation, you can create arbitrary custom publication channels as technically identified by the
identifier specified using the configuration key `channel-id`.
Without any further actions, the administrative user interface will label those custom publication channels as
"Custom".
You can specify both the displayable name (label) and a displayable icon for such custom publication channels in the
configuration files `etc/listproviders/publication.channel.labels.properties` and
`etc/listproviders/publication.channel.icons.properties`.

## Operation Examples

####Publishing to the Internal Channel

    <operation
        id="publish-engage"
        exception-handler-workflow="ng-partial-error"
        description="Publish to internal channel">
        <configurations>
            <configuration key="source-tags">engage,atom,rss</configuration>
            <configuration key="channel-id">internal</configuration>
            <configuration key="url-pattern">http://localhost:8080/admin-ng/index.html#/events/events/${event_id}/tools/playback</configuration>
        </configurations>
    </operation>

####Publishing as External API

    <operation
        id="publish-configure"
        exception-handler-workflow="ng-partial-error"
        description="Publish to external api publication channel">
        <configurations>
            <configuration key="channel-id">api</configuration>
            <configuration key="mimetype">application/json</configuration>
            <configuration key="source-tags">engage-download,engage-streaming</configuration>
            <configuration key="url-pattern">http://api.opencast.org/api/events/${event_id}</configuration>
            <configuration key="check-availability">true</configuration>
        </configurations>
    </operation>
