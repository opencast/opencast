# Tag Engage Workflow Operation

ID: `tag-engage`

## Description

With the tag-engage operation, it's possible to modify the tags and flavors of media package elements (catalogs,
attachments or media) directly in the engage publication. Its behavior is based on the [tag](tag-woh.md)
operation. The publication is updated in the archive (don't forget to take a snapshot) as well as the search service.
Since no files have to be downloaded into the workspace, this operation is usually very quick.

## Parameter Table

| configuration keys | example                        | description                                                                                                                                                                                                                                                                                                      | default value |
|--------------------|--------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| source-tags        | "engage,atom,rss,-publish"     | Tag any media package elements with one of these (comma separated) tags. If a source-tag starts with a '-', media package elements with this tag will be excluded.                                                                                                                                               | EMPTY         |
| source-flavors     | "presentation/trimmed"         | Tag any media package elements with one of these (comma separated) flavors.                                                                                                                                                                                                                                      | EMPTY         |
| target-tags        | "tagged,+rss" / "-rss,+tagged" | Apply these (comma separated) tags to any media package elements. If a target-tag starts with a '-', it will be removed from preexisting tags, if a target-tag starts with a '+', it will be added to preexisting tags. If there is no prefix, all preexisting tags are removed and replaced by the target-tags. | EMPTY         |
| target-flavor      | "presentation/tagged"          | Apply this flavor to any media package elements.                                                                                                                                                                                                                                                                 | EMPTY         |

Tags and flavors can be used in combination. For examples see the [tag](tag-woh.md) operation.

## Operation Example

```xml
    <operation
    id="tag-engage"
    description="Remove tag from composites in Engage">
  <configurations>
    <configuration key="source-flavors">presenter/*</configuration>
    <configuration key="source-tags">engage-streaming</configuration>
    <configuration key="target-flavor">presenter/tagged</configuration>
    <configuration key="target-tags">+test</configuration>
  </configurations>
</operation>
```
