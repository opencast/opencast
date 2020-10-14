# TagByDCTermWorkflowOperationHandler

## Description
With the TagByDCTermWorkflowOperationHandler it's possible to select various media package elements and then modify
their tag set and / or set their flavor according to whether a Dublin Core term in a catalog has a specific value.

So for example it's possible to pick elements like the Dublin Core catalogs that have been added to the media package
at the beginning of the workflow and tag them, so they can be picked up by operations later on or even an application
that harvests the mediapackage from a publication channel.

In combination with [ConfigureByDCTermWorkflowOperationHandler](configure-by-dcterm-woh.md) workflows can be controlled
by the metadata contained within the Dublin core catalogs.

## Parameter Table
Tags and flavors can be used in combination.

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-tags       |"engage,atom,rss,-publish"|Tag any media package elements with one of these (comma separated) tags. If a source-tag starts with a '-', media package elements with this tag will be excluded.|EMPTY|
|source-flavors    |"presentation/trimmed"    |Tag any media package elements with one of these (comma separated) flavors.|EMPTY|
|dccatalog         |"episode" or "series"     |the type of catalog in which to search for dcterm|EMPTY|
|dcterm            |"creator"                 |the name of the Dublin Core term which to check|EMPTY|
|match-value       |"Joe Bloggs"              |the Dublin Core term value to check for|EMPTY|
|default-value"    |"Anon"                    |the implied value if the dublincore term is not present in the catalog|EMPTY|
|target-tags       |"tagged,+rss" / "-rss,+tagged"|Apply these (comma separated) tags to any media package elements. If a target-tag starts with a '-', it will be removed from preexisting tags, if a target-tag starts with a '+', it will be added to preexisting tags. If there is no prefix, all preexisting tags are removed and replaced by the target-tags.|EMPTY|
|target-flavor     |"presentation/tagged"     |Apply these flavor to any media package elements|EMPTY|
|copy              |"true" or "false"         |Indicates if matching elements will be cloned before tagging is applied or whether tagging is applied to the original element. Set to "true" to create a copy first, "false" otherwise.|FALSE|

Note: see [TagWorkflowOperationHandler](tag-woh.md) for further explanation of the source/target-flavor/tags

### dccatalog
The type of Dublin Core catalog in which to look for the `dcterm`. This will usually be `episode` or `series`.

### dcterm
The name of the Dublin Core term to look for in the `dccatalog`. This could be one of the terms set by Opencast or an
additional term adding to the catalog.

### match-value
The value of the `dcterm` which to match against. The comparison is case sensitive.

### default-value
If `default-value` is used when the `dcterm` is not found in the catalog. If not specified the operation will treat the
match as false and not tag anything. If `default-value` is specified the operation will compare the `match-value` to
the `default-value` and apply the tags if they match. This allows an implied value to be explicitly and clearly
defined. For example if you have mediapackages that were created before additional metadata was added to the episode
catalog you may want to imply that the `audience` term has a value of `all-enrolled`.

## Operation Example
    <operation
      id="tag-by-dcterm"
      max-attempts="2"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Tagging media package elements according to dcterm">
      <configurations>
        <configuration key="source-flavors">dublincore/*,security/*</configuration>
        <configuration key="dccatalog">episode</configuration>
        <configuration key="dcterm">audience</configuration>
        <configuration key="match-value">learning-difficulties</configuration>
        <configuration key="default-value">all-enrolled</confiuration>
        <configuration key="target-tags">+publishBeforeEditing</configuration>
      </configurations>
    </operation>
