Create a Custom Workflow
========================

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
      <title>Encode Mp4, Distribute and Publish</title>
      <tags>
        <!-- Tell the UI where to show this workflow -->
        <tag>upload</tag>
        <tag>schedule</tag>
        <tag>archive</tag>
      </tags>
      <description>
        Encode to Mp4 and thumbnail.
        Distribute to local repository.
        Publish to search index.
      </description>
      <displayOrder>10</displayOrder>

      <!-- Operations -->
      <operations></operations>

    </definition>

* The `id` is used in several Opencast endpoints to identify and select this workflow. Make sure that this identifier
  is unique among all endpoints in the system.
* The `tags` define where the user interfaces may use these workflows. Useful tags are:
    * *upload*: Usable for uploaded media
    * *schedule*: Usable for scheduled events
    * *archive*: Usable for archived media
    * *delete*: Usable for deletion of events with publications
    * *editor*: Usable from the video editor
* The `displayOrder` is an integer that indicates in what order workflow definitions shall be displayed by clients.
  If ommitted, the `displayOrder` defaults to `0`. Clients are expected to list workflow definitions in descending order.


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
    <relation-factor> ::= <operation> | <number>
    <operation> ::= <number> <op-literal> <number>
    <rel-literal> ::= ">=" | ">" | "<=" | "<" | "==" | "!="
    <op-literal> ::= "+" | "-" | "*" | "/"
    <bool-literal> ::= "true" | "false"

As the formal description above explains, such boolean expressions may contain the booelan constants (`true` and
`false`) and numbers, as well as references to the variables of the workflow instance that contain these data types.
Workflow instance variables can be accessed by using `${variableName}`.

Example:

    <operation id="..." if="${variableName1} AND NOT (${variableName2} OR ${variableName3})">
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

At any given point of time, there is exactly one thumbnail available for each published event.
There are three different kinds of thumbnails:

* Default thumbnail
* Uploaded thumbnail
* Snapshot thumbnail

### Default Thumbnail

This thumbnail is supposed to be automatically generated without user interaction. It is extracted from the time
position as you define it in your workflow.

If you are not using the default time position, be sure to adapt the value of `thumbnail.default.position` in
`etc/org.opencastproject.adminui.cfg`. This is required to allow the Admin UI to dynamically generate a preview
image for the default thumbnail.

### Uploaded Thumbnail

The Admin UI supports the upload of an image to be used as thumbnail. This thumbnail image will available to
workflows for further processing as media package attachment with a configurable flavour
(default: `thumbnail/source`).

If you need another flavor for the thumbnail in your specific environment, be sure to adapt the value of
`thumbnail.source.flavor.type` and `thumbnail.source.flavor.subtype` in `etc/org.opencastproject.adminui.cfg`
so that the Admin UI adds the thumbnail images with the expected flavour.

### Snapshot Thumbnail

As a balance between the quality of the thumbnail (default) and the effort to create a thumbnail (upload), the Admin UI
supports thumbnails that are be extracted from existing tracks. This allows users to take a snapshot at an arbitrary
time position from a source track by simply selecting the position and pressing a button.

The variable `thumbnailPosition` holds the time value (in seconds as floating point) and the variable `thumbnailTrack`
holds the flavour type of the track where that thumbnail is supposed to be extracted.

Please note that `thumbnailPosition` refers to the (unmodified) track identified by  `thumbnailTrack`.

### Thumbnail Preview

The Admin UI is capable of presenting a preview image of the currently used thumbnail which a) must be
created by the workflow that generates previews for the Admin UI initially and b) will be created and re-distributed
by the Admin UI each time the thumbnail is changed.

The Admin UI manages this preview image of the thumbnail in the publication channel `internal`. To make this thumbnail
preview work in environments that use custom flavors, a number of configuration options are available in
`etc/org.opencastproject.adminui.cfg`:

Option                            | Default           | Description
:---------------------------------|:------------------|:-----------
thumbnail.preview.flavor          | thumbnail/preview | The flavor of the thumbnail preview image
thumbnail.default.position        | 1.0               | The time position where the default thumbnail is extracted at
thumbnail.default.track.primary   | presenter         | The flavor type of the track where the default thumbnail is extracted
thumbnail.default.track.secondary | presentation      | The flavor type of the track where the default thumbnail is extracted in case no track with flavor type thumbnail.default.track.primary is available

Note that the preview image for the default thumbnail needs to be initially created and published which usually is done
in the workflow definition responsible for creating the previews required by the Admin UI.

## Test the Workflow

The easiest way to test a workflow is to just put it into the workflow folder where it will be picked up by Opencast
automatically and will be available in Opencast a few seconds later.
