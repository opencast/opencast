Configuration
=============

## Configuring Encryption Keys

There are two new properties file at `${matterhorn.home}/etc/services/`

    org.opencastproject.security.urlsigning.provider.impl.WowzaUrlSigningProvider.properties
    org.opencastproject.security.urlsigning.provider.impl.GenericUrlSigningProvider.properties

These files controls how URLs are signed within Matterhorn. These properties files allows you to configure multiple encryption keys for signing URLs that are matched differently. There are more details in the configuration file itself but there are three properties to set key, id and url. For example we could have a configuration file such as:


    # This configuration file is used to configure the signing of urls so that
    # they will expire and can either only become available after a certain amount
    # of time or restricted to a particular client IP.

    # There are three values that need to be set for each type of url that will be
    # secured. A suffix of numbers in order allow the configuration of several
    # keys and urls.

    # There is the encryption key that is the 128 byte key used to sign the url. e.g. 0123456789abcdef
    # key.1=0123456789abcdef
    # There is the id that will identify which key to decode the signature with on the resource provider side. e.g. theId
    # id.1=theId
    # There is the url that will identify which urls that should be signed with this key. e.g. rtmp or http://hostname.com/
    # url.1=rtmp

    # To define a second key just add a number to the suffix. For example:
    # key.2=0123456789abcdef
    # id.2=AnotherKey
    # url.2=http://hostname.com/

    # Demo Key No. 1
    key.1=6EDB5EDDCF994B7432C371D7C274F
    id.1=demoKeyOne
    url.1=http://mh-wowza

    # Demo Key No. 2
    key.2=C843C21ECF59F2B38872A1BCAA774
    id.2=demoKeyTwo
    url.2=rtmp://mh-wowza

The .1 and .2 at the end of the keys means that there are two separate sets of encryption keys configured. You can have as many as you want that will match different URLs.

* The **key** property: defines the [SHA-256 HMAC](http://en.wikipedia.org/wiki/Hash-based_message_authentication_code) secret key to be used to encrypt the policy of the url signing. In the example the key "0123456789abcdef" will be used to encrypt the policy for this url.

* The **id** property is what will let the streaming server know which encryption key to use to verify that it is a correctly signed URL. In the example "theId" will be sent to the streaming server to know which key to use so the same key / id pair must be configured on the streaming server.

* The **url** property defines the beginning of the urls to sign with this encryption key. In the example the first key will be used for all urls that start with "http://mh-wowza" and the second encryption key will be used for all urls that start with "rtmp://mh-wowza".

The configuration file:
    `org.opencastproject.security.urlsigning.provider.impl.GenericUrlSigningProvider.properties`
is designed to sign URLs that protect the REST endpoints on an Opencast node, such as an all-in-one, admin, worker, presenter etc., or to protect content on a download server such as using the Apache Httpd stream security plugin. The input URLs will be used as is, without manipulating them in any way so a request to `http://admin/files/collection/composer/video.mp4` would use that URL for the signing.

The configuration file:
    `org.opencastproject.security.urlsigning.provider.impl.WowzaUrlSigningProvider.properties`
is designed to provide signed urls to protect content on a Wowza streaming server and alters the URL in a way that makes it compatible with the Wowza stream security plugin.

## Verifying that Matterhorn URL Signing is Working
## Getting Signed URL

### Creating Signed URL with Signing Endpoint

1. Go to the signing endpoint at: [http://localhost:8080/signing/docs](http://localhost:8080/signing/docs)

1. Make sure that your URL is supported by using the "accepts" endpoint and entering your media URL that is something like:
`rtmp://streamingserver.tld/matterhorn-engage/mp4:engage-player/1a24ca82-ba8a-4030-8d26-65b7f91fa306/397c8689-9c18-4a14-b5a6-ef8aed6a5471/short` or `rtmp://streamingserver.tld/matterhorn-engage/sample.mp4`

1. Either put the URL into the testing box or go to [http://localhost:8080/signing/accepts?baseUrl=rtmp://streamingserver.tld/matterhorn-engage/sample.mp4](http://localhost:8080/signing/accepts?baseUrl=rtmp://streamingserver.tld/matterhorn-engage/sample.mp4)

1. Next get the url signed by entering the URL into the baseUrl text box and put the Unix Epoch time that this url should expire into the validUntil box (You can find the Unix Epoch value easily from this website: [http://www.epochconverter.com](http://www.epochconverter.com), the value should be input in seconds).

1. Optionally put another Unix Epoch seconds into the validFrom when the url will become available.

1. Optionally put an ip address into the ipAddr text box that will be the only ip that can view the video.

1. Hit the submit button and it will return the signed URL such as:

    ```
    rtmp://streamingserver.tld/matterhorn-engage/sample.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVHcmVhdGVyVGhhbiI6MTQyNTA4NDM3OTAwMCwiRGF0ZUxlc3NUaGFuIjoxNDI1MTcwNzc3MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9LCJSZXNvdXJjZSI6InJ0bXA6XC9cL3N0cmVhbWluZ3NlcnZlci50bGRcL21hdHRlcmhvcm4tZW5nYWdlXC9zYW1wbGUubXA0In19&keyId=demoKeyOne&signature=6e5adff77f84a47c5c904d16609a28a359df05a0c08aeaba6c27e6dc85fabe42
    ```

### Creating Signed URL with Search Service

1. Setup a UrlSigningService such as GenericUrlSigningProvider or WowzaUrlSigningProvider.
1. Process a recording
1. Go to the Search documentation at: http://localhost:8080/search/docs
1. Use the http://localhost:8080/search/episode.json or http://localhost:8080/search/episode.xml to get the search results
1. Find one of the signed urls that look like

    ```
    rtmp://streamingserver.tld/matterhorn-engage/sample.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVHcmVhdGVyVGhhbiI6MTQyNTA4NDM3OTAwMCwiRGF0ZUxlc3NUaGFuIjoxNDI1MTcwNzc3MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9LCJSZXNvdXJjZSI6InJ0bXA6XC9cL3N0cmVhbWluZ3NlcnZlci50bGRcL21hdHRlcmhvcm4tZW5nYWdlXC9zYW1wbGUubXA0In19&keyId=demoKeyOne&signature=6e5adff77f84a47c5c904d16609a28a359df05a0c08aeaba6c27e6dc85fabe42
    ```

## Protecting REST Endpoints That Serve Files With URL Signing
Now that we have configured the signing of URLs we can protect the REST endpoints in Opencast that serve files directly to require a signed URL to access them.

Make sure that you have configured keys, keyids and URLs that will match the Opencast hosts that you want to protect in the `org.opencastproject.security.urlsigning.provider.impl.GenericUrlSigningProvider.properties` configuration file.

The first step is to configure the keys that will be used to verify the requests for files. These keys need to have the same id and key value in the configuration file as has been configured in the URL Signing Provider but it doesn't require adding the url again. 

So for example if the provider has been configured to use the key id `demoKeyOne` and key value `6EDB5EDDCF994B7432C371D7C274F` we would protect its REST endpoints by editing the configuration file `org.opencastproject.security.urlsigning.verifier.impl.UrlSigningVerifierImpl.properties` in the `${matterhorn.home}/etc/services/` directory with contents such as:

    # This configuration file is used to configure the verification of signed urls
    # so that they will expire and can either only become available after a certain
    # amount of time or restricted to a particular client IP.
    # There are two values that need to be set for each possible key that will be
    # used to sign urls, the id of the key and key itself. A suffix of numbers, in
    # order, allow the configuration of several keys.

    # There is the encryption key that is the 128 byte key used to sign the url. e.g.6EDB5EDDCF994B7432C371D7C274F
    key.1=6EDB5EDDCF994B7432C371D7C274F

    # There is the id that will identify which key to decode the signature with on the resource provider side. e.g. theId
    id.1=demoKeyOne

The `.1` after the key and id in the configuration file mean that you can have as many keys as required as long as you add them in ascending order (not skipping any numbers or it will stop looking for more). So the next one specified would have `.2` at the end, and then `.3` etc.

The second step is to configure the filter with the endpoints to protect. The configuration file `org.opencastproject.security.urlsigning.filter.UrlSigningFilter.properties` in the `${matterhorn.home}/etc/services/` directory has been pre-configured with regular expressions that match all of the endpoints that serve files so the only step is to turn the filter on by setting the property `enabled` to true. So for example:

    # Enable or disable the UrlSigningFilter.
    enabled=true

    # This configuration file defines the urls that will be protected by the
    # UrlSigningFilter filter. These endpoints will require a signed url by
    # stream security before a file can be downloaded

    # Protects: /files/collection/{collectionId}/{fileName}
    url.regex.1=.*files\/collection\/.*

    # Protects: /files/mediapackage/{mediaPackageID}/{mediaPackageElementID}
    # and Protects: /files/mediapackage/{mediaPackageID}/{mediaPackageElementID}/{fileName}
    url.regex.2=.*files\/mediapackage\/.*

    # Protects: /staticfiles/{uuid} but not /staticfiles/{uuid}/url
    url.regex.3=(?\=(.*staticfiles.*))(?=^(?!.*staticfiles.*url|.*docs.*).*$)(.*)

    # Protects: /archive/archive/mediapackage/{mediaPackageID}/{mediaPackageElementID}/{version}
    url.regex.4=.*archive\/archive\/mediapackage\/.*\/.*\/.*

### Verifying That REST Endpoints Are Protected
If you have configured your REST endpoints to be protected you can test that they need to be signed by going to the REST documentation for the endpoints (e.g. going to http://hostname/archive/docs, http://hostname/files/docs, http://hostname/staticfiles/docs) and trying to access the endpoints that should be protected. If you don't know any of the files for these particular services you can still test with files that don't exist. If you try to access files that don't exist (e.g. http://hostname/files/collection/wrongcollection/missingfile.txt) it should give you a bad request complaining that a stream security query string parameter is missing (policy, signature or keyid) before it will return that the file is missing. Take the same URL to the URL signing service REST documentation at http://hostname/signing/docs and verify that signing is setup correctly by using the `accepts` method to check that it will sign your URL (should return true). Then sign your URL by entering the test URL into the baseUrl parameter, and put the Unix Epoch time that this url should expire into the validUntil box (You can find the Unix Epoch value easily from this website: [http://www.epochconverter.com](http://www.epochconverter.com), the value should be input in seconds). Take the URL from the signing endpoint and it should now let you access the endpoint.

## Configure URL Signing Timeout Values
There are many different services that are configured to sign URLs and this section will explain configuring all of them to expire at different times.

### TrustedHttpClientImpl - URL Signing for Internal Requests
This configuration setting can be used if a URL signing provider is configured for an Opencast host to protect its REST endpoints. For example if the UrlSigningFilter has been enabled on the Admin node in a cluster installation to protect its archive endpoints and a matching URL signing provider has been configured, then the TrustedHttpClientImpl will automatically sign any requests made to a URL that matches the provider's configuration. By default the signed URLs will expire in 60 seconds. This can be changed by setting a value in seconds for the `org.opencastproject.security.internal.url.signing.duration` property in the `config.properties` configuration file. As this is just a request from one Opencast host to another the request should be easily be started in 60 seconds.

### OsgiEventEndpoint - URL Signing for the Admin UI Links
If a URL signing provider is configured the OsgiEventEndpoint will sign the links in the details of an event. By default the URLs that are protected in the Admin UI will remain valid for 2 hours. This can be changed by setting a value in seconds for `url.signing.expires.seconds` in the `org.opencastproject.adminui.endpoint.OsgiEventEndpoint.properties` configuration file.

### ToolsEndpoint - URL Signing for Preview and Editor Files
If a URL signing provider is configured the ToolsEndpoint will sign the links for media that is accessed through the preview and editor interfaces in the Admin UI. By default these URLs will remain valid for 2 hours. This can be overriden by setting a value in `url.signing.expires.seconds` in the `org.opencastproject.adminui.endpoint.ToolsEndpoint.properties` configuration file.

### SigningMediaPackageSerializer - URL Signing for the Search Service
The SigningMediaPackageSerializer transparently signs all URLs that have a working URL signing provider configured. It is used by the search service to sign media that will be accessed by the player. By default, the signed URLs remain valid for 2 hours. This can be changed by setting a value for `url.signing.expires.seconds` in `org.opencastproject.security.urlsigning.SigningMediaPackageSerializer.properties` configuration file.



Server Plugins
==============

Stream Security only works if the server has the corresponding plugin installed. Currently, plugins for Wowza Streaming Server and Apache HTTPd are available. They can be found in their respective Git repositories:

* Wowza: [https://bitbucket.org/entwinemedia/wowza-stream-security-plugin](https://bitbucket.org/entwinemedia/wowza-stream-security-plugin)
* Apache HTTPd: [https://bitbucket.org/entwinemedia/apache-httpd-stream-security-plugin](https://bitbucket.org/entwinemedia/apache-httpd-stream-security-plugin)



## Downloading A Resource From Apache Httpd

Once you have acquired your signed URL, a video, audio or other resource can be downloaded by putting your signed URL into a browser for testing.


## Playing Wowza Signed URL

The Wowza log files by default located at /usr/local/WowzaStreamingEngine/wowzastreamingengine_access.log and /usr/local/WowzaStreamingEngine/wowzastreamingengine_error.log will allow you to see if a request has been rejected so while testing it is useful to watch these log files for potential issues.

For example if the policy doesn't match the signature you will get an error message such as:
2015-03-19    15:20:28    CDT    comment    server    WARN    200    -    Forbidden because policy and signature do not match. Policy: '{"Statement":{"Condition":{"DateLessThan":1584649115000},"Resource":"sample.mp4"}}' created Signature from this policy '9c6e42c2df91f7d9837813ba9d057d87b42f4addccd8b49d1ddf515acd01369e' and query string Signature: '577e05cdf7e37e3e04da9e60b497cf3a66cac9f54ece16b4f0639266a5ab1bea'


### Testing Signed URL with Wowza Player

1. Get a Signed URL With the Steps Above

1. Go to: [http://www.wowza.com/resources/3.5.0/examples/LiveVideoStreaming/FlashRTMPPlayer/player.html](http://www.wowza.com/resources/3.5.0/examples/LiveVideoStreaming/FlashRTMPPlayer/player.html)

1. Break the url into the host/application stream and the stream location. For the above example it breaks down into
    * Server URL: `rtmp://streamingserver.tld/matterhorn-engage/`
    * Stream URL: `sample.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVHcmVhdGVyVGhhbiI6MTQyNTA4NDM3OTAwMCwiRGF0ZUxlc3NUaGFuIjoxNDI1MTcwNzc3MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9LCJSZXNvdXJjZSI6InJ0bXA6XC9cL3N0cmVhbWluZ3NlcnZlci50bGRcL21hdHRlcmhvcm4tZW5nYWdlXC9zYW1wbGUubXA0In19&keyId=demoKeyOne&signature=6e5adff77f84a47c5c904d16609a28a359df05a0c08aeaba6c27e6dc85fabe42`

1. Put the server url into the server text box and the stream url into the stream box and try playing the video by hitting connect


### Testing Signed URL with VLC

VLC will cut off the end of an extra long URL so if your server name and path is extra long you might notice in the Wowza logs that the resource URL being requested is shorter than the entire path. So for example instead of the resource being requested being:

    rtmp://streamingserver.tld/matterhorn-engage/sample.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVHcmVhdGVyVGhhbiI6MTQyNTA4NDM3OTAwMCwiRGF0ZUxlc3NUaGFuIjoxNDI1MTcwNzc3MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9LCJSZXNvdXJjZSI6InJ0bXA6XC9cL3N0cmVhbWluZ3NlcnZlci50bGRcL21hdHRlcmhvcm4tZW5nYWdlXC9zYW1wbGUubXA0In19&keyId=demoKeyOne&signature=6e5adff77f84a47c5c904d16609a28a359df05a0c08aeaba6c27e6dc85fabe42

It is instead:

    rtmp://streamingserver.tld/matterhorn-engage/sample.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVHcmVhdGVyVGhhbiI6MTQyNTA4NDM3OTAwMCwiRGF0ZUxlc3NUaGFuIjoxNDI1MTcwNzc3MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9LCJSZXNvdXJjZSI6InJ0bXA6XC9cL3N0cmVhbWluZ3NlcnZlci50bGRcL21hdHRlcmhvcm4tZW5nYWdlXC9zYW1wbGUubXA0In19&keyId=demoKeyOne&signature=6e5adff77f


1. Open VLC
1. Choose open network
1. Paste the signed url into the URL text box
1. Click on Open
