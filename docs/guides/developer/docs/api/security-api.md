# Introduction

Opencast is distributing encoded media to download and streaming servers to make that media available to end users. At the same time, that media needs to be protected such that - once provided a link to the download and/or streaming representations - only authorized users are able to consume it.

This is achieved by handing signed URLs to end users which are validated by the distribution servers and become invalid after a given period of time (usually 1 hour, depending on the server configuration).

As a consequence, users of the API who are presenting URLs to distributed media for playback will need to make sure that those urls are signed, otherwise the distribution servers will refuse to deliver the content and respond with a `401 NOT AUTHORIZED` status.

## Best practices

The use of signed URLs requires a set of best practices to be followed when clients interact with the API, most notably in the area of performance and caching.

### Performance

When consuming URLs that need to be signed before handing them to the user, client implementors may be inclinded to use the `sign=true` parameter for the events queries to request all URLs to be already signed. On one hand, this saves the client implementation from having to explicitly sign those URLs that users are visiting for playback. On the other hand, signing URLs introduces an overhead to performance for the pre-signing of all urls that are sent to the client, so in these cases it will be important to make sure not to transfer large lists *and* require presigning.

### Caching

One obvious caveat when using pre-signed URLs is the use of cached responses. As described above, signed URLs have a maxmimum life time and therefore need to be refreshed on a regular basis so that a user's request to play back a recording won't be rejected by the distribution servers.

### Secure access by source IP

The signing facility of the security API provides the ability to sign URLs and restrict that URL to a given IP address.

Even though this greatly increases security in sense that signed URLs can only be accessed from that device, it is important to note that in many network setups, source IP addresses of network packets will undergo network address translation (NAT) with NAT replacing the original source address from private networks with a single public address, thereby diminishing the security impact of adding the source IP address immensely.

# URL Signing

### POST /api/security/sign

Returns a signed URL that can be played back for the indicated period of time, while access is optionally restricted to the specified IP address.

Form Parameters            |Type            | Description
:--------------------------|:---------------|:----------------------------
`url`                      | String         | The linke to encode. **required**
`valid-until`              | date           | Until when is the signed url valid
`valid-source`             | ip             | The IP address from which the url can be accessed


__Response__

`200 (OK)`: The signed URL is returned.<br/>
`401 (NOT AUTHORIZED)`: The caller is not authorized to have the link signed.

```
{
  "url": "http://opencast.org/video.mp4?valid-until=2015-03-11T13:23:51Z&keyId=default&signature=lsjhdf67tefj3",
  "valid-until": "2015-03-11T13:23:51Z"
}
```
