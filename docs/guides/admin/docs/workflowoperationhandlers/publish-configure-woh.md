# ConfigurablePublishWorkflowOperationHandler

## Description

The `ConfigurablePublishWorkflowOperationHandler` will distribute the given elements and create a publication element 
for them. By default it will retract all published download elements before publishing anew.

## Parameter Table

These are the keys that can be configured for this operation in the workflow definition. At least one media package 
element must match the supplied `source-flavors` or `source-tags` (either for download or streaming or both) or else 
the operation will not know what to publish. The `channel-id` and `url-pattern` are also mandatory.

|Key                      |Description                                                |Example              |Default  |
|-------------------------|-----------------------------------------------------------|---------------------|---------|
|channel-id               |Id of the channel to publish to                            |`internal`           |         |
|mimetype                 |Mime type of the published element                         |`text/html`          |Type of last distributed element|
|download-source-flavors  |Flavors of the download media package elements to publish  |`*/trimmed`          |         |
|download-source-tags     |Tags of the download media package elements to publish     |`engage-download`    |         |
|streaming-source-flavors |Flavors of the streaming media package elements to publish |`*/trimmed`          |         |
|streaming-source-tags    |Tags of the streaming media package elements to publish    |`engage-streaming`   |         |
|url-pattern              |Pattern to create the URI for the published from           |`ftp://…/${event_id}`|         |
|with-published-elements  |Use the current contents of the media package instead of publishing elements to a channel|`true`|  |
|check-availability       |Check if the media is reachable after publication          |`false`              |`false`  |
|strategy                 |Strategy for when there is already published material      |`fail`               |`retract`|
|mode                     |How elements are distributed                               |`mixed`              |`bulk`   |
|retract-streaming        |Whether to also retract streaming elements \*              |`true`               |`false`  |

\* Enable if you have published streaming elements to this channel.

## Mode

The configuration key `mode` can be used to control how media package elements are being distributed:

|Mode   |Description                                                                      |
|-------|---------------------------------------------------------------------------------|
|single |For each media package element, a job is created                                 |
|mixed  |One job for all media package elements that are not tracks and one job per track |
|bulk   |One job for all media package elements                                           |

This allows you to choose a lot of jobs and parallelism (`single`), just one job and no parallelism (`bulk`)
or something in between (`mixed`). The best choice depends on your setup.

## URL Pattern Variables

These are the variables available in the `url-pattern` configuration. They will be replaced with the value during the
execution of the workflow operation.

|Variable           |Description                               |Example                               |
|-------------------|------------------------------------------|--------------------------------------|
|`${event_id}`      |The event (media package) identifier      |`18633e04-1a3f-4bbb-a72a-99c15deba1b9`|
|`${player_path}`   |The player path for the event             |`/engage/theodul/ui/core.html?id=`    |
|`${publication_id}`|The id of this publication.               |`54f6c12d-8e68-4ec8-badf-cd045b33d01e`|
|`${series_id}`     |The id of the series if available         |`36f3c5d8-ad4d-4dab-beb1-1400ffab4a69`|

The organization properties are also available and can be accessed with the `org_` prefix followed by the property name,
eg. `${org_player}` will be replaced by the value of the organization property named `player`.

Note some organization properties contain an `.` (period) in their name (e.g. `org.opencastproject.external.api.url`).
As this character have an special meaning in the FreeMarker library (used for substitution), all occurrences are replaced
with `_` (underscore).

Additional to the organization properties you can use `org_id`, `org_name`, `org_admin_role` and
`org_anonymous_role` as well.

## Publication Channel Labels and Icons

Using this workflow operation, you can create arbitrary custom publication channels. Without further action, the
administrative user interface will label these channels "Custom". You can specify both a label and an icon for each
custom publication channels in the configuration files `etc/listproviders/publication.channel.labels.properties` and
`etc/listproviders/publication.channel.icons.properties`.

## Operation Examples

### Internal Channel

    <operation
      id="publish-configure"
      exception-handler-workflow="partial-error"
      description="Publish to internal channel">
      <configurations>
        <configuration key="download-source-tags">engage,atom,rss</configuration>
        <configuration key="channel-id">internal</configuration>
        <configuration key="url-pattern">http://localhost:8080/admin-ng/index.html#/events/events/${event_id}/tools/playback</configuration>
      </configurations>
    </operation>

### External API

    <operation
      id="publish-configure"
      exception-handler-workflow="partial-error"
      description="Publish to external api publication channel">
      <configurations>
        <configuration key="channel-id">api</configuration>
        <configuration key="mimetype">application/json</configuration>
        <configuration key="download-source-tags">engage-download,engage-streaming</configuration>
        <configuration key="url-pattern">http://api.oc.org/api/events/${event_id}</configuration>
        <configuration key="check-availability">true</configuration>
      </configurations>
    </operation>

### Meta publication
Meta publications are URL only publications as reference to external systems like media portal.

    <operation
      id="publish-configure"
      exception-handler-workflow="partial-error"
      description="Publish to meta publication channel">
      <configurations>
        <configuration key="channel-id">edu-reference</configuration>
        <configuration key="mimetype">text/html</configuration>
        <configuration key="url-pattern">https://org.mediaportal.edu/events/${event_id}</configuration>
        <configuration key="check-availability">false</configuration>
      </configurations>
    </operation>
