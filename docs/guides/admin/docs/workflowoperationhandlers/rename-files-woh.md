Rename Files Workflow Operation
===============================

ID: `rename-files`

Description
-----------

The rename files operation allows you to rename media files using different metadata.
This can be used to, for example, generate human readable names for downloads.


Parameter Table
---------------

|configuration keys|description                                                                      |
|------------------|---------------------------------------------------------------------------------|
|sorce-flavors     |Comma separated list of flavors to select tracks to rename
|sorce-flavor      |Same as `sorce-flavors` but can only select a single flavor
|name-pattern      |Pattern to use for renaming the media files


### Replacement Patterns

You can use the following placeholders in your replacement pattern:

- `#{file.extension}`
- `#{file.basename}`
- `#{flavor.type}`
- `#{flavor.subtype}`
- `#{episode...}`
- `#{series...}`

You can use all metadata fields present in the episode and series Dublin Core catalogs of an event.

    '#{' ['series' | 'episode'] '.' DC-FIELD '}'

Here are some common examples:

- `#{episode.title}`
- `#{episode.creator}`
- `#{series.title}`


Operation Example
-----------------

```XML
<operation
  id="rename-files"
  description="Rename files based on metadata">
  <configurations>
    <configuration key="name-pattern">#{episode.title}.#{file.extension}</configuration>
    <configuration key="source-flavors">*/*</configuration>
  </configurations>
</operation>
```
