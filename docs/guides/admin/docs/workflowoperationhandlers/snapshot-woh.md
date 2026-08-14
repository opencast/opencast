Asset Manager Snapshot Workflow Operation
=========================================

ID: `snapshot`

Description
-----------

The snapshot operation allows you to take a new, versioned snapshot of a media package which is put into the asset
manager.


Parameter Table
---------------

|configuration keys|example         |description|
|------------------|----------------|-----------|
|source-tags       |text            |Comma separated list of tags. Specifies which media should be the source of a snapshot.|
|source-flavors    |presenter/source|Comma separated list of flavors. Specifies which media should be the source of a snapshot.|

If neither tags nor flavors are configured, all elements of the media package are included.

Operation Example
-----------------

```yaml
  - id: snapshot
    description: Archiving
    configurations:
      - source-tags: archive
```
