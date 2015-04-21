# ComposeWorkflowHandler

## Description
The ComposeWorkflowHandler is used to encode media files to different formats using FFmpeg.

## Parameter Table
<table>
<tr><th>configuration keys</th><th>example</th><th>description</th></tr>	 
<tr><td>source-flavor</td><td>presenter/work</td><td>Which media should be encoded</td></tr>
<tr><td>target-flavor </td><td>presenter/delivery</td><td>Specifies the flavor of the new media</td></tr>
<tr><td>source-tags</td><td>	sometag	</td><td>Tags of media to encode	 </td></tr>
<tr><td>target-tags</td><td>	sometag	</td><td>Specifies the tags of the new media </td></tr>	 
<tr><td>encoding-profile	webm-hd	</td><td>Specifies the encoding profile to use </td></tr>
</table>	 
 
## Operation Example

    <operation
        id="compose"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Encoding presenter (camera) video to Flash download">
        <configurations>
            <configuration key="source-flavor">presenter/trimmed</configuration>
            <configuration key="target-flavor">presenter/delivery</configuration>
            <configuration key="target-tags">engage</configuration>
            <configuration key="encoding-profile">flash.http</configuration>
        </configurations>
    </operation>
