# YouTube Publication Configuration

This page documents the configuration for Opencast module **publication-service-youtube-v3**.

## Before you start

You need to meet these requirements to make a YouTube Publication:

- Google Account
- YouTube Channel to make the publication


## Google Developers Configuration

Below is a summarized version of [Google's quickstart page][googledoc].  If these
instructions do not work for you, or are unclear please let us know - Google has a habit of changing its configuration
pages and we don't always notice!

### Create new Google Project

- Login to Google account
- Navigate to the [**Google Developers Console**][googledevconsole]
- Click **Create Project** and follow the instructions
- Navigate to the [**Google Credentials Console**][googleapiconsole]
- Select **OAuth constent screen**
- Configure the API Consent Screen, you will need to set the Product name
- Select **Credentials**
- Select **Create Credentials**, specifically OAuth Client ID
- Select **Other** application type

### Save Client ID in JSON Format

- Download the client information in JSON format by clicking **Download JSON**
    - This currently looks like an arrow pointing downwards on the rightmost portion of the client id row
- Save the JSON file to `${karaf.etc}/youtube-v3/client-secrets-youtube-v3.json` (Usually this is
  `etc/youtube-v3/client-secrets-youtube-v3.json`)

### Enable API

- Naviate to the [**Google API Dashboard**][googledashboard]
- Click **Enable APIs and Services** in the navigation pane
- Use the filter to find and enable **YouTube Data API v3**


### Enable the publication service

- In `etc/org.opencastproject.publication.youtube.YouTubeV3PublicationServiceImpl.cfg` set `org.opencastproject.publication.youtube.enabled=true`
- Update the category, keywords, default privacy, and default playlist variables as required

## YouTube configuration in Opencast

With the JSON file created and saved previously, you have to proceed as described:

- Start Opencast server (Restart Opencast in case was running)

    **Note:** Until this service is fully configured, Opencast will not start completely. In case you
    want to abort the configuration, you only need to delete the JSON file and restart Opencast.

- In the command line, enter the command to view the extended status of the Opencast service:

        # systemctl status opencast -l

    This command will show parts of the Opencast logs in which you should see an URL that you have to copy to a browser.

- The web page will ask for your Google account (you have to use the account with which you created the developer
  project earlier) followed by access settings and settings for the channel you want to publish to.

- Once you have accepted the access, you will receive an answer like:

        Received verification code. Closing…

- Now verify that Opencast has received the access key and that it has been saved in
  `data/opencast/youtube-v3/data-store/store.`

- Restart Opencast


## Activate YouTube publication in Opencast

Opencast can now publish to YouTube. The last step is to activate this feature. For this you have to create a new
workflow or modify an existing one.

- Open the workflows `etc/opencast/workflows/ng-schedule-and-upload.xml` and `etc/opencast/workflows/ng-publish.xml`

- In the file, modify the `<configuration_panel>` and enable the YouTube option, like this:

        <input id="publishToYouTube" name="publishToYouTube" type="checkbox" class="configField" value="true"
               disabled="disabled" />

  becomes

        <input id="publishToYouTube" name="publishToYouTube" type="checkbox" class="configField" value="true"/>

- Open the workflows `etc/opencast/workflows/ng-retract.xml`

- In the file, modify the `<configuration_panel>` and enable the YouTube option, like this:

        <input id="retractFromYouTube" type="checkbox" class="configField" value="true" disabled="disabled" />

  becomes

        <input id="retractFromYouTube" type="checkbox" checked="checked" class="configField" value="true" />

Opencast will detect the new workflow without restart, with that you can select the new workflow with the YouTube option
enabled.

[googledevconsole]: https://console.developers.google.com/project
[googledoc]: https://developers.google.com/youtube/registering_an_application
[googleapiconsole]: https://console.developers.google.com/apis/credentials
[googledashboard]: https://console.developers.google.com/apis/dashboard
