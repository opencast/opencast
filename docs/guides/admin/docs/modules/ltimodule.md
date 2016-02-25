Integrating Opencast in an LMS with The LTI module
===================================================

The LTI - "Learning Tools Interoperability"  module has been introduced to offer an easy way to integrate Opencast in a learning management system. This guide has been written to explain how to use this module.
For more information about LTI specifications themselves, please visit http://www.imsglobal.org/toolsinteroperability2.cfm or http://developers.imsglobal.org/.

How To Use It
-------------

Test if the LTI module is present in the Opencast instance.
First of all, be sure that the module matterhorn-lti has been compiled, deployed and starts within the opencast instance.To test if the module is running, go on the page
`OPENCAST-URL/ltitools/index.html`.

A welcome message from the LTI tools should appear. This is the default index page for the LTI tool. But two ready-to-use elements  are available in this module:

The engage player:
`OPENCAST-URL/ltitools/player/index.html?id=RESOURCEID`

The media module (recording list, use the player above): `OPENCAST-URL/ltitools/series/index.html?series=[SERIES-ID]`

If no SERIES-ID is given, the media module will display the recordings from all the series.

Configure Opencast 
------------------------

The integration of Opencast in the LMS through the LTI module has to be configured on both sides: In Opencast and in the LMS.
To let the LMS access Opencast, the security file etc/security/mh_default_org.xml from the opencast folder must be configured. The relevant sections are shown below:
Uncomment:
```
<sec:custom-filter after="BASIC_AUTH_FILTER" ref="oauthProtectedResourceFilter" />
```
Set the CONSUMER_KEY and theCONSUMER_SECRET

```
  <!-- ####################### -->
  <!-- # OAuth (LTI) Support # -->
  <!-- ####################### -->
  <bean name="oAuthConsumerDetailsService" class="org.opencastproject.kernel.security.OAuthSingleConsumerDetailsService">
    <constructor-arg index="0" ref="userDetailsService" />
    <constructor-arg index="1" value="CONSUMER_KEY" />
    <constructor-arg index="2" value="CONSUMER_SECRET" />
    <constructor-arg index="3" value="consumerName" />
  </bean>
```
Change trustedKey to:
```
<bean class="org.opencastproject.kernel.security.LtiLaunchAuthenticationHandler">
        <constructor-arg index="0" ref="userDetailsService" />
        <constructor-arg index="1" ref="securityService" />
        <constructor-arg index="2">
          <list>
            <value>CONSUMER_KEY</value>
          </list>
        </constructor-arg>
      </bean>
```

- The two OAuth parameters CONSUMER_KEY and CONSUMER_SECRET have to be set with your own values.
They are then required to embed OPENCAST into my LMS.

Configure the LMS
-----------------

To connect OPENCAST to the LMS additional configuration parameters besides the LTI endpoint (ENGAGE-URL/LTI), the consumerkey and consumersecret are needed on the LMS side. OPENCAST uses custom LTI tags to pass additional information. These include the kind of interface to present (engage player or media module) and the id of the media to show.

The interface is passed using the custom_tool parameter:
`custom_tool = ltitools/player/index.html  (to show the engage player)`
or
`custom_tool = ltitools/series/index.html (to show the series overview)`

The ID of the series/episode needs to be passed differently depending on the interface chosen. For the engage player the custom_id field is used:
`custom_id = 3e9f15b1-97ef-4be6-8276-59da869ceecd`
For the series overview the id is passed as custom_series:
`custom_series = 3e9f15b1-97ef-4be6-8276-59da869ceecd`

For debugging purposes, it could be useful to set the following custom tag:
`custom_test = true`

For more information about how to set custom LTI tags, please check the documenation of your LMS.

How To Customise It
-------------------
The LTI module can be extended/customised to fullfill your own needs.  The default elements from the LTI module (player, series) can be found in the folder `OPENCAST/modules/matterhorn-lti/src/main/resources/tools`.  These elements can be modified or new one can be created to extend the LTI module. Keep in mind that the player and series elements are using some files from the OPENCAST/shared-resources folder. These files are copied at compile-time. For more information about it, look at the maven pom file of the LTI module.
