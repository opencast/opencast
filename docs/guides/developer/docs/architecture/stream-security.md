# Stream Security Developer Guide

To get an introduction to Stream Security, please read the sub section Stream Security in the section Modules of the
Admin Guide.

## Opencast Signing Protocol

The Signing Providers as well as the verification components that are developed by the Opencast community implement the
policy and signature specified in the Opencast Signing Protocol.

### Policy
The policy is a Base64 encoded JSON document. A human-readable version of the JSON document looks like this:

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

|Property Name|Property Description|
|------|-----|
|**Resource**| URL of the resource, must exactly match the requested URL including the schema. In case of a RTMP request, this is only the resource path, without the RTMP application name or the server.|
|**DateLessThan**| Unix epoch that a resource should expire on in milliseconds|
|DateGreaterThan | Unix epoch that a resource should become available in milliseconds|
|IpAddress| Client's IP address that will be accessing the resource|

Properties in bold are mandatory.

Before the JSON document is Base64 encoded, all whitespaces need to be removed. The above sample document would then
look like this:

```json
{"Statement":{"Resource":"http:\/\/opencast.org\/engage\/resource.mp4","Condition":{"DateLessThan":1425170777000,"DateGreaterThan":1425084379000,"IpAddress":"10.0.0.1"}}}
```

The Base64-encoding must be performed in a URL safe way which means that instead of using the characters ‘+’ and ‘/’
they have to be replaced by '-' and '_' respectively. The example above would be encoded into:

    eyJTdGF0ZW1lbnQiOnsiUmVzb3VyY2UiOiJodHRwOlwvXC9vcGVuY2FzdC5vcmdcL2VuZ2FnZVwvcmVzb3VyY2UubXA0IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTQyNTE3MDc3NzAwMCwiRGF0ZUdyZWF0ZXJUaGFuIjoxNDI1MDg0Mzc5MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9fX0=
The encoded policy must be sent to the server as a query parameter named ‘policy’, e.g.

    http://opencast.org/engage/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiUmVzb3VyY2UiOiJodHRwOlwvXC9vcGVuY2FzdC5vcmdcL2VuZ2FnZVwvcmVzb3VyY2UubXA0IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTQyNTE3MDc3NzAwMCwiRGF0ZUdyZWF0ZXJUaGFuIjoxNDI1MDg0Mzc5MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9fX0

Note: Be aware that Base64 encoding can have up to two ‘=’ characters at the end of the string to pad a message to a
necessary length divisible by 3. All components should be able to handle Base64 encoded strings with or without this
padding (Resources signed by Opencast will have the padding characters URL encoded to ‘%3D’).

### Signature
The signature is a hash-based message authentication code (HMAC) based on a secret key. The algorithm used is
HMAC-SHA-256. Only the encoded policy needs to be taken as input for the hash-calculation.

The keys used are simple character strings without any special format. It could be something like ‘AbCdEfGh’, but it’s
recommended to use a key with a length of 256 bit like ‘2195265EE84ED1E1324D31F37F7E3’. Each key must have a unique
identifier, e.g. ‘key1’. In this example, the following key has been used:

Key ID: demoKeyOne Secret Key: 6EDB5EDDCF994B7432C371D7C274F

The HMAC for the signature from the previous section calculated based on the *demoKey1* is

    c8712284aabc843f76a132a3a7c8997670414b2f89cb96b367d5f35d0f62a2e4

The signature must also be sent as a query parameter that forms part of the resource request. The example from above
would now look like this:

    http://opencast.org/engage/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiUmVzb3VyY2UiOiJodHRwOlwvXC9vcGVuY2FzdC5vcmdcL2VuZ2FnZVwvcmVzb3VyY2UubXA0IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTQyNTE3MDc3NzAwMCwiRGF0ZUdyZWF0ZXJUaGFuIjoxNDI1MDg0Mzc5MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9fX0&signature=c8712284aabc843f76a132a3a7c8997670414b2f89cb96b367d5f35d0f62a2e4

The same is true for the key id, which needs to be included to determine which key was used to create the signature.

    http://opencast.org/engage/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiUmVzb3VyY2UiOiJodHRwOlwvXC9vcGVuY2FzdC5vcmdcL2VuZ2FnZVwvcmVzb3VyY2UubXA0IiwiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTQyNTE3MDc3NzAwMCwiRGF0ZUdyZWF0ZXJUaGFuIjoxNDI1MDg0Mzc5MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9fX0&signature=c8712284aabc843f76a132a3a7c8997670414b2f89cb96b367d5f35d0f62a2e4&keyId=demoKeyOne

## Signing URLs from a 3rd party system
URL signatures also need to be issued for resources presented on and linked from a third party system (such as a custom
video portal). There are two options for signing 3rd party system URLs:

### Option #1: Use the existing URL Signing Service**

If the third party system is based on Java, the existing URL Signing bundles/JARs can be reused. They do not have
dependencies to other parts of Opencast and can therefore be used independently.

These bundles are required:

* urlsigning-common
* urlsigning-service-api
* urlsigning-service-impl

Code example:
```java
private UrlSigningService urlSigningService;

/** OSGi DI */
void setUrlSigningService(UrlSigningService service) {
  this.urlSigningService = service;
}

…

String urlToSign = “http://my.custom.url/with/path.mp4”;
long signedUrlExpiresDuration = 60;

if (urlSigningService.accepts(urlToSign)) {
  try {
    String signedUrl = urlSigningService.sign(
        urlToSign,
        signedUrlExpiresDuration,
        null,
        null);
    ...
  } catch (UrlSigningException e) {
    // handle exception
  }
}
```

### Option #2: Create custom URL Signing Service

Based on the technical details outlined in the Opencast Signing Protocol, a URL Signing Service that is compatible with
the other existing parts of the Stream Security system can be implemented.

### Option #3: Give Access to Third Party Systems to Signing REST Endpoints
Opencast servers that have been configured to use URL signing service will have two REST endpoints at
http://admin.opencast.edu:8080/signing/docs. The accepts endpoint will return true if the Opencast server can sign a
particular URL. The sign endpoint will return a signed URL when the correct parameters are given. Due to the sensitive
nature of these endpoints they are locked down to be only accessible by a user with ROLE_ADMIN privileges in the
etc/security/mh_default_org.xml configuration file. Creating a new user with this role and accessing the endpoint using
these credentials will allow a third party system to sign any URLs.

## Further information

* For an overview of Stream Security, please consult the sub section Stream Security in the section Modules of the Admin
  Guide.
* For information about how to configure stream security on your Opencast servers, please consult the sub section Stream
  Security in the section Configuration of the Admin Guide
