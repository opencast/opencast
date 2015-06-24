# ImageWorkflowOperation

## Description
The ImageWorkflowOperation will extract a still image from a video using FFmpeg and a given encoding profile.

##Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------| 
|source-flavor|presenter/source|Specifies which media should be processed.| 
|target-flavor|presenter/work|Specifies the flavor the new files will get.|
|source-tags	|engage	|Specifies which media should be processed.	 |
|target-tags	|engage	|Specifies the tags the new files will get.	 |
|encoding-profile	|search-cover.http	|The encoding profile to use.	 |
|time	|1	|Time in seconds where the image should be taken.	 |
 
## Operation Example

    <operation
          id="image"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Encoding presenter (camera) to search result preview image">
          <configurations>
                <configuration key="source-flavor">presenter/trimmed</configuration>
                <configuration key="source-tags"></configuration>
                <configuration key="target-flavor">presenter/search+preview</configuration>
                <configuration key="target-tags">engage</configuration>
                <configuration key="encoding-profile">search-cover.http</configuration>
                <configuration key="time">1</configuration>
          </configurations>
    </operation>
