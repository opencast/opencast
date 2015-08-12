# SegmentVideoWorkflowOperation

## Description
The SegmentVideoWorkflowOperation will try to identify and mark different segments of a video. A new segment is created when a major change in the video occurs. This might be the case for example if the video is a screenrecording and the slides which were shown change.

## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------|
|source-flavor |presentation/trimmed|Specifies which media should be processed.|
 	 	 	 
## Operation Example

    <operation
          id="segment-video"
          fail-on-error="false"
          exception-handler-workflow="error"
          description="Extracting segments from presentation">
          <configurations>
                <configuration key="source-flavor">presentation/trimmed</configuration>
          </configurations>
    </operation>
