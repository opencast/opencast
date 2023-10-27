# YouTube Publication Configuration

This page documents the configuration for Opencast module **publication-service-youtube-v3**.

## Before you start

You need to meet these requirements to make a YouTube Publication:

- Google Account
- YouTube Channel to publish to


## Google Developers Configuration

Below is a summarized version of [Google's quickstart page][googledoc].  If these
instructions do not work for you, or are unclear please let us know - Google has a habit of changing its configuration
pages and we don't always notice!

### Create new Google Project

- Login to your Google account
- Navigate to the [**Google Developers Console**][googledevconsole]
- Click **Create Project** and follow the instructions
- Navigate to the [**Google Credentials Console**][googleapiconsole]
- Select **OAuth consent screen**
- Configure the API Consent Screen
    - For **User type** select `external`
    - Fill in the rest of the required settings at discretion
- Select **Credentials**
- Select **Create Credentials**, specifically OAuth Client ID
    - **Application type** must be set to `Desktop`


### Save Client ID in JSON Format

- Select **Credentials**, you should see a new entry in the **OAuth 2.0 Client IDs** table
- Download the client information in JSON format by clicking **Download JSON**
    - This currently looks like an arrow pointing downwards on the rightmost portion of the client id row
- Save the JSON file to `${karaf.etc}/youtube-v3/client-secrets-youtube-v3.json` (Usually this is
  `etc/youtube-v3/client-secrets-youtube-v3.json`). It should match the path configured for
  `org.opencastproject.publication.youtube.clientSecretsV3` in
  `org.opencastproject.publication.youtube.YouTubeV3PublicationServiceImpl.cfg`.

### Enable API

- Navigate to the [**Google API Dashboard**][googledashboard]
- Click **Enable APIs and Services** in the navigation pane
- Use the filter to find and enable **YouTube Data API v3**


### Enable the publication service

- In `etc/org.opencastproject.publication.youtube.YouTubeV3PublicationServiceImpl.cfg` set `org.opencastproject.publication.youtube.enabled=true`
- Update the category, keywords, default privacy, and default playlist variables as required

## YouTube configuration in Opencast

With the JSON file created and saved previously, you have to proceed as described:

- Start Opencast server (Restart Opencast in case it was running)

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


## Add YouTube publication to your Opencast workflows

Opencast can now publish to YouTube. To make use of this, add the [Publish YouTube workflow operation](../workflowoperationhandlers/publish-youtube-woh.md)
to your Opencast workflows. You can find more details on how to use the operation on its [own page]((../workflowoperationhandlers/publish-youtube-woh.md)).
In general, it should be placed near your other `publish` operations.

You may also want to add the [Retract YouTube workflow operation](../workflowoperationhandlers/retract-youtube-woh.md) 
near your `retract` operations.



[googledevconsole]: https://console.developers.google.com/project
[googledoc]: https://developers.google.com/youtube/registering_an_application
[googleapiconsole]: https://console.developers.google.com/apis/credentials
[googledashboard]: https://console.developers.google.com/apis/dashboard
