URL Specification
=================

The streaming URLs are secured using 3 query string parameters added onto the usual URL path. There is the:

* Policy - Configures what resource is being requested and what the terms are for securing that resource
* Signature - An encrypted version of the Policy to verify that the request is valid (hasn't been altered or on a different resource)
* KeyID - The unique id for the encryption key to use

#### Policy

* It is in JSON format
* It is Base 64 encoded
* All whitespace characters must be removed
* Contains the following properties
     * Resource - (Required) - The base url of the resource being requested (without the signed query string parameters)
     * DateLessThan - (Required) - Milliseconds since the Unix epoch when this resource will expire. If missing will return forbidden.
     * DateGreaterThan - (Optional) - Milliseconds since the Unix epoch when this resource will become available
     * IpAddress - (Optional) - The IP address that should match the client's IP address

It will have the following JSON structure (Without the //Optional comments):

    {
      "Statement": {
        "Resource":"http://mh-allinone.localdomain/engage/url/to/stream/resource.mp4",
        "Condition":{
          "DateLessThan":1425170777000,
          "DateGreaterThan":1425084379000, // Optional
          "IpAddress": "10.0.0.1" // Optional
        }
      }
    }


#### Signature

This is an SHA-256 HMAC hashed version of the policy JSON data and then encoded in Hex format.

#### KeyId

The id of the encryption key used to sign this URL.



Process for Creating Signed URL
===============================

## Inputs

To sign a URL you need:

* URL for the resource
* Key id and key secret value that will be used to sign the URL
* The unix epoch that a resource should expire on in milliseconds, known as the *DateLessThan*
* Optional unix epoch that a resource should become available in milliseconds, known as the *DateGreaterThan*
* Optional client's ip address that will be accessing the resource

For the purposes of this documentation we are going to use the following inputs:

* URL: http://mh-allinone.localdomain/engage/url/to/stream/resource.mp4
* Key ID: demoKeyOne
* Key Secret: 6EDB5EDDCF994B7432C371D7C274F
* Valid from: 1425170777000
* Valid until: 1425084379000
* Client IP address: 10.0.0.1



## Create Policy

Create a JSON object without whitespace characters with the following structure and properties filled with the information from the input (DateGreaterThan or IpAddress are optional and can be omitted):

    {
       "Statement":{
          "Resource":"http:\/\/mh-allinone.localdomain\/engage\/url\/to\/stream\/resource.mp4",
          "Condition":{
             "DateLessThan":1425170777000,
             "DateGreaterThan":1425084379000,
             "IpAddress":"10.0.0.1"
          }
       }
    }

Your JSON implementation may or may not escape forward slashes, as both versions are part of the JSON specification they should both be supported by plugins. The final version would look like this:

    {"Statement":{"Resource":"http:\/\/mh-allinone.localdomain\/engage\/url\/to\/stream\/resource.mp4","Condition":{"DateLessThan":1425170777000,"DateGreaterThan":1425084379000,"IpAddress":"10.0.0.1"}}}

To see an example of creating the policy JSON the function "public static JSONObject toJson(Policy policy)" in the class PolicyUtils creates the JSON object.


## Encode Policy using Base64

The next step would be to encode the policy using Base64 with a URL safe format ('Instead of using +' and '/'  characters use  '-' and '_' respectively). The example above would be encoded into:

    eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVHcmVhdGVyVGhhbiI6MTQyNTA4NDM3OTAwMDAwMCwiRGF0ZUxlc3NUaGFuIjoxNDI1MTcwNzc3MDAwMDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9LCJSZXNvdXJjZSI6Imh0dHA6XC9cL21oLWFsbGlub25lLmxvY2FsZG9tYWluXC9lbmdhZ2VcL3VybFwvdG9cL3N0cmVhbVwvcmVzb3VyY2UubXA0In19

This will be the value for the query string parameter "policy" for the signed url. You can use a utility such as https://www.base64encode.org/ or https://www.base64decode.org/ to test that your implementation is creating the correct encoding.

To see an example of encoding the policy using Base64 there is the "public static String toBase64EncodedPolicy(Policy policy)" function in the PolicyUtils class.


## Create Signature From Policy

The signature is created using the SHA-256 HMAC (Spec: https://tools.ietf.org/html/rfc4868, wiki: https://en.wikipedia.org/wiki/Hash-based_message_authentication_code) hashing algorithm using the key secret and the JSON value of the policy (not Base64 encoded) as the message to be hashed. The resulting bytes then must be encoded into Hex format. You can use a tool such as http://www.freeformatter.com/hmac-generator.html to verify that the signature is being created correctly. The above policy example with the input key "6EDB5EDDCF994B7432C371D7C274F" would be hashed into:

    a37d6ba4e5819b2506c7d7e029aa558937cbdc586aa83b97d7c29a79d46cf3bd


## Build Query String For Signed URL

Now that we have the Base64 encoded policy and signature we can create the signed URL. We add to the original URL the policy, signature and key id as query string parameters. So in our example it would become:

    http://mh-allinone.localdomain/engage/url/to/stream/resource.mp4?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVHcmVhdGVyVGhhbiI6MTQyNTA4NDM3OTAwMCwiRGF0ZUxlc3NUaGFuIjoxNDI1MTcwNzc3MDAwLCJJcEFkZHJlc3MiOiIxMC4wLjAuMSJ9LCJSZXNvdXJjZSI6Imh0dHA6XC9cL21oLWFsbGlub25lLmxvY2FsZG9tYWluXC9lbmdhZ2VcL3VybFwvdG9cL3N0cmVhbVwvcmVzb3VyY2UubXA0In19&keyId=demoKeyOne&signature=a37d6ba4e5819b2506c7d7e029aa558937cbdc586aa83b97d7c29a79d46cf3bd

To see an example there is a function "public static String digest(String plainText, String secretKey)" in the SHA256Util class that hashes a message and encodes it into Hex. Now this signed url contains all of the components required for a plugin to verify that a request is correct.


Process for Verifying a Signed URL in a Plugin
==============================================

There is an example of verifying that a request is valid with a signed URL in the "public static ResourceRequest resourceRequestFromQueryString" function in the ResourceRequestUtil class.

1. If any of the query string parameters are missing or are the wrong case / spelt incorrectly return a Bad Request 400.

1. If there are multiple copies of any of the query string parameters return a Bad Request 400.

1. Decode the Base 64 Encoded Policy (Two possible gotchas is that it should support both padded and unpadded Base 64 encoded strings, and make sure that your implementation supports URL safe encoding, "+" replaced by "-" and "/" replaced by "_"). Also verify that your JSON implementation can handle the resource having escaped forward slashes such as `http://mh-allinone.localdomain/engage/url/to/stream/resource.mp4` becoming: `http:\/\/mh-allinone.localdomain\/engage\/url\/to\/stream\/resource.mp4`. Example is in the class PolicyUtils in the function `public static Policy fromBase64EncodedPolicy(String encodedPolicy)`.

1. If any of the required policy variables are missing return a Bad Request 400.

1. If there is no encryption key that matches the KeyID known by the plugin return a Bad Request 400.

1. Using the SHA-256 HMAC algorithm, hash the given Policy. The Policy & Signature must match exactly when encrypted using the secret key. If they don't match return a Forbidden 403. There is an example in the RequestResourceUtil class in the function ` protected static boolean policyMatchesSignature(Policy policy, String signature, String encryptionKey)`.

1. If the client's IP address is specified, check it against the client's IP address, if it doesn't match return a Forbidden 403.

1. Check the dates of the policy to make sure that it is still valid. If it is not currently valid because the link has expired or it is not yet available return a Gone 410.

1. If all of the above conditions pass, then allow the request.
