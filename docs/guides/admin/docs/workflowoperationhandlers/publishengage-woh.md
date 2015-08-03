# PublishEngageWorkflowOperation

## Description
The PublishEngageWorkflowOperation will bring your media to the engage distribution channels (streaming, progressive download, …)

##Parameter Table
|configuration keys|example|description|
|------------------|-------|-----------|
|download-source-tags|engage|Specifies which media should be published to progressive download.|
|streaming-source-tags	|engage	 |Specifies which media should be published to the streaming server.	 |
|check-availability	|true 	|If the opertion should check if the media if rechable.	 |
|download-source-flavors | |
|download-target-subflavors	 	 	| | 
|download-target-tags	 	 	 | |
|streaming-tagret-tags	 	 	 | |
|streaming-source-flavors	 	 | |	 
|streaming-target-flavors	 	 | |	 

## Operation Example

    <operation
          id="publish-engage"
          max-attempts="2"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Distribute and publish to engage player">
          <configurations>
                <configuration key="download-source-tags">engage,atom,rss</configuration>
                <configuration key="streaming-source-tags">engage</configuration>
                <configuration key="check-availability">true</configuration>
          </configurations>
    </operation>
