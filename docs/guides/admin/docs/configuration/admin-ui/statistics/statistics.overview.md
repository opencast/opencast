# Overview

In Opencast, the "Statistics" feature can be seen as a set of charts which can be displayed in the Admin UI. Currently,
statistics for three so-called "resource types" are available:

- Statistics for the resource type **EPISODE** are displayed in a tab in the event details dialog.
- Statistics for the resource type **SERIES** are displayed in a tab in the series details dialog.
- Statistics for the resource type **ORGANIZATION** are displayed in the "Statistics" menu of Opencast.

These tabs/menus are only visible if the statistics feature is configured. For the statistics to work, you need a data
source from which Opencast can retrieve the data to display. Currently, [InfluxDB](https://docs.influxdata.com/influxdb)
and [Matomo](https://matomo.org/) are the only supported data sources.

## General Configuration

To enable the statistics view in the admin interface, set `prop.admin.statisttics.enabled=true`
in `etc/org.opencastproject.organization-mh_default_org.cfg`.

To support the detailed configuration of the charts to be shown in the Admin UI, Opencast has a concept called
_Statistics Providers_. Each statistics provider can be configured separately and for each provider, there is one chart
displayed in the Admin UI.

The configuration files of the providers have to be stored at `etc/statistics` and they have to follow a certain naming
convention. Configuration files of providers using InfluxDB have to be named starting with `influx.`, configuration files
for providers using Matomo have to start with `matomo`. All provider configurations have to be in json format. So e.g.
_influx.views.episode.sum.json_ would be a valid name. For more details on these configuration files, please refer to the
corresponding section for the statistics provider in question
([InfluxDB Statistics Provider](/configuration/admin-ui/statistics/influxdb-statistics-provider#influxdb-statistics-provider-config),
[Matomo Statistics Provider](/configuration/admin-ui/statistics/matomo-statistics-provider#matomo-statistics-provider-config)).

## CSV Exports

Statistics can be exported to CSV files by clicking the "download" button in the top right corner of a graph. Per default,
the export will contain the data which the graph currently displays. For series statistics, it is possible to change this
behavior in the way that exported series statistics contain the data of all events of a series instead of just the top
level series data. To enable this, it is necessary to specify which Statistics Provider should be used to get the episode
data. See the configuration file `org.opencastproject.statistics.export.impl.StatisticsExportServiceImpl.cfg` for details.
