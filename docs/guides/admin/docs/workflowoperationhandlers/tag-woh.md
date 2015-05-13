# TagWorkflowOperation

## Description
With the TagWorkflowOperationHandler it's possible to select various media package elements and then modify their tag set and / or set their flavor.

So for example it's possible to pick up elements like the dublin core catalogs that have been added to the media package at the beginning of the workflow and tag them, so they can be picked up by operations later on.

## Parameter Table
Tags and flavors can be used in combination.

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-tags|"engage,atom,rss,-publish"|Tag any media package elements with one of these (comma separated) tags. If a source-tag starts with a '-', media package elements with this tag will be excluded.|EMPTY|
|source-flavors|"presentation/trimmed"|Tag any media package elements with one of these (comma separated) flavors.|EMPTY|
|target-tags|"tagged,+rss" / "-rss,+tagged"|Apply these (comma separated) tags to any media package elements. If a target-tag starts with a '-', it will be removed from preexisting tags, if a target-tag starts with a '+', it will be added to preexisting tags. If there is no prefix, all preexisting tags are removed and replaced by the target-tags.|EMPTY|
|target-flavor|"presentation/tagged"|Apply these flavor to any media package elements|EMPTY|
|copy|"true" or "false"|Indicates if matching elements will be cloned before tagging is applied or whether tagging is applied to the original element. Set to "true" to create a copy first, "false" otherwise.|FALSE|

### Target Tags Example

|Target-Tags|Preexisting Tags|Resulting Tags|
|-----------|----------------|--------------|
|rss|engage|rss|
|+rss|engage|engage,rss|
|-rss|engage,rss|engage|
|tagged,+rss|engage|tagged|
|-rss,+tagged|engage,rss|engage,tagged|

## Operation Example

    <operation
      id="tag"
      max-attempts="2"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Tagging media package elements">
      <configurations>
        <configuration key="source-tags">engage,atom,-publish</configuration>
        <configuration key="source-flavors">presentation/trimmed</configuration>
        <configuration key="target-tags">-atom,+rss</configuration>
        <configuration key="target-flavor">presentation/tagged</configuration>
        <configuration key="copy">true</configuration>
      </configurations>
    </operation>
