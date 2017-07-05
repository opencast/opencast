Execute Workflow Operation Handler
=================================

Description
-----------

The Execute Service allows a workflow to run external scripts or applications with any MediaPackage element as arguments.
This provides a flexible way to operate with MediaPackage resources without the need to write Java code or build Opencast
from source. Commands are executed on worker nodes.

There are two execute workflow operations:

* Execute Once: for running a single command that may operate on multiple elements of a mediapackage
* Execute Many: for running a command for each element in a mediapackage that matches the given criteria

Service Configuration
---------------------

The Execute Service configuration in `org.opencastproject.execute.impl.ExecuteServiceImpl.cfg` must be updated to
define which commands may be run:

````
# Load factor
job.load.execute = 1.0

# The list of commands, separated by spaces, which may be run by the Execute Service.
# A value of * means any command is allowed.
# Default: empty (no commands allowed)
#commands.allowed = 
````

If commands.allowed is empty or undefined (the default), the service won't be able to run any commands.
Use the special key * to permit any command to be executed (not recommended for production systems).

To adjust the job load factor for a command, use the load parameter in the workflow operation (defined below)
rather than adjusting the job.load.execute parameter above.

## Parameter substitution

The line containing the arguments to the command (params) allows some placeholders that are substituted
before running the command.

|Placeholder             |Used in |Meaning                                            |
|------------------------|--------|---------------------------------------------------|
|#{id}                   |Execute Once| The Mediapackage ID                                |
|#{flavor(some/flavor)}  |Execute Once| The absolute path of the element matching the specified flavor. If several elements have the same flavor, the first element returned by MediaPackage#getElementsByFlavor is used.
|#{in}                   |Execute Many| The absolute path of the input element             |
|#{out}                  |Execute Once, Execute Many| The absolute path of the output element, formed from the output-filename parameter

## Using custom properties in the argument list

Custom properties can be included in the command line by using the syntax #{name}, where name is the variable name,
as defined in the Execute Service's configuration file or in the global configuration file `custom.properties`.

The substitution will be done in the following order of precedence:

1. Placeholders defined in the table above.
2. Configuration keys defined in `org.opencastproject.execute.impl.ExecuteServiceImpl.cfg`.
3. Configuration keys defined in `custom.properties`.

For instance, suppose you use the Execute Service with the following arguments:

    "John Doe" xyz #{my.property}

the command run will receive that argument list as-is, because my.property is not a valid placeholder, nor 
is it defined in the Execute Service's configuration file or `custom.properties`.

However, if you define my.property in `custom.properties`:
  
    my.property = foo

then the command will get the following argument list:

    "John Doe" xyz foo

If you define the same variable in the Execute Service's configuration file (regardless of whether the variable
is defined in `custom.properties` or not):
 
    my.property = bar

then the actual argument list will be:

    "John Doe" xyz bar

Execute Once Workflow Operation
-------------------------------

This operation handler runs a single command with multiple MediaPackage elements as arguments. 

### Parameter table

All parameters are empty by default if not specified.

|configuration keys|example|description|required?|
|------------------|-------|-----------|---------|
|exec|qtfaststart|The command to run|Yes|
|params| -f -t 15 # {flavor(presentation/distribute)} #{out}|The arguments to the command. This string allows some placeholders for input and output MediaPackage elements (see Parameter Substitution)|Yes
|load|1.5|A floating point estimate of the load imposed on the node by this job|No
|output-filename|outfile.mp4|Specifies the name of the file created by the command (if any), without path information. Used as the last part of the #{out} parameter|No
|expected-type|Track|Specifies the type of MediaPackage element produced by the command: Manifest, Timeline, Track, Catalog, Attachment, Publication, Other|Required if output- filename is present
|target-flavor|presentation/processed|Specifies the flavor of the resulting Mediapackage element created by the command. If no new element is created, this parameter is ignored.|Required if output- filename is present
|target-tags|execservice, -trim|List of tags that will be applied to the resulting Mediapackage element. Tags starting with "-" will be deleted from the element instead, if present. The resulting element may be the same as the input element.|No

### Operation Example

````
<operation
  id="execute-once"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Run command">
  <configurations>
    <configuration key="exec">ges-launch</configuration>
    <configuration key="params">-e #{flavor(presenter/source)} 0 5m14s #{flavor(presentation/source)} 0 14s</configuration>
    <configuration key="output-filename">result.avi</configuration>
    <configuration key="target-flavor">output/joined</configuration>
    <configuration key="target-tags">joined, -tojoin</configuration>
    <configuration key="expected-type">Track</configuration>
  </configurations>
</operation>
````

Execute Many Workflow Operation
-------------------------------

This operation handler filters a set of MediaPackageElements that match certain input conditions and runs a command on each of them.

### Parameter table

All parameters are empty by default if not specified.

|configuration keys|example|description|required?|
|------------------|-------|-----------|---------|
|exec |qtfaststart |The command to run |Yes
|params |-f -t 15 #{in} #{out} |The arguments to the command. This string allows some placeholders for input and output MediaPackage elements (see Parameter Substitution) |Yes
|load|1.5|A floating point estimate of the load imposed on the node by this job|No
|source-flavor |presentation/source |Run the command for any MediaPackage elements with this flavor. Elements must also match the source-tags condition, if present |No
|source-tag|rss, trim, -engage|Run the command for any MediaPackage elements with one of these (comma- separated) tags. If any of them starts with '-', MediaPackage elements containing this tag will be excluded. Elements must also match the source-flavor condition, if present|No
|output-filename|outfile.mp4|Specifies the name of the file created by the command (if any), without path information. Used as the last part of the #{out} parameter|No
|expected-type |Track |Specifies the type of MediaPackage element produced by the command: Manifest, Timeline, Track, Catalog, Attachment, Publication, Other|Required if output- filename is present
|target-flavor |presentation/processed |Specifies the flavor of the resulting Mediapackage element created by the command |Required if output- filename is present
|target-tags |execservice, -trim |List of tags that will be applied to the resulting Mediapackage element. Tags starting with "-" will be deleted from the element instead, if present. The resulting element may be the same as the input element |No


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

