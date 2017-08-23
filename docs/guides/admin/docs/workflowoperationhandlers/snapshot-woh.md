AssetManagerSnapshotWorkflowOperationHandler
============================================

Description
-----------

The snapshot operation allows you to take a new, versioned snapshot of a media package which is put into the asset
manager.


Parameter Table
---------------

|configuration keys|example         |description|
|------------------|----------------|-----------|
|source-tags       |text            |Comma separated list of tags. Specifies which media should be the source of a snapshop.|
|source-flavors    |presenter/source|Comma separated list of flavors. Specifies which media should be the source of a snapshot.|


Operation Example
-----------------

    <operation
      id="snapshot"
      description="Archiving">
      <configurations>
        <configuration key="source-tags">archive</configuration>
      </configurations>
    </operation>
