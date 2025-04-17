# Matomo Statistics Provider

## Preconditions

Before you can start using the Matomo Statistics Provider, make sure that the following conditions are met:

- You have a running instance of [Matomo](https://matomo.org) and did set up a website in Matomo
- Configure CustomDimensions for event and series in Matomo.
- Configure the Paella Player to send statistic data including event/series IDs to your Matomo instance
- Create an auth token for your Matomo instance to give access to the Matomo Reporting API
- Optionally activate the [Matomo Media Analytics Plugin](https://plugins.matomo.org/MediaAnalytics) (paid plugin) to
  collect more detailed user statistics

### Paella Player Configuration

Follow the instructions for the configuration of the
[Paella Player Matomo user tracking plugin](/configuration/player/paella.player7/plugins/org.opencast.paella.matomo.userTrackingDataPlugin).
Make sure that you use the same CustomDimension IDs in the Paella configuration that were assigned to the CustomDimensions
in Matomo.

If you are using the Matomo Media Analytics plugin, make sure you configure `videoId` as the value for the `mediaAnalyticsTitle`
parameter. This will allow the correct statistics to be assigned to the correct event in Opencast.

## Access to Matomo

You will need to provide the URL of your Matomo instance and the Matomo auth token in the following config file in order
for Opencast to be able to connect to Matomo: `etc/org.opencastproject.statistics.provider.matomo.StatisticsProviderMatomoService.cfg`

```bash
# The URL to use to connect to Matomo
# Default: http://your-matomo-instance.org/
matomo.api.url=https://your-matomo-instance.org/

# The Matomo API token to use to connect to Matomo
matomo.api.token=0123456789abcdef0123456789abcdef
```

## Matomo Statistics Provider Configuration Files<a name="matomo-statistics-provider-config"></a>

For each Statistic Provider (which corresponds to one graph in the admin UI) you have to create a configuration file in
`etc/statistics`. Configuration files of providers using Matomo have to be named starting with `matomo.`. All provider
configurations have to be in json format. So e.g. _matomo.episode.views.sum.json_ would be a valid name.

For each provider, the following properties have to be configured:

- **`id`**: has to be a unique identifier and can be chosen freely.
- **`title`**: is the title to be displayed with the chart. This can be a translation key.
- **`description`**: is the description to be displayed with the chart. This can be a translation key.
- **`resourceType`**: tells Opencast to which type of entity the chart refers to. Valid values are `EPISODE`, `SERIES`,
  and`ORGANIZATION`. This is used by Opencast to decide where to display the chart.
- **`sources`**: is a list of JSON objects, each containing the following fields:
    - **`siteId`**: Matomo ID of your website
    - **`method`**: [Matomo Reporting API method](https://developer.matomo.org/api-reference/reporting-api), e.g.
      `CustomDimensions.getCustomDimension` to get statistics for a CustomDimension or `MediaAnalytics.getVideoTitles`
      for statistics provided by the Media Analytics Plugin
    - **`dimensionId`**: Matomo CustomDimension ID, only necessary if `"method": "CustomDimensions.getCustomDimension"`
    - **`aggregation`**: e.g. `SUM` to tell Opencast to calculate a sum of all values
    - **`aggregationVariable`**:
     [Matomo metrics value](https://developer.matomo.org/api-reference/reporting-api#api-response-metric-definitions)
     available in the response of the Matomo Reporting API method, e.g. `nb_visits` for the method
     `CustomDimensions.getCustomDimension` or `avg_time_watched` for `MediaAnalytics.getVideoTitles`
    - **`resolutions`**: is a list of resolutions supported by this provider. Opencast allows the user to select a
      _resolution_ with which the data is displayed. Valid values are `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY` and
      `YEARLY`. E.g. when a chart shows data of two years, a `DAILY` resolution will lead to 2x365=730 values to be
      plotted while a `MONTHLY` resolution would leave us with 24 values being plotted in the chart.
- **`type`**: defines the structure of the data provided by this provider. Currently, `timeseries` and `runningtotal`
  are supported.

### Configuration Example to Access CustomDimension Statistics

The following example generates charts for series with information about the number of visits. The CustomDimension with
ID 2 is used to store the series IDs within Matomo.

`etc/statistics/matomo.series.views.sum.json`
```json
{
    "id": "series.views.sum.matomo",
    "title": "STATISTICS.TITLE.VIEWS_SUM",
    "description": "STATISTICS.DESCRIPTION.VIEWS_SUM",
    "resourceType": "SERIES",
    "sources": [{
      "siteId": "1",
      "dimensionId": "2",
      "method": "CustomDimensions.getCustomDimension",
      "aggregation": "SUM",
      "aggregationVariable": "nb_visits",
      "resolutions": [
        "HOURLY",
        "DAILY",
        "WEEKLY",
        "MONTHLY",
        "YEARLY"
      ]
    }],
    "type": "timeseries"
}

```

### Configuration Example to Access Media Analytics Statistics

The following example generates charts for events with information about the average time the video was watched. This
information is only available if the Matomo Media Analytics plugin is in use.

`etc/statistics/matomo.episode.time-watched.sum.json`
```json
{
    "id": "episode.time-watched.sum.matomo",
    "title": "Time Watched [sec]",
    "description": "Average Time Watched [sec]",
    "resourceType": "EPISODE",
    "sources": [{
      "siteId": "1",
      "method": "MediaAnalytics.getVideoTitles",
      "aggregation": "SUM",
      "aggregationVariable": "avg_time_watched",
      "resolutions": [
        "HOURLY",
        "DAILY",
        "WEEKLY",
        "MONTHLY",
        "YEARLY"
      ]
    }],
    "type": "timeseries"
}

```

### Configuration Example to Access Overall Statistics

The following example generates charts at the organization level with information about the number of visits for all events.

`etc/statistics/matomo.organization.visits.sum.json`
```json
{
    "id": "organization.plays.sum.matomo",
    "title": "STATISTICS.TITLE.VIEWS_SUM",
    "description": "STATISTICS.DESCRIPTION.VIEWS_SUM",
    "resourceType": "ORGANIZATION",
    "sources": [{
      "siteId": "1",
      "method": "API.get",
      "aggregation": "SUM",
      "aggregationVariable": "nb_visits",
      "resolutions": [
        "HOURLY",
        "DAILY",
        "WEEKLY",
        "MONTHLY",
        "YEARLY"
      ]
    }],
    "type": "timeseries"
}
```

## Limitations

- As Opencast currently only supports timeseries statistics, other non-timeseries statistics available through Matomo,
  such as visits per hour, client configuration etc., can't be displayed.
- No media analytcis statistics are available at series level. There is currently no aggregation of event media analytics
  by series.