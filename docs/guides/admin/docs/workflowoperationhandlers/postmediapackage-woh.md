# PostMediapackageWorkflowHandler

## Description

This Workflow Operation Handler can be used to send a POST request containing an XML/JSON representation of the Mediapackage processed by the workflow to an external webservice. The service supports HTTP Basic and Digest Authentication.

## Options

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
            <!-- export format: xml or json -->
            <configuration key="format">xml</configuration>
            <!--
                Disable this on a productive system. If enabled, request bodies
                etc. will be written to log. If disabled, only errors will be
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
