Execute Many Workflow Operation
-------------------------------

This operation handler filters a set of MediaPackageElements that match certain input conditions and runs a command on each of them.

To run a command only once for the whole mediapackage, use the [Execute Once](execute-once-woh.md) operation.

Commands run by this operation handler must first be included in the `commands.allowed` list in the
[Execute Service](../modules/execute.md#service-configuration) configuration.

### Parameter table

All parameters are empty by default if not specified. The special parameters `#in` and `#out` are described
in [Execute Service: Parameter Substitution](../modules/execute.md#parameter-substitution)

|Configuration keys|Example               |Description        |Required?|
|------------------|----------------------|-------------------|---------|
|exec              |qtfaststart           |The command to run |Yes      |
|params            |-f -t 15 #{in} #{out} |The arguments to the command. This string allows some placeholders for input and output MediaPackage elements (see Parameter Substitution) |Yes|
|load              |1.5                   |A floating point estimate of the load imposed on the node by this job|No|
|source-flavor     |presentation/source   |Run the command for any MediaPackage elements with this flavor. Elements must also match the source-tags condition, if present |No|
|source-tag        |rss, trim, -engage    |Run the command for any MediaPackage elements with one of these (comma- separated) tags. If any of them starts with '-', MediaPackage elements containing this tag will be excluded. Elements must also match the source-flavor condition, if present|No|
|output-filename   |outfile.mp4           |Specifies the name of the file created by the command (if any), without path information. Used as the last part of the #{out} parameter|No|
|expected-type     |Track                 |Specifies the type of MediaPackage element produced by the command: Manifest, Timeline, Track, Catalog, Attachment, Publication, Other|Required if output- filename is present|
|target-flavor     |presentation/processed|Specifies the flavor of the resulting Mediapackage element created by the command |Required if output- filename is present|
|target-tags       |execservice, -trim    |List of tags that will be applied to the resulting Mediapackage element. Tags starting with "-" will be deleted from the element instead, if present. The resulting element may be the same as the input element |No|

### Operation Example

````
<operation
  id="execute-many"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Run command">
  <configurations>
    <configuration key="exec">qt-faststart</configuration>
    <configuration key="params">-f #{in} #{out}</configuration>
    <configuration key="source-flavor">*/toprocess</configuration>
    <configuration key="source-tags">copy, -rss</configuration>
    <configuration key="output-filename">result.avi</configuration>
    <configuration key="target-flavor">output/processed</configuration>
    <configuration key="target-tags">copied, -copy</configuration>
    <configuration key="expected-type">Track</configuration>
  </configurations>
</operation>
````

