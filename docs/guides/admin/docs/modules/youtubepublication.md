# YouTube Publication Configuration

This page documents the configuration for Opencast module **publication-service-youtube-v3**.

## Before you start


You need to meet these requirements to make a YouTube Publication:

- Google Account
- YouTube Channel to make the publication


## Google Developers Configuration

### Create new Google Project

- Login to Google account
- Navigate to the [**Google Developers Console**][googledevconsole]
- Click **Create Project** and follow the instructions
- On your new projects page, choose **APIs & auth** then **Consent screen** in the navigation pane
- Set the **PRODUCT NAME** and the **EMAIL ADDRESS**

### Enable API

- Choose **APIs** in the navigation pane
- Use the filter to find and enable **YouTube Data API v3**

### Register an Application

- Choose **Credentials** in the navigation panel
- Click **Create new Client ID** for OAuth
- Choose **Installed application** for the application type and **Other** for the installed application type
- Accept with **Create Client ID**

### Save Client ID in JSON Format

- Download the client information in JSON format by clicking **Download JSON**
- Save the JSON file to `${karaf.etc}/youtube-v3/client-secrets-youtube-v3.json` (Usually this is
  `etc/youtube-v3/client-secrets-youtube-v3.json`)


## YouTube configuration in Opencast

With the JSON file created and saved previously, you have to proceed as described:

- Start Opencast server (Restart Opencast in case was running)

    *ATTENTION:* Until this operation is fully configured, Opencast will not read and write the database. In case you
    want to abort the configuration, you only need to delete the JSON file and restart Opencast.

- In the command line, enter the command to view the extended status of the Opencast service:

        # systemctl status opencast -l

    This command will show parts of the Opencast logs in which you should see an URL that you have to copy to a browser.

- The web page will ask for your Google account (you have to use the account with which you created the developer
  project earlier) followed by access settings and settings for the channel you want to publish to.

- Once you have accepted the access, you will receive an answer like:

        Received verification code. Closing…

- Now verify that Opencast has received the access key and that it has been saved in
  `work/opencast/youtube-v3/data-store/store.`

- Restart Opencast


## Activate YouTube publication in Opencast

Opencast can now publish to YouTube. The last step is to activate this feature. For this you have to create a new
workflow or modify an existing one.

- Make a copy of the default workflow `etc/opencast/workflows/schedule-and-upload.xml` and create a copy named
  `etc/opencast/workflows/schedule-and-upload-youtube.xml`

- In the file, modify the `<configuration_panel>` and enable the YouTube option, like this:

        <input id="publishToYouTube" name="publishToYouTube" type="checkbox"
          checked="checked" class="configField" value="true" />
        <label for="publishToYouTube">YouTube</label>

- In `<operation id="defaults"`, set `publishToYouTube` to `true`.

- Additionally, in the workflow `partial-publish.xml` you also configure the `<operation id="publish-youtube"`. For
  example, you could specify to publish the presentation stream only by setting the configuration to:

        <configuration key="source-flavors">presentation/trimmed</configuration>

Opencast will detect the new workflow without restart, with that you can select the new workflow with the YouTube option
enabled.

[googledevconsole]: https://console.developers.google.com/project
