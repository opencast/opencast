Overview
========

In Opencast, the "Statistics" feature can be seen as a set of charts which can be displayed in the Admin UI. Currently,
statistics for three so-called "resource types" are available:

- Statistics for the resource type **EPISODE** are displayed in a tab in the event details dialog.
- Statistics for the resource type **SERIES** are displayed in a tab in the series details dialog.
- Statistics for the resource type **ORGANIZATION** are displayed in the "Statistics" menu of Opencast.

These tabs/menus are only visible if the statistics feature is configured. For the statistics to work, you need a data
source from which Opencast can retrieve the data to display. Currently, [InfluxDB](https://docs.influxdata.com/influxdb)
is the only supported data source.

Architecture
============

A complete setup consists of the following components:

- InfluxDB
- A source which actually generates your data
- A tool which ingests your data into InfluxDB
- Opencast

For example, using Opencast's [_opencast-influxdb-adapter_](https://github.com/opencast/opencast-influxdb-adapter), your
architecture would look like this:

```graphviz dot statistics-architecture.png

/**
Webserver Logs --> opencast-influxdb-adapter --> InfluxDB --> Opencast
**/

digraph G {
  rankdir="LR";
  bgcolor="transparent";
  node[fontsize=8.0, fontname="sans"];

  webserver -> adapter -> influxdb -> opencast;

  webserver[label="Webserver Logs"];
  adapter[label="influxdb-adapter"];
  influxdb[label="InfluxDB"];
  opencast[label="Opencast"];
}
```

Precisely, the Opencast bundle `opencast-statistics-provider-influx` is the one that needs to be able to connect to
InfluxDB using http(s). So the node hosting this bundle needs network access to InfluxDB.

Configuration
=============

Before configuring Opencast, you should have a running InfluxDB instance and should think about how you want your data
to be written to InfluxDB and what your InfluxDB database schema should look like. Specifically, you should think about
retention policies, measurement names, field/tag names and how much you want to
[downsample your data](https://docs.influxdata.com/influxdb/latest/guides/downsampling_and_retention/). If you don't
have any data in your InfluxDB, but want to verify your setup is working, there is some test data provided in the
section [_Verifying Your Setup_](#verify).

InfluxDB Access
---------------

Opencast needs to know how to talk to your InfluxDB instance. Therefore, you should edit the configuration file
`etc/org.opencastproject.statistics.provider.influx.StatisticsProviderInfluxService.cfg` and fill in your influx URI,
username, password, and database name.

Statistics Providers
--------------------

To support the detailed configuration of the charts to be shown in the Admin UI, Opencast has a concept called
_Statistics Providers_. Each statistics provider can be configured separately and for each provider, there is one chart
displayed in the Admin UI.

The configuration files of the providers have to be stored at `etc/statistics` and they have to follow a certain naming
convention. Configuration files of providers using InfluxDB have to be named starting with `influx.`. All provider
configurations have to be in json format. So e.g. _influx.views.episode.sum.json_ would be a valid name.

For each provider, the following properties have to be configured:

- **`id`** has to be a unique identifier and can be chosen freely.
- **`title`** is the title to be displayed with the chart. This can be a translation key.
- **`description`** is the description to be displayed with the chart. This can be a translation key.
- **`resourceType`** tells Opencast to which type of entity the chart refers to. Valid values are `EPISODE`, `SERIES`,
  and`ORGANIZATION`. This is used by Opencast to decide where to display the chart.
- **`sources`** is list of JSON objects, each containing the following fields:
    - **`measurement`**, e.g. `infinite.impressions_daily` tells Opencast that your InfluxDB data retention policy is
      named `infinite` and your InfluxDB measurement name is `impressions_daily`.
    - **`aggregation`**, e.g. `SUM` tells Opencast that InfluxDB's `SUM()` function should be used to calculate the
      values to display in the chart.
    - **`aggregationVariable`**, e.g. `value` tells Opencast that the InfluxDB field which should be summed is named
      `value`.
    - **`resourceIdName`**, e.g. `episodeId` tells Opencast that the InfluxDB tag identifying the resource type this
       provider refers to is named `episodeId`.
    - **`resolutions`** is a list of resolutions supported by this provider. Opencast allows the user to select a
      _resolution_ with which the data is displayed. Valid values are `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY` and
      `YEARLY`. E.g. when a chart shows data of two years, a `DAILY` resolution will lead to 2x365=730 values to be
      plotted while a `MONTHLY` resolution would leave us with 24 values being plotted in the chart.
- **`type`** defines the structure of the data provided by this provider. Currently, `timeseries` and `runningtotal`
  are supported.

Here is an example json configuration for a provider which generates charts for episodes showing the number of views:

`etc/statistics/influx.views.episode.sum.json`

```json
{
  "id": "episode.views.sum.influx",
  "title": "STATISTICS.TITLE.VIEWS_SUM",
  "description": "STATISTICS.DESCRIPTION.VIEWS_SUM",
  "resourceType": "EPISODE",
  "sources": [{
    "measurement": "infinite.impressions_daily",
    "aggregation": "SUM",
    "aggregationVariable": "value",
    "resourceIdName": "episodeId",
    "resolutions": [
      "DAILY",
      "WEEKLY",
      "MONTHLY",
      "YEARLY"
    ]
  }],
  "type": "timeseries"
}
```

CSV Exports
-----------

Statistics can be exported to CSV files by clicking the "download" button in the top right corner of a graph. Per default,
the export will contain the data which the graph currently displays. For series statistics, it is possible to change this
behavior in the way that exported series statistics contain the data of all events of a series instead of just the top
level series data. To enable this, it is necessary to specify which Statistics Provider should be used to get the episode
data. See the configuration file `org.opencastproject.statistics.export.impl.StatisticsExportServiceImpl.cfg` for details.

Using the `runningtotal` provider
-----------

The `runningtotal` statistics provider is a special type of time series statistics provider. To illustrate what it
can be used for, let’s assume we want to track the number of hours of videos per organization (this is actually
what the provider was initially designed for). We create a JSON file for the provider as such:

```json
{
  "id": "organization.publishedhours.influx",
  "title": "STATISTICS.TITLE.PUBLISHEDHOURS",
  "description": "STATISTICS.DESCRIPTION.PUBLISHEDHOURS",
  "resourceType": "ORGANIZATION",
  "sources": [{
    "measurement": "infinite.publishedhours",
    "aggregation": "SUM",
    "aggregationVariable": "hours",
    "resourceIdName": "organizationId",
    "resolutions": [
      "DAILY",
      "WEEKLY",
      "MONTHLY",
      "YEARLY"
    ]
  }],
  "type": "runningtotal"
}
```

Note that the published hours entries can be negative, in case we retract a video.

When the `runningtotal` provider is asked to report on, for example, the monthly hours of video for a specific year,
it will first take the sum of all video lengths _up until_ that year. Then, for each month, it will take the sum of
all the entries in that month, and add it to the previous value. And so on for the next months.

To actually _write_ these hours to the statistics data base, you have to add the `statistics-writer` workflow
operation handler to your workflows. Specifically, somewhere in your publishing workflow, you have to add an entry
such as this:

```XML
<operation
  id="statistics-writer"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Collect video statistics">
  <configurations>
    <configuration key="flavor">presenter/video</configuration>
    <configuration key="retract">false</configuration>
    <configuration key="measurement-name">publishedhours</configuration>
    <configuration key="organization-resource-id-name">organizationId</configuration>
    <configuration key="length-field-name">hours</configuration>
    <configuration key="temporal-resolution">hours</configuration>
  </configurations>
</operation>
```

To _decrement_ the running total of hours in the case of retractions, set the `retract` property to `true`.
In the default case, or when the `retract` property is `false` the running total is not decremented when a
retraction occurs.

Verifying Your Setup<a name="verify"></a>
====================

If you want to test your setup, you can put the following test data into InfluxDB and check if Opencast displays all
charts correctly. First, create a series and an event as part of that series using the Opencast Admin UI. Second, copy
the test data to a file called `testdata.txt` and edit it to match your InfluxDB database schema. Make sure you replace
the `episodeId`, `seriesId`, and `organizazionId` tag value with the correct identifiers of the test event/series you
just created. Also make sure, that the tag names (e.g.) `episodeId` and the field name (`value`) match the ones you have
specified in the `source` strings of your providers. Also, the database name, retention policy name and measurement name
have to match your configuration.

The InfluxDB test data could look like this:

```text
# DDL

CREATE DATABASE opencast

# DML

# CONTEXT-DATABASE: opencast

impressions_daily,episodeId=6d3004a3-a581-4fdd-9dab-d4ed02f125f8,seriesId=5b421e3c-56a5-4c9e-86cd-bedcfa739cfa,organizationId=mh_default_org value=1 1554468810
impressions_daily,episodeId=6d3004a3-a581-4fdd-9dab-d4ed02f125f8,seriesId=5b421e3c-56a5-4c9e-86cd-bedcfa739cfa,organizationId=mh_default_org value=1 1554555210
impressions_daily,episodeId=6d3004a3-a581-4fdd-9dab-d4ed02f125f8,seriesId=5b421e3c-56a5-4c9e-86cd-bedcfa739cfa,organizationId=mh_default_org value=1 1554641610
impressions_daily,episodeId=6d3004a3-a581-4fdd-9dab-d4ed02f125f8,seriesId=5b421e3c-56a5-4c9e-86cd-bedcfa739cfa,organizationId=mh_default_org value=1 1554728010
impressions_daily,episodeId=6d3004a3-a581-4fdd-9dab-d4ed02f125f8,seriesId=5b421e3c-56a5-4c9e-86cd-bedcfa739cfa,organizationId=mh_default_org value=1 1554814410
impressions_daily,episodeId=6d3004a3-a581-4fdd-9dab-d4ed02f125f8,seriesId=5b421e3c-56a5-4c9e-86cd-bedcfa739cfa,organizationId=mh_default_org value=1 1554900810
```
The file format of the InfluxDB test data is described
[here](https://docs.influxdata.com/influxdb/latest/write_protocols/line_protocol_reference/).


You can import the test data into InfluxDB using the following command:

`influx -import -path=testdata.txt -precision=s -database=opencast`

Once you have imported your test data, you should be able to view the charts you have configured when accessing the
event/series details of your test event or Opencast's statistics section.
