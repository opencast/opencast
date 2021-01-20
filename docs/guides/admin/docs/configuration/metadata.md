Overview
========================

In Opencast, metadata is stored in so-called metadata catalogs. For each event or series, an arbitrary number of
such configurable metadata catalogs can be managed. A common set of metadata has been standardized to form a
common basis (standard metadata), whereas administrators can configure Opencast to support other metadata sets
(extended metadata).

This document provides an overview of Opencast's metadata capabilities and its configuration.

## Standard Metadata

For both events and series, a common set of metadata is supported by Opencast out of the box. Since metadata catalogs
are referenced from within media package, flavors can be used to identify a specific metadata catalog. The following
flavors are treated by Opencast as standard metadata, meaning Opencast expects them to be present:

* `dublincore/episode` holds the standard metadata of an event
* `dublincore/series` holds the standard metadata of a series

Opencast assumes specific metadata fields to be present in the standard metadata in means of defining hard-coded
filters, table columns and search indices.

To adjust the standard metadata to your specific needs, you can configure them in
`etc/org.opencastproject.ui.metadata.CatalogUIAdapterFactory-episode-common.cfg` and
`etc/org.opencastproject.ui.metadata.CatalogUIAdapterFactory-series-common.cfg`.

For details on how to configure metadata catalogs, see the section Metadata Catalog Configuration.

As mentioned above, however, Opencast expects specific metadata fields to be present to work correctly. In case you want
to map metadata specific to your use case, you might consider using the extended metadata capbilities of Opencast
described in the next section.

## Extended Metadata

For both events and series, Opencast support an arbitrary number of customized metadata catalogs.

To add extended metadata catalogs, create a configuration file with a valid filename of the form
`org.opencastproject.ui.metadata.CatalogUIAdapterFactory-<name>.cfg` in `etc/`. on the admin node.

For details on how to configure metadata catalogs, see the section Metadata Catalog Configuration.

Limitations:

* Cannot be sorted, searched or filtered
* Cannot be displayed in tables

## Metadata Catalog Configuration

The metadata configuration file format can be logically split up into different parts:

### Part 1: General catalog information

|Configuration key|Example                  |Description                                                               |
|-----------------|-------------------------|--------------------------------------------------------------------------|
|type             |events                   |Two different types of catalog UI adapters may be configured, such for events and others for series.|
|organization     |mh_default_org           |A custom catalog definition is mapped 1:1 to an organization and is available to this one organization only.|
|flavor           |mycompany/episode        |The catalog must be of a certain flavor. For a events catalog, the flavor consists of the form type/subtype whereas for series you only need to define the subtype. Attention: For series catalogs, the type (the part before the slash '/') is used as element type.|
|title            |My Personal Catalog Name |This is the title that is displayed in the UI. It should be something that is readable by humans.|

### Part 2: XML serialization information

The only supported serialization of catalogs is currently the XML file format. The file follows the recommendation of
the Dublin Core Metadata Initiative.

|Configuration key             |Example                           |Description                                         |
|------------------------------|----------------------------------|----------------------------------------------------|
|xml.rootElement.name          |mycatalog                         |The name of the XML root element                    |
|xml.rootElement.namespace.URI |http://myorg.com/metadata/catalog |The URI of the XML namespace of the root element    |

**Namespace bindings**

To properly serialize to XML each prefix has to be bound to an XML namespace. Multiple namespace bindings can be
configured, each identified by its unique name.

|Configuration key                  |Example                         |Description                                      |
|-----------------------------------|--------------------------------|-------------------------------------------------|
|xml.namespaceBinding.{name}.URI    |http://myorg.com/metadata/terms |The URI of the XML namespace                     |
|xml.namespaceBinding.{name}.prefix |myterms                         |The prefix used to identify elements of the namespace|

### Part 3: Catalog fields configuration

`{field-id}` must be a unique identifier for each property for a given catalog and can be the same as the input or
output id to make it easy to find.

|Configuration key |Example |Description |
|------------------|--------|------------|
|property.{field-id}.inputID¹ |title |The id used to identify this property in the catalog e.g. The name of the property inside the xml file of a Dublin Core catalog. If an outputID is not specified then this inputID is used for both the catalog and the front end id. This value is mandatory.|
|property.{field-id}.outputID |title |The id used inside the json for this property. If this value is missing then the inputID will be used instead.|
|property.{field-id}.namespace |http://purl.org/dc/terms/ |The URL that represents the namespace for this property. Different properties in the same catalog can have different namespaces.|
|property.{field-id}.label¹ |"EVENTS.EVENTS.DETAILS.METADATA.TITLE" or "Event Title" |The label to show for this property in the UI. If there is a i18n support for a label that should be the value used so that it will be translated, if you don't mind it being locked to one translation just put that single value in.|
|property.{field-id}.type¹ |text |The type of the metadata field. |
|property.{field-id}.pattern |yyyy-MM-dd |Applies to date and time types for now. It is used to format their values using the java DateTimeFormatter values²|
|property.{field-id}.delimiter |;|For mixed_text and iterable_text type fields, a string at which inputs into the corresponding fields are split into individual values for easier bulk entry of lists. The default is no delimiter, in which case no splitting takes place.|
|property.{field-id}.readOnly¹ |false |If the property can be edited in the UI or if it should only be displayed. |
|property.{field-id}.required¹ |true |If the property has to have a value before the metadata can be saved (the UI's save button will be disabled until all of the required fields are entered)|
|property.{field-id}.collectionID |USERS |The id of the list provider that will be used to validate the input in the backend. So for example entering a username that doesn't exist will throw an error in this case.|
|property.{field-id}.listprovider |USERS  |The id of the list provider that will be used as a drop down option for that field. So for example using the USERS list provider means that in the front end the user will be able to choose the field value from the list of users in Opencast.|
|property.{field-id}.order |3 |Defines the order of properties where this property should be oriented in the UI i.e. 0 means the property should come first, 1 means it should come second etc. Giving two properties the same number will cause them to be next to one another but doesn't guarantee one above or below the other.|

¹ Mandatory field attribute

² See [DateTimeFormatter](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)

**Field types**

|Type          |Description                     |Example value in catalog  |Example value in UI  |JSON response example|
|--------------|--------------------------------|--------------------------|---------------------|---------------------|
|boolean       |Represents a true / false value in the UI that is represented by a check box.|false|false|             |
|date          |A Java Date object that can include the year, month, day, hour, minute second ... and is formatted by the pattern value. |2014-12-10T16:29:43Z |2014-12-10| |
|text          |A text input value for entering in one line of text. It supports more, it just won't increase in size for the interface. |This is the Title |This is the Title| |
|text_long     |A textarea which allows for more than 1 row of text|Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.|Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. | {<br>"id": "notesEpisode",<br>"readOnly": false,<br>"value": "",<br>"label": "Notes",<br>"required": false,<br>"type":"text_long"<br>}|
|iterable_text |A text input value for entering in a list of text objects that are comma separated in the front end but stored separately in the catalog.|<presenter>Adam,Basil,Lukas</presenter>|value : ["Adam","Basil","Lukas"]|{<br>"id": "contributor",<br> "readOnly": true,<br>"value": ["Adam", "Basil", "Lukas"],<br>"label": "Contributor(s)",<br>"required": false,<br>"type": "text"<br>}|
|start_date   |The start date portion of a Dublin Core Catalog Period. |start=2014-11-04T19:00:00Z; end=2014-11-05T20:00:00Z; scheme=W3C-DTF; |2014-11-04| |
|start_time   |The start time portion of a Dublin Core Catalog Period. |start=2014-11-04T19:00:00Z; end=2014-11-05T20:00:00Z; scheme=W3C-DTF; |19:00:00 | |
|duration     |The duration of the event portion of a Dublin Core Catalog Period.|start=2014-11-04T19:00:00Z; end=2014-11-05T20:00:00Z; scheme=W3C-DTF; |01:00:00 | |

**Workflow Configuration**

Since the extended metadata don't have the `dublincore/*` flavor, a tagging operation for the archive has to be added
for the extended catalogs.
In our examples below, we use ext/episode as a flavor, so the following operation should be added to the workflows

    <!-- Tag the extended metadata catalogs for publishing -->
    <operation
        id="tag"
        description="Tagging extended metadata catalogs for archival and/or publication">
        <configurations>
            <configuration key="source-flavors">ext/*</configuration>
            <configuration key="target-tags">+archive</configuration>
        </configurations>
    </operation>


If you want the extended metadata to be published the same way as the standard metadata, you can update the existing
tagging operation for dublincore metadata the following way

    <!-- Tag the incoming metadata catalogs for publishing -->
    <operation
      id="tag"
      description="Tagging metadata catalogs for archival and publication">
      <configurations>
        <configuration key="source-flavors">dublincore/*,ext/*</configuration>
        <configuration key="target-tags">+archive,+engage-download</configuration>
      </configurations>
    </operation>

## Configuring the events publisher metadata field

The metadata field can be used in two ways, and its meaning varies slightly:

* The publisher is the creator of the event: when an event is created, this field is filled automatically with the
logged in user. It cannot be modified on creation of the event nor later.
* The publisher is responsible for uploading the content but may not be the creator of the event in the UI:
in this case, when the event is created, the publisher is selected from a list provider that includes the logged in user
(selected by default) and it is also modifiable later, but then the logged in user is not selectable.

The configuration is done in the file: `etc/org.opencastproject.ui.metadata.CatalogUIAdapterFactory-episode-common.cfg`.

First option is the default one and the configuration is as follows:

    property.publisher.inputID=publisher
    property.publisher.label=EVENTS.EVENTS.DETAILS.METADATA.PUBLISHER
    property.publisher.type=text
    property.publisher.readOnly=true
    property.publisher.required=false
    property.publisher.order=16

To configure the second option:

    property.publisher.inputID=publisher
    property.publisher.label=EVENTS.EVENTS.DETAILS.METADATA.PUBLISHER
    property.publisher.type=text
    property.publisher.readOnly=false
    property.publisher.required=true
    property.publisher.listprovider=YOUR_LIST_PROVIDER
    property.publisher.order=16

If you want to use the publishers as list provider, you must set up the provider in this way:

    property.publisher.listprovider=EVENTS.PUBLISHER

In both cases, you can filter events by publisher.
