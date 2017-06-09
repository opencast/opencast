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
------------------------

To enable LTI authentication in Opencast, edit `OPENCAST/etc/security/mh_default_org.xml`

* In the Authentication Filters section, uncomment the oAuthProtectedResourceFilter: 
````
    <!-- 2-legged OAuth is used by trusted 3rd party applications, including LTI. -->
    <!-- Uncomment the line below to support LTI or other OAuth clients.          -->
    <ref bean="oauthProtectedResourceFilter" />
````

* Replace CONSUMER_KEY and CONSUMER_SECRET with LTI the key and secret values that you will use in your LMS:
````
    <!-- ####################### -->
    <!-- # OAuth (LTI) Support # -->
    <!-- ####################### -->

    <!-- This is required for LTI. If you are using LTI and have enabled the oauthProtectedResourceFilter  -->
    <!-- in the list of authenticationFilters above, set custom values for CONSUMERKEY and CONSUMERSECRET. -->

    <bean name="oAuthConsumerDetailsService" class="org.opencastproject.kernel.security.OAuthSingleConsumerDetailsService">
    <constructor-arg index="0" ref="userDetailsService" />
    <constructor-arg index="1" value="CONSUMERKEY" />
    <constructor-arg index="2" value="CONSUMERSECRET" />
    <constructor-arg index="3" value="constructorName" />
    </bean>
````

* To give LMS users the same username in Opencast as the LMS username, uncomment the constructor arguments 
below and update CONSUMERKEY to the same key used above:

````
    <!-- Uncomment to trust usernames from the LTI consumer identified by CONSUMERKEY.           -->
    <!-- Users from untrusted systems will be prefixed with "lti:" and the consumer domain name. -->

    <constructor-arg index="1" ref="securityService" />
    <constructor-arg index="2">
      <list>
        <value>CONSUMERKEY</value>
      </list>
    </constructor-arg>
````

Configure and test an LTI tool in the LMS
-----------------------------------------

Configure an LTI tool in the LMS with these values:

* LTI launch URL: `OPENCAST-URL/lti`
* LTI key: the value chosen for CONSUMERKEY in `mh_default_org.xml`
* LTI secret: the value chosen for CONSUMERSECRET in `mh_default_org.xml`

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
* To show all videos for a single series, use `tool=ltitools/series/index.html;series=SERIESID`
* To show a single video, use `tool=engage/ui/theodul/core/index.html;id=MEDIAPACKAGEID`
* To show a short debugging page before proceeding to the tool page, add the parameter `test=true`

For more information about how to set custom LTI parameters, please check the documentation of your LMS.

