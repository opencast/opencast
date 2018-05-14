# Security API

## Introduction

Opencast is distributing encoded media to download and streaming servers to make that media available to end users. At
the same time, that media needs to be protected such that - once provided a link to the download and/or streaming
representations - only authorized users are able to consume it.

This is achieved by handing signed URLs to end users which are validated by the distribution servers and become invalid
after a given period of time (usually 1 hour, depending on the server configuration).

As a consequence, users of the External API who are presenting URLs to distributed media for playback will need to make
sure that those urls are signed, otherwise the distribution servers will refuse to deliver the content and respond with
a `401 NOT AUTHORIZED` status.

### Best practices

The use of signed URLs requires a set of best practices to be followed when clients interact with the External API,
most notably in the area of performance and caching.

#### Performance

When consuming URLs that need to be signed before handing them to the user, client implementors may be inclinded to use
the `sign=true` parameter for the events queries to request all URLs to be already signed. On one hand, this saves the
client implementation from having to explicitly sign those URLs that users are visiting for playback. On the other hand,
signing URLs introduces an overhead to performance for the pre-signing of all urls that are sent to the client, so in
these cases it will be important to make sure not to transfer large lists *and* require presigning.

#### Caching

One obvious caveat when using pre-signed URLs is the use of cached responses. As described above, signed URLs have a
maxmimum life time and therefore need to be refreshed on a regular basis so that a user's request to play back a
recording won't be rejected by the distribution servers.

#### Secure access by source IP

The signing facility of the security API provides the ability to sign URLs and restrict that URL to a given IP address.

Even though this greatly increases security in sense that signed URLs can only be accessed from that device, it is
important to note that in many network setups, source IP addresses of network packets will undergo network address
translation (NAT) with NAT replacing the original source address from private networks with a single public address,
thereby diminishing the security impact of adding the source IP address immensely.

## URL Signing

### POST /api/security/sign

Returns a signed URL that can be played back for the indicated period of time, while access is optionally restricted to
the specified IP address.

Form Parameters | Required |Type                                  | Description
:---------------|:---------|:-------------------------------------|:----------------------------
`url`           | yes      | [`string`](types.md#basic)           | The URL to be signed
`valid-until`   | no       | [`datetime`](types.md#date-and-time) | The date and time until when the signed URL is valid
`valid-source`  | no       | [`string`](types.md#basic)           | The IP address from which the url can be accessed

__Response__

`200 (OK)`: A JSON object containing the signed URL or an error message is returned:

Field         | Type                                 | Description
:-------------|:-------------------------------------|:-----------
`url`         | [`string`](types.md#basic)           | The signed URL
`valid-until` | [`datetime`](types.md#date-and-time) | The date and time until when the signed URL is valid

In case of an error:

Field    |Type                       | Description
:------ -|:--------------------------|:-----------
`error`  |[`string`](types.md#basic) | An error message describing the error

`401 (NOT AUTHORIZED)`: The caller is not authorized to have the link signed.

__Example__

```
{
  "valid-until": "2018-03-19T13:08:39Z",
  "url":"http://localhost?policy=eyJTdGF0ZW1lbnQiOnsiQ29uZGl0aW9uIjp7IkRhdGVMZXNzVGhhbiI6MTUyMTQ2NDkxOTI4NH0sIlJlc291cmNlIjoiaHR0cDpcL1wvbG9jYWxob3N0In19&keyId=demoKeyOne&signature=717dd8f958a15c1cdb7e88a61417a07bb6a1e6238d9293805cc0893f798a07e8"
}
```

Error example:

```
{
  "error": "Given URL cannot be signed"
}
```

