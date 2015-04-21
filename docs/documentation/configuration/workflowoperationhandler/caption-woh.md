# CaptionWorkflowOperation

## Description
TheCaptionWorkflowOperation waits for user to upload caption files which will be added to the media.

## Parameter Table

<table>
<tr><th>configuration keys</th><th>example</th><th>description</th></tr>
<tr><td>target-tags</td><td>engage</td><td>Specifies which media should be processed.</td></tr>
<table>
 	
## Operation Example

    <operation
          id="caption"
          if="${captionHold}"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Waiting for user to upload captions">
          <configurations>
                <configuration key="target-tags">engage,archive</configuration>
          </configurations>
    </operation>
 
