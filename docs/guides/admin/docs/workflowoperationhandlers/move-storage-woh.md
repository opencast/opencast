# MoveStorageOperationHandler

## Description
The MoveStorageOperationHandler can be used to move files in the Asset Manager from one storage system to another.

## Parameter Table

|Configuration Key|Example           |Description                                       |
|:----------------|:----------------:|:-------------------------------------------------|
|target-storage*  |local-storage     |The ID of the storage to move the files to        |
|target-version   |0                 |The (optional) snapshot version to move           |

\* mandatory configuration key

Notes:

* Omitting `target-version` will move **all** current versions of the mediapackage to `target-storage`.  An example
  usecase would be moving the raw input media to a cold(er) storage system after initial processing.

## Operation Example

    <operation
      id="move-storage"
      description="Offloading to AWS S3">
      <configurations>
        <configuration key="target-storage">aws-s3</configuration>
      </configurations>
    </operation>
