## Why do we need to further protect recordings?

The usual way to access media files processed by Opencast is the Engage Media Module. It comes with built-in authorization management which means that no user can see any recording which he/she is not allowed to view. So why do we need to further protect resources and streams? There are several possibilities how someone could get access to restricted content:

* Someone that is authorized to view a recording might just copy the link to the A/V files and give it to a friend or even publish it to a social network.

* Some of the URLs can be guessed because they are composed of known values like the series name and the recording ID.

* Special kinds of recordings may be only access for a certain time period. But if a user has saved the direct URL to the A/V content, it remains accessible as long as it is available on the server.

The reason for this is that the actual A/V files are stored on a separate HTTP or streaming server. Because those servers usually have no connection to Opencast and therefore cannot enforce any user or role based access restrictions, it is common practice to not protect the files at all and just make them publicly available.


## What is URL signing?

Generally speaking, signing a URL is nothing more than adding a digital signature which allows another server to verify if the URL (including its parameters) has been changed since it was signed. Using signed URLs offers new possibilities: It allows you to send information that must not be modified as part of the request and verify that it is unchanged.

Part of this unchangeable data could be an access policy that defines during which period of time a resource may be accessed. By signing such a URL, you can make sure that no-one is able to modify that policy. With this we can create URLs that are only valid for a defined period of time and we can even limit access to it to certain IP addresses.


## What is secured with Stream Security and what is not?

Having a policy which defines rules for accessing the resource solves some but not all of the problems. That is why it is very important to be aware of what is protected and what not. On a high level, this is what signing URLs brings you in terms of access security:

* **Every unsigned URL is rejected**: Even if someone guesses the proper URL path to a resource the content won’t be served as the signature is missing (or not valid).

* **URLs have a limited availability**: Depending on the use case the period of availability can be configured to be as small as a few seconds, as long as several months or even years.

* **Access to URLs can be limited by IP address**: Trying to access the resource from another IP address won’t be possible anymore.


Even with Stream Security enabled, some things remain unprotected. If the following limitations are not acceptable for your use case, you might need to look around for additional solutions to protect your content.

* **No DRM!** Protecting access to resources with signed URLs cannot be compared to Digital Rights Management. If a user had access to a resource and made a local copy of it, there is no way to restrict further playback or even redistribution of that media.

* **No further control during period of validity**: During its validity, there is no way of enhanced access control management. Valid URLs can be copied and shared. A short availability period minimizes the risk of abuse.

## Further information

For further technical information like installation instructions, configuration guides, server plugins and the signing specification, please have a look at these documents:

* [Configuration & Testing](../configuration/stream-security.md)
* [URL Specification](http://docs.opencast.org/latest/developer/stream-security-insights)
* [Wowza plugin](https://bitbucket.org/entwinemedia/apache-httpd-stream-security-plugin)
* [Apache HTTPd plugin](https://bitbucket.org/entwinemedia/apache-httpd-stream-security-plugin)
