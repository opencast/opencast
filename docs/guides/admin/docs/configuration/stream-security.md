Configuration of Stream Security
================================

To get an introduction to stream security before deploying, please read the overview at:

* [Stream Security Overview](../modules/stream-security.md)


It is important to note that if stream security is enabled, all resources will be signed and protected, even ones that
do not have any access restrictions defined in their access control lists. Accessing resources with unsigned URLs will
not be possible.

On a high level, to use Stream security, these steps are required:

* Install and configure the URL signing service and signing providers
* Configure Opencast services (and, optionally, 3rd party services) that use the signing infrastructure to sign requests
* Install and configure verification components

URL Signing Service Installation
--------------------------------

There are three modules that are built by default and need to be present on each Opencast node in order to initiate URL
signing:

* urlsigning-common
* urlsigning-service-api
* urlsigning-service-impl

If these modules are present, the URL signing service will be available, to which the URL signing providers can then
register themselves.


Minimal Configuration Example
-----------------------------

This is a minimal configuration example which requires valid tokens for all static file downloads:

`etc/org.opencastproject.security.urlsigning.filter.UrlSigningFilter.cfg`:

```properties
enabled=true
url.regex.files=.*localhost:8080/static/.*
```

`etc/org.opencastproject.security.urlsigning.provider.impl.GenericUrlSigningProvider.cfg`

```properties
key.default.secret=THISISNOSECUREKEY
key.default.url=http://localhost:8080/static/
```

`etc/org.opencastproject.security.urlsigning.verifier.impl.UrlSigningVerifierImpl.cfg`:

```properties
key.default=THISISNOSECUREKEY
```


Configuration of Signing Providers
----------------------------------

The GenericUrlSigningProvider that comes with Opencast has its own configuration file:

    etc/org.opencastproject.security.urlsigning.provider.impl.GenericUrlSigningProvider.cfg

All signing providers follow the same configuration structure and support multiple configuration blocks, providing the
settings for separate distributions (i.e. download or streaming servers, services or paths).

Each signing key configuration consists of the following attributes:

* **Key ID:** Key identifier, e.g. `demoKeyOne`
* **Key secret:** Key value, e.g. `25DA2BA549CB62EF297977845259A`. The key-length is not predefined, but a key length of
  at least 128 bit is recommended. Any larger value will not increase security of the underlying algorithm
* **URL prefix:** The URL signing provider will only sign URLs that start with this value. This allows to support
  multiple distributions and different key pairs
* **Organization:** Keys can be restricted to organizations so that different organizations use different keys.
  This attribute is optional. If not specified, the key can be used by all organizations

A typical configuration looks like this:

    key.demoKeyOne.secret=6EDB5EDDCF994B7432C371D7C274F
    key.demoKeyOne.url=http://download.opencast.org/engage

    key.demoKeyTwo.secret=6EDB5EDDCF994B7432C371D7C274F
    key.demoKeyTwo.url=http://download.opencast.org/custom
    key.demoKeyTwo.organization=mh_default_org

It is also possible to use one key for multiple URL prefixes:

    key.demoKeyThree.secret=6EDB5EDDCF994B7432C371D7C274F
    key.demoKeyThree.url.http=http://download.opencast.org/custom
    key.demoKeyThree.url.https=https://download.opencast.org/custom
    key.demoKeyThree.url.streaming=http://streaming.opencast.org/custom
    key.demoKeyThree.organization=mh_default_org


A Java regular expression can be defined to identify URLs to be excluded from URL signing.
Any URL that matches this anchored regex will not be signed.

    exclude.url.pattern=.*/.*/unprotected/.*/.*


Configuration of URL Signing Timeout Values
-------------------------------------------

Once stream security is turned on by configuring the signing providers, multiple different services within Opencast will
be signing URLs, and while some services are signing on behalf of administrative users working in the Opencast
administrative user interface, others are signing URLs in order to grant access to learners playing back video content
i.e. the functionality we have been talking about up to now.

This section explains how to best configure URLs to ensure that they expire at the right time. This might be required if
the default valid times do not seem secure enough or is more secure than needed.

### Signing for external access

The lifetime of the signed URLs can be configured by setting a custom value for the property
`url.signing.expires.seconds` that defines the validity in seconds. The default valid time is 7200 seconds (2 hours).
The signed URLs can also be configured to restrict access to the user’s IP address by setting the property
`url.signing.use.client.ip` to true. By default this is disabled.

Overview of configuration files for services that are able to automatically sign URLs on behalf of users:

|URLs That Are Signed     |Configuration File Name                                                    |
|-------------------------|---------------------------------------------------------------------------|
|Video player content     | org.opencastproject.security.urlsigning.SigningMediaPackageSerializer.cfg |
|Admin UI links           | org.opencastproject.adminui.endpoint.OsgiEventEndpoint.cfg                |
|Preview and editor files | org.opencastproject.adminui.endpoint.ToolsEndpoint.cfg                    |

The URLs will be signed by the first signing provider that will accept the URL’s path based upon the signing provider’s
configuration. This makes it flexible to support many different scenarios. For example, we could configure the signing
provider to have one key for any URL that begins with one scheme, such as http, which would cover all of the URLs to be
signed with a single key. Or it could be configured so that each different scheme and hostname pair would have a
different keys protecting each host’s URLs separately etc. Having the timing configurations separate from the key
configuration allows the different types of URLs to be signed differently depending on the needs of the users without
needing to configure this timing for all of the different keys.

### Signing for Opencast-internal access

Signing of requests for internal use is performed by a core component called *TrustedHttpClientImpl*, which is used to
establish all internal HTTP connections. More specifically, the HTTP client needs access to internal storage areas such
as the working file repository as well as to distributed artifacts on the downloads and streaming servers, all of which
are protected by verification components.

The default expiration time for signed internal requests is 60 seconds. This can be changed by setting a value in
seconds for the `org.opencastproject.security.internal.url.signing.duration` property in the `custom.properties`
configuration file. Since those URLs are signed right before the request is made, the valid time of 60 seconds should be
sufficiently long.

Configuration of Verification Components
----------------------------------------

The verification components ensure that only valid and correctly signed URLs are accessible at any given time. URLs
which are not properly signed or have expired will be rejected.

Out of the box, Opencast provides an internal verification component:

* Opencast internal UrlSigningFilter

The following section is dedicated to the installation and configuration of the Opencast internal UrlSigningFilter. The
stream security architecture allows the implementation for URL verification for third-party applications which are not
covered in this documentation.

### Configuration of Opencast verification filter

The Servlet filter providing the verification of requests to Opencast internal resources is implemented in the bundles:

* urlsigning-verifier-service-api
* urlsigning-verifier-service-impl

The filter uses a set of regular expressions to determine which requests to an Opencast instance need to be verified.

#### Installation

The bundles are built by default and as soon as they are running in Opencast, the filter is active, and ready to be
enabled.

#### Configuration

Two things need to be configured for the Opencast verification filter:

* key pairs used to verify the signatures
* paths and endpoints that need to be protected

The configuration is located at:

    etc/org.opencastproject.security.urlsigning.verifier.impl.UrlSigningVerifierImpl.cfg

Example:

    key.demoKeyOne=6EDB5EDDCF994B7432C371D7C274F

    key.demoKeyTwo=C843C21ECF59F2B38872A1BCAA774

The entries in this file need to have the same values for the signing providers configuration.

The second step is to configure the filter defining the endpoints to be protected. The configuration file is located at:

    etc/org.opencastproject.security.urlsigning.filter.UrlSigningFilter.cfg

The configuration defaults to a set of regular expressions which match all of the endpoints that serve files, and avoid
protecting endpoints that only serve data. Therefore, the remaining step is enabling the filter by setting the property
`enabled` to `true` and determining whether strict or non-strict verification of the resource is required.

Note that strict verification of resources means the entire URL will be considered when comparing the incoming request
for a resource against the policy, including the scheme (http, https, etc.), hostname and port. If turned off, only the
path to the resource will be considered. So if the resource `http://httpdserver:8080/the/full/path/video.mp4` is
requested, only the `/the/full/path/video.mp4` part of the URL will be checked against the policy’s path. As mentioned
before, this is useful when using a load balancer so that the requested host name does not have to match the actual
hostname or if a video player is rewriting requests, e.g. by inserting the port number.

Example:

    enabled=true

    strict=true

    url.regex.collection=.*files\/collection\/.*
    url.regex.mediapackage=.*files\/mediapackage\/.*
    url.regex.staticfiles=(?\=(.*staticfiles.*))(?=^(?!.*staticfiles.*url|.*docs.*).*$)(.*)
    url.regex.archive=.*archive\/archive\/mediapackage\/.*\/.*\/.*
    url.regex.static=.*static.*

Testing
-------

Once all components of Stream Security are installed and properly configured, it is important to verify that the system
is working as expected. It is especially important to try to access resources that should *not* be accessible.

There are ways to test in a structured way which will be explained below.

### Creating Signed URLs with Signing Endpoint

The signing service provides a REST endpoint, which allows for the signing of arbitrary URLs. For manual use it is
recommended to visit the endpoint’s documentation page at `http://localhost:8080/signing/docs`.

#### Is the URL accepted?

Check if the URL to be signed is accepted by the signing service (or by one of its signing providers respectively) by
using the `/signing/accepts` endpoint. If that is not the case, the configuration of the signing providers should be
checked again to ensure that at least one signing provider is responsible for the URL in question.

If the service is fully operational, the response code will be *200 OK* and the response body either *true* (accepted)
or *false* (refused).

### Signing the URL

On the same documentation page URLs can be signed using the `/signing/sign` endpoint, and the access policy may be
specified in that form as well. With this, several scenarios can be tested. Examples are:

* URLs that have already expired or will expire at a known date
* URLs that are not yet valid (if you provided a validFrom data in the access policy)
* URLs that are missing some or all of the signing parameters (policy, keyId or signature)
* URLs that are attempting to use signing parameters (policy and signature) from a different signed URL

### Verifying the URL

The signed URLs can then be passed to the appropriate testing tool (web browser, cURL, player, …) to test the
functionality of the verification component(s). The following table is the return codes associated with different
rejection conditions:

| Case                                                                                        | Return Code      |
| ------------------------------------------------------------------------------------------- | ---------------- |
| If any of the query string parameters are missing or are the wrong case / spelt incorrectly | Bad Request (400)|
| If any of the required policy variables are missing                                         | Bad Request (400)|
| No encryption key that matches the KeyID known by the plugin                                | Bad Request (400)|
| The Policy and Signature don’t match in any way                                             | Forbidden (403)  |
| If client IP is specified and doesn’t match                                                 | Forbidden (403)  |
| The current time has passed the DateGreaterThan, the time the URL expires                   | Gone (410)       |
| The current time is before the DateLessThan, the time the URL becomes available             | Gone (410)       |

The components that verify a URL is signed will run before a request is checked to be valid, so if a non-existent URL is
signed for example, the above conditions will need to be fixed before a missing (404) response code will be returned.

### Inspect policy

The generated policy which is added to the signed URLs can be inspected. It needs to be decoded from Base64 and the
result must be a JSON document that contains exactly the values which have been passed during signing.

Decoding this Base64 encoded policy

    eyJTdGF0ZW1lbnQiOnsiUmVzb3VyY2UiOiJodHRwOlwvXC9vcGVuY2FzdC5vcmdcL2VuZ2FnZVwvcmVzb3VyY2UubXA0IiwiQ29uZGl0aW9uIjp7IkRh
    dGVMZXNzVGhhbiI6MTQyNTE3MDc3NzAwMCwiRGF0ZUdyZWF0ZXJUaGFuIjoxNDI1MDg0Mzc5MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9fX0

…would result in this JSON document (policy):

```json
{
  "Statement":{
    "Resource":"http:\/\/opencast.org\/engage\/resource.mp4",
    "Condition":{
      "DateLessThan":1425170777000,
      "DateGreaterThan":1425084379000,
      "IpAddress":"10.0.0.1"
    }
  }
}
```

Inspecting and modifying the policy is useful for advanced testing, such as:

* URLs where the policy was modified after signing
* URLs where the policy was modified and resigned with a different key

## Further information

For an overview of Stream Security:

* [Stream Security Overview](../modules/stream-security.md)

For further developer information, please have a look at the stream security section in the developer guide.
