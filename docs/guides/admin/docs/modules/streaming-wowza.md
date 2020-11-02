# Wowza Streaming Distribution Service

The `distribution-service-streaming-wowza` module copies the media files to the Wowza application directory
and generates a SMIL file containing the paths to those files, grouping those with the same flavor but different
qualities. Then, for each configured streaming protocol, it generates the adequate entries in the MediaPackage and sets
the necessary URLs and MIME-Types automatically.

The protocols supported and the transport format they use are summarized below:

* HTTP(S)-based protocols, corresponding to the modern Streaming Formats
    * **HLS:** (Live) Streaming from Apple
    * **HDS:** Dynamic Streaming from Adobe
    * **DASH:** MPEG-DASH Dynamic Adaptive Streaming
    * **SMOOTH:** Microsoft's Smooth Streaming

**Please note**: Only the protocols HLS and DASH (with and without SSL) have been thoroughly tested.


## Requirements

A Wowza Streaming Engine version >= 4.0 is required. Please pay special attention to the instructions re.
cross-domain access.


## Directory Structure

The structure how this module stores the SMIL and media files is important to understand how the Wowza server must be
configured to properly work with Opencast.

This structure always follows the same pattern:

    ${org.opencastproject.streaming.directory}/<organization-id>/<channel-id>/<mediapackage-id>/<element-id>/<filename>

, where:

* `${org.opencastproject.streaming.directory}` is this module's root directory, as configured in Opencast's
  configuration (see below)
* `<organization-id>` is the identifier for the current organization (by default `mh-default-org`)
* `<channel-id>` is the channel identifier. Normally, the Workflow Operation determines the value of this parameter;
  for instance, the operation `publish-engage` calls the Streaming Service with a hardcoded value for this property
  of `engage-player`
* `<mediapackage-id>`, `<element-id>` and `<filename>` are different for each MediaPackageElement that this module
  distributes.

The organization ID is automatically assigned based on the server's DNS name
([more info](../configuration/multi.tenancy.md)). Each organization (or *tenant*) is
independent from the others defined in the system. For the media distribution, that means that each organization's
media content is stored in separate directories, so the streaming applications should also be different, as we will see
below.


## Configuration


1. Edit the file `etc/org.opencastproject.distribution.streaming.wowza.WowzaStreamingDistributionService.cfg`
and adjust the values to match those of your scenario. Most important are:

        org.opencastproject.<tenant>.wowza.url=http(s)://<wowza-server>/<wowza-application>
        org.opencastproject.<tenant>.wowza.port=<port_number>

    The port numbers are only necessary when non-standard ports are used. In
    most cases, it is safe to comment them out or simply not include those properties in the file.
    Not defining the streaming url for a tenant will result in streams not being published for this tenant.
    
2. Set the streaming directory in `$KARAF/etc/custom.properties`:

        org.opencastproject.streaming.directory=/mnt/opencast-drive/content/streams

3. Restart your Opencast server.


## Installation on the Wowza side

---

### Pre-requirements

* Download/Purchase the Wowza Streaming Engine from the [Wowza Homepage](https://www.wowza.com/) and install it
according to their manuals.

* The shared drive indicated in the `org.opencastproject.streaming.directory` in the `custom.properties` file in
Opencast must also be mounted in the Wowza server. **Please note that mount points do not necessarily match!** (e.g.
the path `/mnt/opencast-drive-content-streams` in the Opencast server might be mounted as `/media/opencast-streams` in
the Wowza server).

* Do not forget to open your firewall on ports 80 (HTTP) and, if you want to use SSL,
443.

* You will have set your login credentials during the setup of Wowza. You will need these for the web UI.

---

1. Open `http://<wowza-server>:8088/enginemanager` and log in

2. Select "Application -> Add Application" in the top menu

3. Select "VOD Single Server"

4. Enter a name for the new application. You **must** use the same application name you have configured
in `$KARAF/etc/custom.properties` (for instance: *opencast-engage*)

5. **Application Description**: Feel free to add a description.

6. **Playback Types**: Enable your desired streaming protocols

7. **Options**: Disable the global CORS

8. **Content Directory**: Mark the checkbox *Use the following directory*. The directory you should input is a
    subdirectory of the path indicated in the property `org.opencastproject.streaming.directory` defined in the file
    `$KARAF/etc/custom.properties`. That subdirectory's name is the organization's ID (`mh_default_org` by default).

    For instance, if the `org.opencastproject.streaming.directory` is mounted in the Wowza Server as:

        /mnt/opencast-streams

    then the **Content Directory** for the default organization would be:

        /mnt/opencast-streams/mh_default_org

    In a multitenant Opencast setup, an organization with ID `my_organization` should have the **Content Directory**
    set to:

        /mnt/opencast-streams/my_organization


## Optional Settings

Opencast HTML5 Player is able to play videos from Wowza using adaptive streaming protocols. However, some browsers may
experiment problems due to cross-domain issues, which means that we need to instruct Wowza to include the right
`Allow-Origin` headers in its HTTP requests.

On the other hand, you may experiment problems with the MPEG-DASH protocol, depending on the encoding of the video
sources.

All this can be configured in the "Options" section of the Wowza application:

1. Click on the tab "Properties" in your application
    * If you can't see the "Properties" tab, go to "Users" > "Edit" > "Preferences" and select "Allow access to advanced
      properties and features"
2. Scroll down the page to "Custom"
3. Click the "Edit" button
4. Add the following Properties

    |Path                           |Name                                     |Type    |Value |
    |-------------------------------|-----------------------------------------|--------|------|
    |/Root/Application/HTTPStreamer |cupertinoUserHTTPHeaders                 |String  | \*\* |
    |/Root/Application/HTTPStreamer |mpegdashUserHTTPHeaders                  |String  | \*\* |
    |/Root/Application/HTTPStreamer |mpegdashAdjustCTTSForFirstKeyFrameToZero |Boolean | true |

    * *Due to some limitations in Bitbucket's Markdown parser, we can write this value within a table because it
    contains a "pipe" symbol ("|"). The correct value for this property is:*

            Access-Control-Allow-Origin: *|Access-Control-Allow-Methods:GET, HEAD, OPTIONS

5. **Do not forget to restart the application!**


## Players and Formats

* **Theodul**: HLS, DASH (over HTTP and HTTPS)
* **Paella** : HLS, DASH (over HTTP and HTTPS)


## Encoding Profiles

Keep in mind that you have to adapt your encoding profiles when you want generate the videos to distribute via HLS or
DASH. Specifically, if the videos with different qualities are not keyframe-aligned, they may not play smoothly or not
play at all. You can find more information
[here](https://www.wowza.com/docs/how-to-do-adaptive-bitrate-streaming).


## Limitations

This module is able to correctly distribute new elements incrementally. That means that if some elements in a
mediapackage are already distributed when another `Distribute` operation runs, the operation should run without errors.
However, partial `Retract` operations are discouraged and cause the remaining elements to be no longer playable.

The recommended procedure to retract only some elements in a mediapackage is therefore:

1. Completely retract the mediapackage
2. Distribute again only the desired elements

The effects of this limitation are small, because the `retract-engage` workflow operation always retracts the whole
Mediapackage and because partial retractions seem to have little to no practical application. These can however be
performed by calling the corresponding REST endpoints. In such cases, users are encouraged to use the recommended
method above.
