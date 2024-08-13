# Tag Workflow Operation

ID: `tag`

## Description

With the tag operation, it's possible to select various media package elements and then modify their tags
and/or their flavor.

So for example it's possible to pick up elements like the dublin core catalogs that have been added to the media package
at the beginning of the workflow and tag them, so they can be picked up by operations later on.

## Parameter Table

Tags and flavors can be used in combination.

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-tags|"engage,example,-publish"|Tag any media package elements with one of these (comma separated) tags. If a source-tag starts with a '-', media package elements with this tag will be excluded.|EMPTY|
|source-flavors|"presentation/trimmed"|Tag any media package elements with one of these (comma separated) flavors.|EMPTY|
|target-tags|"tagged,+example" / "-example,+tagged"|Apply these (comma separated) tags to any media package elements. If a target-tag starts with a '-', it will be removed from preexisting tags, if a target-tag starts with a '+', it will be added to preexisting tags. If there is no prefix, all preexisting tags are removed and replaced by the target-tags.|EMPTY|
|target-flavor|"presentation/tagged"|Apply these flavor to any media package elements|EMPTY|
|copy|"true" or "false"|Indicates if matching elements will be cloned before tagging is applied or whether tagging is applied to the original element. Set to "true" to create a copy first, "false" otherwise.|FALSE|

### Target Tags Example

|Target-Tags |Preexisting Tags|Resulting Tags|
|------------|----------------|--------------|
|example     |engage          |example       |
|+example    |engage          |engage,example|
|-example    |engage,example  |engage        |
|tagged,+ex  |engage          |tagged        |
|-ex,+tagged |engage,ex       |engage,tagged |

## Operation Example

```xml
<operation
  id="tag"
  description="Tagging media package elements">
  <configurations>
    <configuration key="source-tags">engage,publish</configuration>
    <configuration key="source-flavors">presentation/trimmed</configuration>
    <configuration key="target-flavor">presentation/tagged</configuration>
    <configuration key="copy">true</configuration>
  </configurations>
</operation>
```

You can also use `tag` to just modify flavors (`*/source` will not be present any longer):

```xml
<operation
  id="tag"
  description="Changing flavor of source elements">
  <configurations>
    <configuration key="source-flavors">*/source</configuration>
    <configuration key="target-flavor">*/delivery</configuration>
  </configurations>
</operation>
```
