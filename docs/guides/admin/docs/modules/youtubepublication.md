# YouTube Publication Configuration

This page documents the configuration for Opencast module **matterhorn-publication-service-youtube-v3**.

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

`**ATTENTION:** 
Until this operation is fully configured, Opencast will not read and write the database. In case you want to abort the configuration, you only need to delete the JSON file and restart Opencast.`

- In the command line, enter the command to view the extended status of the Opencast service:

`# systemctl status opencast -l`

This command will retrieve an URL that you have to copy in a browser in a pc with internet access.

- The web page will ask for your Google account; you have to use the account with you created the developer project in Google in the first place. The page will ask the selection to witch channel you want Opencast publish and if we grant access.
- Once the access is granted, the browser will show a connection error, this is normal, because it's asking to an inexistence site inside the client. **You need to copy that invalid direction and execute this line**:

`# curl [Returned direction by the client's browser]`

- Once you have done that, you will receive an answer from Opencast

`Received verification code. Closing... `

- Verify that the key has been saved in `work/opencast/youtube-v3/data-store/store.`
- Restart Opencast

##Activate YouTube publication in Opencast

Opencast is enabled now to publish in YouTube, the last step is to activate this feature. For this you have to create a new workflow that allow it.

From Opencast 2.2.2 this feature was disabled, to enable it, you need to follow this steps:

- Make a copy of the default workflow `etc/opencast/workflows/ng-schedule-and-upload.xml` and create a copy named `etc/opencast/workflows/ng-schedule-and-upload-youtube.xml`

- To the copy, you have to modify this values:

 - In `<configuration_panel>` enable the YouTube option, like this:

```xml
<input id="publishToYouTube" name="publishToYouTube" type="checkbox" checked="checked" class="configField" value="true" />
<label for="publishToYouTube">YouTube</label>
```
 - In `<operation id="defaults" description="Applying default configuration values">`, change to `true` the key `publishToYouTube`.

 - In the workflow `ng-partial-publish.xml` you have to comment the block called `<operation "publish-youtube">` and paste this code:

```xml
<operation
      id="publish-youtube"
      if="${publishToYouTube}"
      max-attempts="2"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Publish to YouTube">
      <configurations>
        <configuration key="source-flavors">presenter/trimmed</configuration>
      </configurations>
    </operation>
```

This code has been configured to publish the records of the presenter stream, if you want to publish the presentation stream, you have to change the line from `presenter/trimmed` to `presentation/trimmed` 

Opencast will detect the new workflow without restart, with that you can select the new workflow with the YouTube option enabled.

[googledevconsole]: https://console.developers.google.com/project
