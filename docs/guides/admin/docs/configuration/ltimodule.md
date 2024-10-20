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
For a better  understanding of what LTI is and what it is not, consider this
[introductory webinar](https://video.ethz.ch/events/opencast/miscellaneous/webinars/3bb13443-19d3-4147-8ff9-7106f5a959bb.html).


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
`etc/org.opencastproject.security.lti.LtiLaunchAuthenticationHandler.cfg`.


The “delete” key in the series overview tool can be configured by specifying the retraction workflow in
`etc/org.opencastproject.lti.service.impl.LtiServiceImpl.cfg`. The property is called `retract-workflow-id`, and it defaults
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
The Opencast role granted would therefore be `<context-id>_Learner`.

To make a series or video visible to students who access Opencast through LTI in an LMS course,
add the role `<context-id>_Learner` to the series or event access control list (ACL).

An additional prefix for these generated roles may be defined in Opencast's LTI configuration file based on the used
OAuth consumer. That way, you can distinguish between users from multiple different consumers.

LTI users may also have additional roles if the LTI user is created as an Opencast user in the Admin UI and
given additional roles, or if one or more Opencast User Providers or Role Providers are configured.


Specifying LTI Tools
--------------------

Opencast will redirect an LTI user to the URL specified by the LTI custom `tool` parameter. Some LMS systems allow
custom parameters to be defined separately in each place where an LTI tool is used, whereas other systems only allow
custom parameters to be defined globally.

- To show the media module, use `engage/ui/` as LTI `custom_tool` launch parameter
- To show all videos for a single series, use `ltitools/index.html` as LTI `custom_tool` launch parameter
  and specify the following query parameters:
    - `subtool=series`
    - `series=SERIESID` if you have the series ID
    - `series_name=SERIESNAME` if you just have the series name (has to be unique)
    - `deletion=true` to show a delete button next to each episode
    - `edit=true` if you want to display an edit button next to each episode
    - `annotate=true` if you want to display an annotate (annotation tool) button next to each episode
    - `download=true` to show a button next to each episode that allows for downloading individual video files
    - `lng=LANG` to force a language (the browser language is used otherwise)
- To show an upload dialog, use `ltitools/index.html` as LTI `custom_tool` launch parameter
  and specify the following query parameters:
    - `subtool=upload`
    - `series=SERIESID` if you have the series ID
    - `series_name=SERIESNAME` if you just have the series name (has to be unique)
    - `lng=LANG` to force a language (the browser language is used otherwise)
- To show a single video, use `/play/<id>` as LTI `custom_tool` launch parameter
- To show a debug page before proceeding to the tool, append the parameter `test=true`

For more information about how to set custom LTI parameters, please check the documentation of your LMS.


### Customizing LTI’s look

The LTI module provides the option to provide custom style sheets for configuring the look and feel of the
tools which may be important to match the design of the LTI consumer in which it is included. The CSS file can be found
in the user interface configuration directory usually located at:

    etc/ui-config/mh_default_org/ltitools/lti.css
