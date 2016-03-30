
[figure_2]: media/img/figure_2.png "Figure 2: Authentication and  authorization based on Basic  Authentication"

[figure_3]: media/img/figure_3.png "Authentication  and authorization using SSL certificates"

# Introduction

The Application API’s security layer supports a multitude of mechanisms for authentication such as api keys, digest authentication and others. That being said, the two mechanisms presented below tie in perfectly with the user management infrastructure and therefore the favorable options.

# Basic Authentication
The Application API is protected by basic authentication, requiring a user and a password be sent in the form of the standard HTTP `Authorization` header. (see [Figure 2](#figure_2)). In the header, the username and password are sent encoded in Base64 format.
The incoming requests are matched against an existing user whose password needs to match with the one that is found in the Authorization request header.

<center>
![][figure_2]
</center>
<a name="figure_2"></a>Figure 2: Authentication and  authorization based on Basic  Authentication

## Protection of authentication data
Since Base64 is by no means regarded as encryption in the sense of security, it is strongly recommended to only offer access to the Application API over HTTPS rather than over HTTP in order to avoid unprotected transmission of the username and password via the Authorization header.


# SSL Certificates

As an alternate solution to authentication via Basic Authentication, SSL certificates can be used to authenticate requests to the Application API (see [Figure 3](#figure_3)). In this case, the provider of the Application API will issue a signed SSL certificate to the client which contains the client’s username as part of the certificate metadata.

<center>
![][figure_3]
</center>
<a name="figure_3"></a>Figure 3: Authentication  and authorization using SSL certificates

## Validation criteria
When initiating the connection, the Application API analyzes the certificate and extracts the username to match against an existing API user. If that user is found, the connection is authenticated, otherwise it’s rejected.
