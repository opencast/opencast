Integrating Opencast in an LMS using LTI
========================================

What it does
------------

The Opencast LTI module provides an easy way to integrate Opencast into a Learning Management System (LMS),
or any other system which supports the LTI standard as an LTI _tool consumer_.

Typically, students enrolled in a course access Opencast through an LTI tool in the LMS course site,
and can play back videos in an Opencast series set up for the course.

More information about the LTI specifications is available at
[IMS Learning Tools Interoperability](http://www.imsglobal.org/activity/learning-tools-interoperability).

Configure Opencast
------------------

### Configure OAuth authentication

LTI uses OAuth to authenticate users. To enable OAuth in Opencast, edit `etc/security/mh_default_org.xml` and uncomment
the oauthProtectedResourceFilter in the Authentication Filters section:

```xml
    <!-- 2-legged OAuth is used by trusted 3rd party applications, including LTI. -->
    <!-- Uncomment the line below to support LTI or other OAuth clients.          -->
    <ref bean="oauthProtectedResourceFilter" />
```

To configure OAuth consumers (e.g. a LMS), edit
`etc/org.opencastproject.kernel.security.OAuthConsumerDetailsService.cfg` and replace CONSUMERNAME, CONSUMERKEY, and
CONSUMERSECRET with the values you will use in your LMS:

```properties
oauth.consumer.name.1=CONSUMERNAME
oauth.consumer.key.1=CONSUMERKEY
oauth.consumer.secret.1=CONSUMERSECRET
```

### Configure LTI (optional)

To give LMS users the same username in Opencast as the LMS username, edit
`etc/org.opencastproject.kernel.security.LtiLaunchAuthenticationHandler.cfg` and add the configured OAuth consumer key
to the list of highly trusted keys.

```properties
lti.oauth.highly_trusted_consumer_key.1=CONSUMERKEY
```

Use can exempt specific users even if a highly trusted consumer is used by configuring a blacklist. Additionally, there
are settings for excluding the system administrator as well as the digest user (enabled by default).

```properties
lti.allow_system_administrator=false
lti.allow_digest_user=false
lti.blacklist.user.1=myAdminUser
```

> **Notice:** Marking a consumer key as highly trusted can be a security risk! If the usernames of sensitive Opencast
> users are not blacklisted, the LMS administrator could create LMS users with the same username and use LTI to grant
> that user access to Opencast. In the default configuration, that includes the `admin` and `opencast_system_account`
> users.

Configure and test an LTI tool in the LMS
-----------------------------------------

Configure an LTI tool in the LMS with these values:

* LTI launch URL: `OPENCAST-URL/lti`
* LTI key: the value chosen for CONSUMERKEY in `org.opencastproject.kernel.security.OAuthConsumerDetailsService.cfg`
* LTI secret: the value chosen for CONSUMERSECRET in `org.opencastproject.kernel.security.OAuthConsumerDetailsService.cfg`

In a clustered Opencast system, choose the URL of the presentation server where the media module and player are available.

Access the LTI tool configured for Opencast in the LMS. The Opencast LTI Welcome page should appear. Click on the links
provided to `OPENCAST-URL/lti` and `OPENCAST-URL/info/me.json` to verify the LTI parameters provided to Opencast by the LMS,
and the list of roles which the LTI user has in Opencast.

LTI roles
----------

LTI users will only see Opencast series and videos which are public, or those to which they have access
because of the Opencast roles which they have. The Opencast LTI module grants an LTI user the role(s) formed
from the LTI parameters `context_id` and `roles`.

The LTI context is typically the LMS course ID, and the default LTI role for a student in a course is `Learner`.
The Opencast role granted would therefore be `SITEID_Learner`.

To make a series or video visible to students who access Opencast through LTI in an LMS course,
add the role `SITEID_Learner` to the Series or Event Access Control List (ACL).

LTI users may also have additional roles if the LTI user is created as an Opencast user in the Admin UI and
given additional roles, or if one or more Opencast User Providers or Role Providers are configured.

Customize the LTI tool in the LMS
----------------------------------

Opencast will redirect an LTI user to the URL specified by the LTI custom `tool` parameter. Some LMS systems allow
custom parameters to be defined separately in each place where an LTI tool is used, whereas other systems only allow
custom parameters to be defined globally.

* To show the Opencast Media Module, use `tool=engage/ui/`
* To show all videos for a single series, use `tool=ltitools/series/index.html?series=SERIESID`
* To show a single video, use `tool=/play/MEDIAPACKAGEID`
* To show a short debugging page before proceeding to the tool page, add the parameter `test=true`

For more information about how to set custom LTI parameters, please check the documentation of your LMS.

