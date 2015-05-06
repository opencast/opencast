# SegmentpreviewsWorkflowOperation

## Description
The SegmentpreviewsWorkflowOperation will extract still images from a video using FFmpeg, a given encoding profile and previous discovered segments.

## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------| 
|source-flavor|presenter/source|Specifies which media should be processed.|
|target-flavor|presenter/work|Specifies the flavor the new files will get.|
|source-tags	|engage	|Specifies which media should be processed.	 |
|target-tags	|engage	|Specifies the tags the new files will get.	 |
|encoding-profile	|search-cover.http	|The encoding profile to use.	 |
|reference-flavor	|presentation/work	|Flavor of the segments to use.	 |
|reference-tags	|engage	|Tags of the segments to use.	 |
 
## Operation Example

    <operation
          id="segmentpreviews"
          fail-on-error="false"
          exception-handler-workflow="error"
          description="Encoding presentation (screen) to segment preview image">
          <configurations>
                <configuration key="source-flavor">presentation/trimmed</configuration>
                <configuration key="source-tags"></configuration>
                <configuration key="target-flavor">presentation/segment+preview</configuration>
                <configuration key="reference-flavor">presentation/delivery</configuration>
                <configuration key="reference-tags">engage</configuration>
                <configuration key="target-tags">engage</configuration>
                <configuration key="encoding-profile">player-slides.http</configuration>
          </configurations>
    </operation>
