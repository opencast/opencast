# PostMediapackageWorkflowHandler

## Description

This Workflow Operation Handler can be used to send a POST request containing an XML/JSON representation of the
Mediapackage processed by the workflow to an external webservice. The service supports HTTP Basic and Digest
Authentication.

## Parameter Table

|Configuration Keys |Description                                                                                   |
|-------------------|----------------------------------------------------------------------------------------------|
|url                |The target url                                                                                |
|format             |The desired export format: `xml` or `json`                                                    |
|debug              |Disable this on a productive system. If enabled, request bodies etc. will be written to log. If disabled, only errors will be logged. |
|mediapackage.type  |Type of Mediapackage to send (possible values: `workflow`, `search`; default: `search`)       |
|auth.enabled       |enable authentication (simple/digest will be detected automatically)                          |
|auth.username      |username for authentication                                                                   |
|auth.password      |password for authentication                                                                   |
|+source_system     |fields with keys beginning with `+` will be added to the message body                         |

## Operation Example

    <operation
        id="post-mediapackage"
        fail-on-error="false"
        exception-handler-workflow="error"
        description="Sending MediaPackage to Lernfunk3">
        <configurations>
            <configuration key="url">http://example.com:5000/</configuration>
            <configuration key="format">xml</configuration>
            <configuration key="debug">no</configuration>
            <configuration key="mediapackage.type">search</configuration>
            <configuration key="auth.enabled">yes</configuration>
            <configuration key="auth.username">exportuser</configuration>
            <configuration key="auth.password">secret</configuration>
            <configuration key="+source_system">video.example.com</configuration>
        </configurations>
    </operation>
