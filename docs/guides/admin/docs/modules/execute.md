Execute Service
===============

The Execute Service allows a workflow to run external scripts or applications with any MediaPackage element as arguments.
This provides a flexible way to operate with MediaPackage resources without the need to write Java code or build Opencast
from source. Commands are executed on worker nodes.

There are two execute workflow operations:

* [Execute Once](../workflowoperationhandlers/execute-once-woh.md): for running a single command
   that may operate on multiple elements of a mediapackage
* [Execute Many](../workflowoperationhandlers/execute-many-woh.md): for running a command
   for each element in a mediapackage that matches the given criteria

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

If `commands.allowed` is empty or undefined (the default), the service won't be able to run any commands.
Use the special key `*` to permit any command to be executed (not recommended for production systems).

To adjust the job load factor for a command, use the `load` parameter in the workflow operation rather than
adjusting the `job.load.execute` parameter above.

## Parameter substitution

The command arguments may contain placeholders, which are substituted by their corresponding values before
the command runs. The complete list of available placeholders is detailed in the table below.

|Placeholder            |Used in     |Meaning                                            |
|-----------------------|------------|---------------------------------------------------|
|#{id}                  |Execute Once|The Mediapackage ID                                |
|#{flavor(some/flavor)} |Execute Once|The absolute path of the element matching the specified flavor. If several elements have the same flavor, the first element returned by MediaPackage#getElementsByFlavor is used.
|#{in}                  |Execute Many|The absolute path of the input element             |
|#{out}                 |Execute Once, Execute Many|The absolute path of the output element, formed from the output-filename parameter

## Using custom properties in the argument list

Custom properties can be included in the command line by using the syntax `#{name}`, where name is the variable name,
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

Executing commands in workflows
-------------------------------

For more information on how to execute a command in a workflow, see:

* [Execute Once Workflow Operation](../workflowoperationhandlers/execute-once-woh.md)
* [Execute Many Workflow Operation](../workflowoperationhandlers/execute-many-woh.md)

