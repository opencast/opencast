Execute Many Workflow Operation
===============================

ID: `execute-many`

Description
-----------

This operation handler filters a set of MediaPackageElements that match certain input conditions
and runs a command on each of them. The command may be used to create a new mediapackage element,
or to add configuration properties to the running workflow.

To run a command only once for the whole mediapackage, use the [Execute Once](execute-once-woh.md) operation.

Commands run by this operation handler must first be included in the `commands.allowed` list in the
[Execute Service](../configuration/execute.md#service-configuration) configuration.


Parameter Table
---------------

All parameters are empty by default if not specified. The special parameters `#in` and `#out` are described
in [Execute Service: Parameter Substitution](../configuration/execute.md#parameter-substitution)

|Configuration keys| Example                |Description        |Required?|
|------------------|------------------------|-------------------|---------|
|exec              | qtfaststart            |The command to run |Yes      |
|params            | -f -t 15 #{in} #{out}  |The arguments to the command. This string allows some placeholders for input and output MediaPackage elements (see Parameter Substitution) |Yes|
|load              | 1.5                    |A floating point estimate of the load imposed on the node by this job|No|
|set-workflow-properties| true / false           |Import workflow properties from the output file|No|
|source-flavor     | presentation/source    |Run the command for any MediaPackage elements with this flavor. Elements must also match the source-tags condition, if present |No|
|source-tag        | exp, trim, -engage     |Run the command for any MediaPackage elements with one of these (comma- separated) tags. If any of them starts with '-', MediaPackage elements containing this tag will be excluded. Elements must also match the source-flavor condition, if present|No|
|source-audio      | true                   |If present, require the element either to have an audio stream (true) or no audio stream (false), in addition to any source-flavor or source-tag conditions.|No|
|source-video      | true                   |If present, require the element either to have a video stream (true) or no video stream (false), in addition to any source-flavor or source-tag conditions. |No|
|source-subtitle   | true                   |If present, require the element either to have a subtitle stream (true) or no subtitle stream (false), in addition to any source-flavor or source-tag conditions. |No|
|output-filename   | outfile.mp4            |Specifies the name of the file created by the command (if any), without path information. Used as the last part of the #{out} parameter|No|
|expected-type     | Track                  |Specifies the type of MediaPackage element produced by the command: Manifest, Timeline, Track, Catalog, Attachment, Publication, Other|Required if output- filename is present|
|target-flavor     | presentation/processed |Specifies the flavor of the resulting Mediapackage element created by the command |Required if output- filename is present|
|target-tags       | +execservice, -trim    |Apply these (comma separated) tags to any media package elements. If a target-tag starts with a '-', it will be removed from preexisting tags, if a target-tag starts with a '+', it will be added to preexisting tags. If there is no prefix, all preexisting tags are removed and replaced by the target-tags.|No|

If `set-workflow-properties` is true, the command should write a plain-text properties file to the
location specified by #{out} in the key-value format supported by the [Java Properties](http://docs.oracle.com/javase/8/docs/api/java/util/Properties.html#load-java.io.Reader-)
class, for example:

```properties
key1=value1
key2=value2
```

Operation Example
-----------------

Run a command which creates a new version of a track:

```yaml
  - id: execute-many
    description: Run command
    configurations:
      - exec: qt-faststart
      - params: '-f #{in} #{out}'
      - source-flavor: '*/toprocess'
      - source-tags: copy,-exp
      - output-filename: result.avi
      - target-flavor: output/processed
      - target-tags: copied,-copy
      - expected-type: Track
```

Run a command which inspects any track with a `presenter/source` flavor and an audio stream,
and adds new configuration properties to the running workflow, leaving the mediapackage unchanged:


```yaml
  - id: execute-many
    fail-on-error: true
    exception-handler-workflow: error
    description: Inspect track and update workflow properties
    configurations:
      - exec: /usr/local/bin/oc-track-inspect-audio.sh
      - source-flavor: presenter/source
      - source-audio: true
      - params: '#{in} #{out}'
      - set-workflow-properties: true
      - output-filename: wf.properties
      - expected-type: Attachment
```
