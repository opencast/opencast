# SanitizeAdaptiveWorkflowHandler

## Description
The SanitizeAdaptiveWorkflowHandler is used to fix references to media files in a playlist.
When files are ingested, they are put into system created and uniquely named directories.
This code will attempt to match the referenced files by changing the references in the playlists.
All the file names must be unique.
This will allow the inspection to parse and inspect all the renditions in an adaptive playlist.
Currently only HLS is supported

## Parameter Table

|configuration keys|example             |description                                                                    |
|------------------|--------------------|-------------------------------------------------------------------------------|
|source-flavor     | presenter/work     | Which media should be checked                                                 |
|target-flavor     | presenter/delivery | Specifies the flavor of the new media                                         |
|target-tags       | sometag            | Specifies the tags of the new media                                           |


## Operation Example

    <operation
        id="sanitize-adaptive"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Fix uploaded HLS files ">
        <configurations>
            <configuration key="source-flavor">presenter/ingested</configuration>
            <configuration key="target-flavor">presenter/delivery</configuration>
            <configuration key="target-tags">engage</configuration>
        </configurations>
    </operation>
