=============================================
matterhorn-workflowoperation-mediapackagepost
=============================================

This Opencast Matterhorn workflowoperation handler can be used to send a POST
request containing the currently processed Mediapackage to an external
webservice.

--------
Features
--------

- Send Mediapackage to remote webservice
- Handle HTTP Basic/Digest authentication
- Send additional data


------------
Contributors
------------

- Benjamin Wulff  <bwulff@uos.de>    Original Author
- Lars Kiesow     <lkiesow@uos.de>   Current Maintainer


----------------------
Workflow Configuration
----------------------

The workflowoperation handler can be integrated like this:

	<!--
		This operation will send a POST request containing the Mediapackage to an
		external webservice.
	-->
	<operation
		id="post-mediapackage"
		fail-on-error="false"
		exception-handler-workflow="error"
		description="Sending MediaPackage to Lernfunk3">
		<configurations>
			<!-- target url -->
			<configuration key="url">http://example.com:5000/</configuration>
			<!-- Use xml as export format -->
			<configuration key="format">xml</configuration>
			<!--
				Disable this on a productive system. if enabled, request bodies
				etc. will be written to log. Is disabled, only errors will be
				logged.
			-->
			<configuration key="debug">no</configuration>
			<!-- Type of Mediapackage to send (possible values: workflow, search; default: search) -->
			<configuration key="mediapackage.type">search</configuration>
			<!-- enable authentication (simple/digest will be detected automatically) -->
			<configuration key="auth.enabled">yes</configuration>
			 <!-- username for authentication -->
			<configuration key="auth.username">exportuser</configuration>
			<!-- password for authentication -->
			<configuration key="auth.password">secret</configuration>
			<!-- fields with keys beginning with + will be added to the message body -->
			<configuration key="+source_system">video.example.com</configuration>
		</configurations>
	</operation>
