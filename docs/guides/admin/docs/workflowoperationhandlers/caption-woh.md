# CaptionWorkflowOperation

## Description
TheCaptionWorkflowOperation waits for user to upload caption files which will be added to the media.

## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------|
|target-tags|engage|Specifies which media should be processed.|
 	
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
 
