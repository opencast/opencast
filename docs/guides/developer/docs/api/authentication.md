
[figure_2]: media/img/figure_2.png "Figure 2: Authentication and authorization based on Basic Authentication"

# Introduction

The External API’s security layer is designed to support a multitude of mechanisms for authentication such as API
keys, digest authentication and others. While the current implementation only supports basic authentication, further
authentication mechanisms may be added in the future.

# Basic Authentication
The External API is protected by basic authentication, requiring a user and a password be sent in the form of the
standard HTTP `Authorization` header. (see [Figure 2](#figure_2)). In the header, the username and password are sent
encoded in Base64 format. The incoming requests are matched against an existing user whose password needs to match with
the one that is found in the Authorization request header.

NOTE: Basic authentication is not activated by default, please activate it in the security settings
(`etc/security/mh_default_org.xml`) before using the External API.

<center>
![][figure_2]
</center>
<a name="figure_2"></a>Figure 2: Authentication and authorization based on Basic Authentication

## Protection of authentication data
Since Base64 is by no means regarded as encryption in the sense of security, it is strongly recommended to only offer
access to the External API over HTTPS rather than over HTTP in order to avoid unprotected transmission of the
username and password via the Authorization header.


## Validation criteria
When initiating the connection, the External API analyzes the header and extracts the username to match against an
existing API user. If that user is found, the connection is authenticated, otherwise it’s rejected.
