# CopyWorkflowOperationHandler

## Description
The CopyWorkflowOperationHandler can be used to copy media package elements to a given target directory.

## Parameter Table

|Configuration Key|Example           |Description                                       |
|-----------------|------------------|--------------------------------------------------|
|source-flavors    |presenter/source |Comma-separated list of source-flavors            |
|source-tags       |archive          |Comma-separated list of source-tags               |
|target-directory* |/mnt/mydisk      |The directory where the file is copied to         |
|target-filename   |test             |The optional target filename. The file extension extract from the media package element URI will be appended|

\* mandatory configuration key

Notes:

* *source-flavors* and *source-tags* may be used both together to select media package elements based on both flavors and tags
* In case that neither *source-flavors* nor *source-tags* are specified, the operation will be skipped
* In case no media package elements match *source-flavors* and *source-tags*, the operation will be skipped

## Target Filenames
If *target-filename* is not specified, the filename for each media package element is extracted from the media package element URI. If *target-filename* is specified, the filename is the result of appending the file extension (extracted from the media package element URI) to *target-filename*. 
In case the *source-flavors* and *source-tags* match mutliple media package elements, a sequentially increasing integer number (starting at 1) can be used within *target-filename* in Java string formatting manner to ensure unique filenames.

##Operation Example

    <operation id="copy"
             description="Copy sources to my disk"
             fail-on-error="true"
             exception-handler-workflow="partial-error">
    <configurations>
      <configuration key="source-flavors">presenter/source, presentation/source</configuration>
      <configuration key="target-directory">/mnt/mydisk</configuration>
    </configurations>
  </operation>

