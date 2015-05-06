# EmailWorkflowOperation
## Description
The EmailWorkflowOperationHandler queries the SMTP Service to send an email with the provided parameters. It is useful to send email notifications that some operation(s) have been completed or that some error(s) occurred in a workflow.
The mail body consists of a single line of the form: <Recording Title> (<Mediapackage ID>).

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|to|"my-email-account@my-email-domain.org"|It specifies the field "to" of the email, i.e. the email account the email will be sent to.|EMPTY|
|subject|Operation has been completed|Specifies the mail subject|EMPTY|

**Some other email parameters can be customized in the SMTP Service configuration**

##Operation Example

    <operation
        id="send-email"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Sends email">
        <configurations>
            <configuration key="to">root@localhost</configuration>
            <configuration key="subject">Failure processing a mediapackage</configuration>
        </configurations>
    </operation>
