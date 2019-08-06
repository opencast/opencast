Integrating Opencast using LTI
==============================


About LTI
---------

LTI provides an easy way to integrate Opencast into any system which can act as an LTI tool consumer such as many
learning management systems (LMS). Popular examples for LTI consumers include [Sakai](https://sakailms.org),
[Moodle](https://moodle.org) or [ILIAS](https://ilias.de).

Using the LTI integration, students can access Opencast through an LTI tool in the LMS course site, and can play back
Opencast videos without ever leaving their course.

More information about the LTI specification is available at
[IMS Learning Tools Interoperability](https://imsglobal.org/activity/learning-tools-interoperability).


Configuration
-------------


### Configure OAuth

LTI uses OAuth to authenticate users. To enable OAuth in Opencast, edit `etc/security/mh_default_org.xml` and uncomment
the oauthProtectedResourceFilter in the authentication filters section:

```xml
<ref bean="oauthProtectedResourceFilter" />
```

Next, configure the OAuth consumer by setting custom credentials in
`etc/org.opencastproject.kernel.security.OAuthConsumerDetailsService.cfg`:

```properties
oauth.consumer.name.1=CONSUMERNAME
oauth.consumer.key.1=CONSUMERKEY
oauth.consumer.secret.1=CONSUMERSECRET
```


### Configure LTI

Opencast's LTI module allows additional configuration like making a OAuth consumer key a highly trusted key, preventing
Opencast from generating a temporary username, or to block some specific usernames like the system administrator.

For more details, take a look at the options in
`etc/org.opencastproject.kernel.security.LtiLaunchAuthenticationHandler.cfg`.


The “delete” key in the series overview tool can be configured by specifying the retraction workflow in
`etc/org.opencastproject.lti.endpoint.EventsEndpoint.cfg`. The property is called `retract-workflow-id`, and it defaults
to `retract`.

Configure and test an LTI tool in the LMS
-----------------------------------------

Configure an LTI tool in the LMS with these values:

- LTI launch URL: `<presentation-node-url>/lti`
- LTI key: the value of `oauth.consumer.key`
- LTI secret: the value of `oauth.consumer.secret`

Access the LTI tool configured for Opencast in the LMS. The Opencast LTI welcome page should appear. Use the links
provided there to verify the LTI connection.


LTI Roles
---------

LTI users will only see Opencast series and videos which are public, or those to which they have access
because of the Opencast roles which they have. The Opencast LTI module grants an LTI user the role(s) formed
from the LTI parameters `context_id` and `roles`.

The LTI context is typically the LMS course ID, and the default LTI role for a student in a course is `Learner`.
The Opencast role granted would therefore be `SITEID_Learner`.

To make a series or video visible to students who access Opencast through LTI in an LMS course,
add the role `SITEID_Learner` to the Series or Event Access Control List (ACL).

LTI users may also have additional roles if the LTI user is created as an Opencast user in the Admin UI and
given additional roles, or if one or more Opencast User Providers or Role Providers are configured.


Specifying LTI Tools
--------------------

Opencast will redirect an LTI user to the URL specified by the LTI custom `tool` parameter. Some LMS systems allow
custom parameters to be defined separately in each place where an LTI tool is used, whereas other systems only allow
custom parameters to be defined globally.

- To show the media module, use `tool=engage/ui/`
- To show all videos for a single series, use `tool=ltitools/series/index.html?series=SERIESID`. You can also pass
  `series_name` to select a series by name (the name has to be unique). If you want the ability to delete events
  from the series (which will only be available for instructors), add `&deletion=true` to the URL.
* To show an upload form, use `tool=ltitools/upload/index.html?series=SERIESID`. You can also pass
  `series_name` to select a series by name (the name has to be unique).
- To show a single video, use `tool=/play/MEDIAPACKAGEID`
- To show a debug page before proceeding to the tool, append the parameter `test=true`

For more information about how to set custom LTI parameters, please check the documentation of your LMS.


### Series LTI Tool

Opencast's series LTI tool provides the option to provide custom style sheets for configuring the look and feel of the
tool which may be important to match the design of the LTI consumer in which it is included. The CSS file can be found
in the user interface configuration directory usually located at:

    etc/ui-config/mh_default_org/ltitools/series.css
