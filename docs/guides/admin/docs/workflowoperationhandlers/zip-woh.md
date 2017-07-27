ZipWorkflowOperation
====================

Description
-----------

The ZipWorkflowOperationHandler creates a zip archive including all elements of the current media package that are
specified in the operation configuration. It then adds the archive to the media package as an attachment with the given
flavor and tags and by default stores the zip file in the working file repository's "zip" collection.


Parameter Table
---------------

|configuration  |example                |description                                                |default value|
|---------------|-----------------------|-----------------------------------------------------------|-------------|
|zip-collection |`zips`                 |A comma separated list of flavors to preserve from deleting|`zip`        |
|include-flavors|`*/source,dublincore/*`|Which elements to include in the archive                   |(all)        |
|target-flavor  |`archive/zip`          |The flavor of the created attachment                       |`archive/zip`|
|target-tags    |`archive`              |The tags to apply to the attachment                        |             |
|compression    |`true`                 |Whether to compress the archive content                    |`flase`      |

Additional notes:

- The `include-flavors` configuration parameter accepts exact flavors like `presenter/source` as well as wildcard flavor
  definitions like `*/source`.
- Usually, for media content, zip compression does not reduce the size of the archive very much but adds significant
  processing time. That is why activating this is usually not recommended.


Operation Example
-----------------

    <operation
      id="zip"
      description="Creating zipped recording archive">
      <configurations>
        <configuration key="zip-collection">failed.zips</configuration>
        <configuration key="include-flavors">*/source,dublincore/*</configuration>
        <configuration key="target-flavor">all/zip</configuration>
        <configuration key="compression">false</configuration>
      </configurations>
    </operation>
