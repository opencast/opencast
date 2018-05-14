Stream Security
===============

Introduction
------------

Security usually is a challenging, technically aspects of any software system. However, diving into technical details
before understanding the principles of the solution can lead to false assumptions about the level of security in place.
Therefore, this section provides a high-level overview of Opencast's stream security functionality.


Content Security in Opencast
----------------------------

In many settings, some or even all content published by an Opencast installation must not be accessible by everyone.
Instead, access should be restricted to those users with corresponding permissions. So, if access control already
ensures that each user only has access to the recordings he or she is allowed to see, what does stream security add to
the mix?

Looking more closely at what it means to serve recordings to a viewer reveals that a distinction needs to be made
between:

* the presentation of the video player, the recording metadata
* the serving of the video streams, preview images etc. to that player.

The former is protected by the engage part of Opencast. The latter may be served by download and streaming servers.
Those distribution servers are independent of Opencast and have no knowledge about the current user and its permissions
with regard to the requested video asset .

To summarize: Opencast is capable of assessing a recording’s access control list and the current user’s permissions to
decide if a user is allowed to access the recording’s metadata and the player.  External download and streaming servers,
serving the actual video files are not aware of these permissions. As a result, nothing prevents an authorized user from
passing on the actual video and image URLs to the public, thereby circumventing the restrictions implied on the
presentation layer earlier on.


Securing the Streams
--------------------

Since the download and streaming servers do not (and should not) have access to security related information about the
user, its roles nor its permissions with regard to the media files, there is no way to perform authorization checks the
same way Opencast is performing them while serving up recording metadata. The only way to decide if a given request
should be served or not is to leave authorization to Opencast and agree on a secure protocol that defines whether a
request is meant to be granted by Opencast or not.

Stream security solves the problem exactly as described: Each request that is sent to any of the download or streaming
servers must contain a validation policy, indicating for how long access should be granted and optionally even from
which IP address. Signing of the policy ensures that potential changes to the policy will be detected. On the other end,
the server must be enabled to verify the signature and extract the policy to verify whether it should comply with the
request or not.

What is secured and what is not?
--------------------------------

Even with Stream security enabled, some loopholes exist where unauthorized viewers might be able to get access to
protected resources, even though for a limited time only. The following section describes in detail what is and what
is not secured.

### URL hacking

*Executive summary: Accessing a resource with an unsigned or incorrectly signed URL is impossible.*

Resources distributed by Opencast are organized in a file structure that is built upon a resource’s series identifier as
well as the identifier of the recording itself. Since those identifiers usually are based on
[UUIDs](https://en.wikipedia.org/wiki/Universally_unique_identifier), guessing the URL is hard but not impossible. In
addition, a malicious user might be getting hold of a valid identifier through network sniffing, social hacking or by
other means.

With Stream Security enabled, a user cannot access that resource, since the URL for accessing the resource would either
be lacking the policy and signature completely or would contain a broken signature due to an identifier mismatch in the
policy.

It is important to note that, if stream security is enabled, all resources will be signed and protected, even ones that
do not have any access restrictions defined in their access control lists. Accessing resources with unsigned URLs will
not be possible.

### Revoking access rights

*Executive summary: Access is revoked once the digital signature expires.*

If a user has the rights to access a resource, it does not automatically mean that permission has been granted for a
lifetime. After a signed URL’s policy has expired, the URL must receive an updated policy and be signed again in order
to provide continuous access to the corresponding resource, so in the case of revoked access rights, the user in
question will be able to keep access to the resource as long as the initially signed url is valid. After that, Opencast
will not provide a signed URL anymore due to the change in permissions.

On the other hand, there is no way to revoke access to that resource for that particular user unless the URL expires.
The only way would be to completely remove the resource from the distribution server. It is therefore important to
choose reasonable expiration times for signed URLs.

### Unauthorized sharing of URLs

*Executive summary: Leaked signed URLs are only accessible for the duration of the validity of the signature.*

A signed URL shared by an authorized user with a non-authorized third party will expire (as explained above). The
expiration time can be set as low as some seconds but will then require even authorized users to obtain newly signed
URLs as they continue to access protected content (e.g. the user takes a quick break watching a recording by hitting
“pause”, then hits “play” again to resume). This risk can be lowered further by restricting a resource to a client’s IP
address so that it can only be played by someone with the same IP.

### Downloading or ripping content

*Executive summary: Content protected by stream security is not protected against unauthorized publication through
authorized users.*

Since stream security does not implement digital rights management (DRM), authorized users may download content while in
possession of correctly signed URLs. When that content is republished on systems that are not under the control of the
original owner (i.e. are not protected by stream security or any other means), it is publicly available.

Most institutions will have a policy in place that legally prevents circumventing protection and sharing of protected
media, and as a result, the above scenario will be taxed as piracy.

Technical Overview
------------------

Stream security consists of several components, and each of these components must be installed and configured properly,
otherwise the system may not behave as expected. This part of the documentation describes how each of the components
need to be installed and holds information on which configuration options are available.

### Terms

For the understanding of this document it is important to have the following terms clearly defined.

#### Policy

A policy defines the timeframe and (optionally) from which addresses a specified resource may be accessed. In order to
exchange the policy between system components, the involved components must agree on a serialization specification.

#### Signature

The signature expresses the validity of a policy. As with the policy, the system’s signature components, must follow a
predefined signing algorithm. Only then is it possible to verify if the signature was issued for a specific policy, or
if either the signature or the policy was modified.

#### Key

Using keys is a common way to protect information that is being shared between two or more systems. In stream security,
keys are used to prevent signature forgery. A key consists of an identifier (ID) and a secret value. The keys need to be
kept private, otherwise everyone can create signatures and thereby gain unlimited access to all resource protected by
that key.

#### Signing Protocol

The combination of a policy specification and a signature algorithm forms the signing protocol, where the policy
contains the rules to be applied and the signature ensures that the rules remain unaltered. Components that implement
the same signing protocol are compatible and can be used in combination.

### Components

A typical signing infrastructure consists of two main components: a signing service and a verification component. While
the signing service is used to sign arbitrary URLs, the verification component is located on the distribution servers to
protect the resources and only serve requests that have been properly signed.

All signing providers and verification components developed by the Opencast community implement the Opencast signing
protocol as documented in the developer guide and are therefore compatible.

#### URL Signing Service

The URL signing service is designed to support one or more signing implementations called signing providers. With this
concept, different signing protocols, and by virtue, different verification components are supported. The resource is
presented to each signing provider in turn, where it is either signed or passed on. This process continues until a
signature is obtained.

Out of the box, Opencast provides the following implementation:

* *Generic Signing Provider*: This provider may be used in combination with HTTP servers. It appends the necessary
  information (policy, signature and key id) to the URL.

The URL signing service makes it straightforward to provide additional implementations to handle third party
distribution servers URL signatures. This becomes important in situations where files are served by a server that is
currently not supported or if files are served by a CDN that implements its own proprietary signing protocol.

#### Verification components

In order to take advantage of the signed URLs, a verification component needs to reside on the distribution servers to
verify the validity of the signature (i.e. check that the URL has not been altered after it was signed) and then grant
or deny access to the resource, based on the policy associated with the URL.

In addition to these external verification components there is also an Opencast verification component called the
*UrlSigningFilter* that is used to protect files that Opencast itself provides.

Verification components have the option of strict or non-strict checking. Strict verification of resources means the
entire URL will be considered when comparing the incoming request for a resource against the policy, including the
scheme (http, https, etc.), hostname and port. If using non-strict checking, only the path to the resource will be
considered. So if the request is for a resource at `http://httpdserver:8080/the/full/path/video.mp4`, only the
`/the/full/path/video.mp4` part of the URL will be checked against the policy’s path. This is useful when using a load
balancer so that the requested hostname does not have to match the actual hostname or if a video player is rewriting
requests, e.g. by inserting the port number.

Further Information
-------------------

For further technical information like installation instructions, configuration guides, server plugins and the signing
specification, please have a look at these documents:

* [Stream Security Configuration & Testing](../configuration/stream-security.md)
* The Opencast Signing Protocol is defined in the subsection Stream Security in the modules section of the developer guide.
