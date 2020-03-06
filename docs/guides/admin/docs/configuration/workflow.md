Create a Custom Workflow
========================

Creating custom workflows can be complex.  Some members of the community have contributed their production workflows to
a public repo.

> [Community Workflow Repository](https://github.com/opencast/community-workflows)

Please feel free to contribute your workflows when you have Opencast in production!

This document will help you get started with creating your own Opencast workflows. For a list of available workflow
operations, see:

> [List of Workflow Operation Handler](../workflowoperationhandlers/index.md)

## Overview

A Opencast workflow is an ordered list of operations. There is no limit to the number of operations or their
repetition in a given workflow.

Workflow operations can be configured using configuration elements. The use of string replacement in configuration
values allows workflows to dynamically adapt to a given input or user decision.

### Document

Opencast workflows are defined in XML.  The structure of a Opencast workflow looks like this:

    <definition xmlns="http://workflow.opencastproject.org">

      <!-- Description -->
      <id></id>
      <title></title>
      <tags></tags>
      <description></description>
      <displayOrder></displayOrder>

      <!-- Operations -->
      <operations>
        <operation></operation>
        ...
      </operations>

    </definition>

## Create a Workflow

This sections will walk you through creating a custom workflow, which will encode ingested tracks to defined output
format.

### Encoding Profiles

First create or select the encoding profiles you want to use. For more details on this, have a look at the [Encoding
Profile Configuration Guide](encoding.md). For this guide we assume that we have an encoding profile `mov-low.http`
which creates a distribution format definition for mp4 video and a `feed-cover.http` encoding profile to create
thumbnail images for the videos.

### Describe the Workflow

Start by naming the workflow and giving it a meaningful description:

    <definition xmlns="http://workflow.opencastproject.org">

      <!-- Description -->
      <id>example</id>
      <!-- Optionally specify an organization -->
      <organization>mh_default_org</organization>
      <!-- optionally specify roles for this workflow -->
      <roles>
        <role>ROLE_ADMIN</role>
      </roles>
      <title>Encode Mp4, Distribute and Publish</title>
      <tags>
        <!-- Tell the UI where to show this workflow -->
        <tag>upload</tag>
        <tag>schedule</tag>
        <tag>archive</tag>
      </tags>
      <description>
        1. Encode to Mp4 and thumbnail.

        2. Distribute to local repository.

        3. Publish to search index.
      </description>
      <displayOrder>10</displayOrder>

      <!-- Operations -->
      <operations></operations>

    </definition>

* The `id` is used in several Opencast endpoints to identify and select this workflow. Make sure that this identifier
  is unique among all endpoints in the system (except in multitenant workflows, see `organization` below).
* The `organization` specifies the organization this workflow is valid for (thus, it only makes sense in multitenant
  installations). If there are two workflows with the same id, the one corresponding to the user’s organization is
  always chosen. This pertains workflow dropdowns (for example, the “Add new event” dropdown) as well as workflows
  included in other workflows via the `include` workflow operation handler.
* The `roles` define which user roles are allowed to see and start this workflow (a user needs one of the roles provided
  in the definition). If this is omitted or no roles are specified, everyone can see and start the workflow (provided
  the `organization` constraints are satisfied). Also, users with `ROLE_ADMIN` can see and start every workflow. Note
  that the workflows included in Opencast do not set roles.
* The `tags` define where the user interfaces may use these workflows. Useful tags are:
    * *upload*: Usable for uploaded media
    * *schedule*: Usable for scheduled events
    * *archive*: Usable for archived media
    * *delete*: Usable for deletion of events with publications
    * *editor*: Usable from the video editor
* The `displayOrder` is an integer that indicates in what order workflow definitions shall be displayed by clients.
  If ommitted, the `displayOrder` defaults to `0`. Clients are expected to list workflow definitions in descending order.
* The `description` allows you to describe the workflow in detail. Blank lines are formatted as newlines, while single
  line breaks are ignored so that the XML remains compact and readable even with long paragraphs.


### Inspect the Media

The first operation will be to inspect the media for technical metadata, such as format and length:

    <definition xmlns="http://workflow.opencastproject.org">

      <!-- Description -->
      ...

      <!-- Operations -->
      <operations>

        <!-- inspect media -->
        <operation
          id="inspect"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Inspect media package">
        </operation>

      </operations>

    </definition>

The *fail-on-error* attribute is a boolean determining whether the workflow will throw an error to the
exception-handler-workflow or simply proceed with the remaining operations.

### Encoding

The next operations will encode the media to the Mp4 format:

    <definition xmlns="http://workflow.opencastproject.org">

      <!-- Description -->
      ...

      <!-- Operations -->
      <operations>

        <!-- inspect media -->
        ...

        <!-- encode: mp4 -->
        <operation
          id="compose"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Encode camera to mp4">
          <configurations>
            <configuration key="source-flavor">presenter/source</configuration>
            <configuration key="target-flavor">presenter/delivery</configuration>
            <configuration key="target-tags">rss, atom</configuration>
            <configuration key="encoding-profile">mov-low.http</configuration>
          </configurations>
        </operation>

        <operation
          id="compose"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Encode screen to mp4">
          <configurations>
            <configuration key="source-flavor">presentation/source</configuration>
            <configuration key="target-flavor">presentation/delivery</configuration>
            <configuration key="target-tags">rss, atom</configuration>
            <configuration key="encoding-profile">mov-low.http</configuration>
          </configurations>
        </operation>

      </operations>

    </definition>


* The `target-tags` attribute causes the resulting media to be tagged. For example, this could be used to define these
  media as input for other operations, using their `source-tags` attribute.
* The `encoding-profile` attribute refers to an encoding profile defined in `etc/encoding`.


### Encode to Thumbnail

The next operations will create thumbnails from the media:

    <definition xmlns="http://workflow.opencastproject.org">
      ...
      <operations>
        ...
        <!-- encode: images -->
        <operation
          id="image"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Encode camera to thumbnail">
          <configurations>
            <configuration key="source-flavor">presenter/source</configuration>
            <configuration key="source-tags"></configuration>
            <configuration key="target-flavor">cover/source</configuration>
            <configuration key="target-tags">rss, atom</configuration>
            <configuration key="encoding-profile">feed-cover.http</configuration>
            <configuration key="time">1</configuration>
          </configurations>
        </operation>

        <operation
          id="image"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Encode screen to thumbnail">
          <configurations>
            <configuration key="source-flavor">presentation/source</configuration>
            <configuration key="source-tags"></configuration>
            <configuration key="target-flavor">cover/source</configuration>
            <configuration key="target-tags">rss, atom</configuration>
            <configuration key="encoding-profile">feed-cover.http</configuration>
            <configuration key="time">1</configuration>
          </configurations>
        </operation>

      </operations>

    </definition>

* The time attribute determines the approximate frame of the source media is used. The time unit is in seconds.

### Distribute the Media

The next operation copies the encoded media to the Opencast distribution channel:

    <definition xmlns="http://workflow.opencastproject.org">
      ...
      <operations>

        <!-- distribute: local -->
        <operation
          id="publish-engage"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Distribute media to the local distribution channel">
          <configurations>
            <configuration key="download-source-tags">publish,rss,atom</configuration>
            <configuration key="streaming-source-tags"></configuration>
            <configuration key="check-availability">true</configuration>
          </configurations>
        </operation>

      </operations>

    </definition>

* The publish-engage operation uses all media tagged as *rss* or *atom* as input.

## Accept User Input

Workflow definitions may optionally include variables to be replaced by user input. For instance, this may be used to
select optional parts of a workflow. To enable user control of individual workflow instances, the workflow definition
must:

* use the `${variable}` notation in the workflow definition
* contain a custom configuration panel.

Here is an example of a configurable operation:

    <operation id="..." if="${somevar}">
      ...
    </operation>

The attribute `if` specifies the execution condition in means of the operation only being executed if that condition
evaluates to true. You can find more details on conditional execution in the next section.

Once the operation is configured to accept a variable, we need to describe how to gather the value from the
administrative user. The `<configuration_panel>` element of a workflow definitions describes this user interface
snippet.  A simple configuration panel could look like this:

    <configuration_panel>
      <![CDATA[
        <input id="someaction" name="someaction" type="checkbox" value="true" />
        <label for="someaction">Execute some operation?</label>
      ]]>
    </configuration_panel>

The checkbox in this `<configuration_panel>` will now be displayed in the administrative tools, and the user's
selection will be used to replace the `${someaction}` variable in the workflow.

This input can also be sent by capture agents, using the ingest endpoints. Please note that capture agents usually do
not load the configuration panel. Hence defaults set in the user interface will not apply to ingests. To circumvent
this, the [defaults operation](../workflowoperationhandlers/defaults-woh.md) can be used.

## Conditional Execution

The attribute `if` of the `operation` element can be used to specify a condition to control whether the workflow
operation should be executed. This so-called execution condition is a boolean expression of the following form:

    <expression> ::= <term> ["OR" <expression>]
    <term> ::= <value> ["AND" <term>]
    <value> ::= ["NOT"]* ( "(" <expression> ")" | <relation> | <bool-literal> )
    <relation> ::= <relation-factor> <rel-literal> <relation-factor>
    <relation-factor> ::= <operation> | <atom>
    <operation> ::= <atom> <op-literal> <atom>
    <rel-literal> ::= ">=" | ">" | "<=" | "<" | "==" | "!="
    <op-literal> ::= "+" | "-" | "*" | "/"
    <bool-literal> ::= "true" | "false"
    <atom> ::= <number> | <string>

As the formal description above explains, such boolean expressions may contain…

- …the boolean constants `true` and `false`.
- …numbers, which may contain a decimal point.
- …strings, which must be surrounded by single-quotes. Escaping of single quotes is supported, just use two single
  quotes next to each other: `'foo''bar'`
- …as well as references to the variables of the workflow instance that contain these data types. Variables
  are enclosed in in `${}`, as shown below. A default value may be specified for a variable, after the name, 
  separated by a colon, as such: `${foo:1}`. The default value will be used in case the variable doesn’t exist. 
  If no default value is specified, `false` will be used. This, of course, only makes sense in boolean contexts. Be
  aware to specify a default value in relations such as `${foo} < ${bar}`.

Example for simple boolean expressions:

    <operation id="..." if="${variableName1} AND NOT (${variableName2} OR ${variableName3})">
      …
    </operation>

Example for string comparisons:

    <operation id="..." if="${captureAgentVendor} == 'ACME Corporation'">
      …
    </operation>

Note that operations containing strings and numbers are somewhat well-behaved, for example, the following operation
gets executed because `3` is converted to a string and then added to the string `'4'`:

    <operation id="..." if="3+'4' == '34'">
      …
    </operation>

Note that XML requires certain characters like the `<` and `>` operators to be written as XML entities. Even if they are
used quoted in attributes. The following table shows all those characters:

```no-highlight
"  →  &quot;
'  →  &apos;
<  →  &lt;
>  →  &gt;
&  →  &amp;
```

Example:

    <operation id="..." if="${yresolution} &gt; 720">
      …
    </operation>


## Thumbnail Support

The Admin UI comes with explicit support for thumbnails that are supposed to represent events visually, e.g. in lists
of events as commonly used in video portals and other similar systems.
To make it possible to implement the required processing and retain flexibility, the Admin UI will store the following
information in variables of workflow instances:

Variable          | Description
:-----------------|:-----------
thumbnailType     | The type of the thumbnail as number (see table below)
thumbnailPosition | The time position in case of snapshot thumbnails
thumbnailTrack    | The source track in case of snapshot thumbnails

Thumbnail Type | Description
:--------------|:-----------
0              | The default thumbnail shall be extracted at a configured time position
1              | The thumbnail has been uploaded and is stored in the asset manager as media package attachment
2              | The thumbnail shall be extracted at a given time position from a given track

To fully support the thumbnail feature, your workflows should take care of creating the different types of thumbnails
and be consistent to the Admin UI thumbnail configuration (see [Thumbnail Configuration](admin-ui/thumbnails.md))

## Test the Workflow

The easiest way to test a workflow is to just put it into the workflow folder where it will be picked up by Opencast
automatically and will be available in Opencast a few seconds later.
