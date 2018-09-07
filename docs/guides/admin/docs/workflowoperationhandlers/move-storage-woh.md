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

    <operation id="copy"
             description="Copy sources to my disk"
             fail-on-error="true"
             exception-handler-workflow="partial-error">
    <configurations>
      <configuration key="source-flavors">presenter/source, presentation/source</configuration>
      <configuration key="target-directory">/mnt/mydisk</configuration>
    </configurations>
  </operation>

