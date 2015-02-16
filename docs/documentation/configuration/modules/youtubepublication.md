YouTube Publication Configuration
=================================
This page documents the configuration for Matterhorn module
**matterhorn-publication-service-youtube-v3**.
Create new Google Project
------------------
- Login to Google account
- Navigate to the [**Google Developers Console**][googledevconsole]
- Click **Create Project** and follow the instructions
- On your new projects page, choose **APIs & auth** then **Consent screen**
in the navigation pane
- Set the **PRODUCT NAME** and the **EMAIL ADDRESS**

Enable API
-----------
- Choose **APIs** in the navigation pane
- Use the filter to find and enable **YouTube Data API v3**

Register an Application
-----------------------
- Choose **Credentials** in the navigation pane
- Click **Create new Client ID** for OAuth
- Choose **Installed application** for the application type
and **Other** for the installed application type
- Accept with **Create Client ID**

Save Client ID in JSON Format
--------------------------------------
- Download the client information in JSON format by clicking **Download JSON**
- Save the JSON file to
`${bundles.configuration.location}/youtube-v3/client-secrets-youtube-v3.json`
(Usually this is `etc/youtube-v3/client-secrets-youtube-v3.json`)

Generate OAuth Tokens
---------------------
- Start Matterhorn
- Follow the request URL appearing in the Matterhorn console output and click
**Accept**
- The resulting website will say `Received verification code. Closing...`

The generated token is saved at
`${org.opencastproject.storage.dir}/youtube-v3/data-store/store`
(Usually this is `work/opencast/youtube-v3/data-store/store`).
If the file is not found or invalid a new request URL will be generated.
Both paths for the JSON file and the token file can be altered in the
service properties file at `etc/services/org.opencastproject.publication.
youtube.YouTubePublicationServiceImpl.properties`

[googledevconsole]: https://console.developers.google.com/project
