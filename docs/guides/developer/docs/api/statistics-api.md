[TOC]

# General

The Statistics API is available since API version 1.3.0.

### GET /api/statistics/providers

Returns a list of statistics providers.

The following query string parameters are supported to filter the returned list:

Query String Parameter |Type                         | Description
:----------------------|:----------------------------|:-----------
`filter`               | [`string`](types.md#basic)  | A comma-separated list of filters to limit the results with (see [Filtering](usage.md#filtering)). See the below table for the list of available filters

Filter Name       | Description
:-----------------|:-----------
`resourceType`    | Filter statistics provider by resource type (either `episode`, `series` or `organization`)

This request additionally supports the following query string parameters to include additional information directly in
the response:

Query String Parameter | Type                        | Description
:----------------------|:----------------------------|:-----------
`withparameters`       | [`boolean`](types.md#basic) | Whether support parameters should be included in the response

__Sample request__
```xml
https://opencast.domain.com/api/statistics/providers?filter=resourceType:episode
```

__Response__

`200 (OK)`: A (potentially empty) list of providers is returned as a JSON array containing JSON objects describing the series:

Field          | Type                                      | Description
:--------------|:------------------------------------------|:-----------
`identifier`   | [`string`](types.md#basic)                | The unique identifier of the provider
`title`        | [`string`](types.md#basic)                | The title of the provider
`description`  | [`string`](types.md#basic)                | The description of the provider
`type`\*       | [`string`](types.md#basic)                | The type of the provider
`resourceType` | [`string`](types.md#basic)                | The resource type of the provider
`parameters`   | [`array[parameter]`](types.md#statistics) | Supported query parameters (optional)

\* Currently, only the `timeseries` type is supported

__Example__

```
[
  {
    "identifier": "influxdb-episode-views-provider",
    "title": "Episode Views",
    "description": "Episode Views, Lorem Ipsum",
    "type": "timeSeries",
    "resourceType": "episode"
  }
]
```

### GET /api/statistics/providers/{providerId}

Returns a statistics provider.

This request additionally supports the following query string parameters to include additional information directly in
the response:

Query String Parameter | Type                        | Description
:----------------------|:----------------------------|:-----------
`withparameters`       | [`boolean`](types.md#basic) | Whether support parameters should be included in the response

__Sample request__
```xml
https://opencast.domain.com/api/statistics/providers/a-timeseries-provider?withparameters=true
```

__Response__

`200 (OK)`: A (potentially empty) list of providers is returned as a JSON array containing JSON objects describing the series:

Field          | Type                                      | Description
:--------------|:------------------------------------------|:-----------
`identifier`   | [`string`](types.md#basic)                | The unique identifier of the provider
`title`        | [`string`](types.md#basic)                | The title of the provider
`description`  | [`string`](types.md#basic)                | The description of the provider
`type`\*       | [`string`](types.md#basic)                | The type of the provider
`resourceType` | [`string`](types.md#basic)                | The resource type of the provider
`parameters`   | [`array[parameter]`](types.md#statistics) | Supported query parameters (optional)

\* Currently, only the `timeSeries` type is supported

__Example__

```
{
  "identifier": "a-timeseries-provider",
  "title": "Episode Views",
  "description": "Episode Views, Lorem Ipsum",
  "type": "timeseries",
  "resourceType": "episode",
  "parameters": [
    {
      "name": "resourceId",
      "optional": false,
      "type": "string"
    },
    {
      "name": "from",
      "optional": false,
      "type": "datetime"
    },
    {
      "name": "to",
      "optional": false,
      "type": "datetime"
    },
    {
      "values": [
        "daily",
        "weekly",
        "monthly",
        "yearly"
      ],
      "name": "dataResolution",
      "optional": false,
      "type": "enumeration"
    }
  ]
}
```


### POST /api/statistics/data/query

Retrieves statistical data from one or more providers

Form Parameters | Required |Type                                  | Description
:---------------|:---------|:-------------------------------------|:-----------
`data`          | yes      | [`array[object]`](types.md#extended) | A JSON array describing the statistics queries to request (see below)

The JSON array consists of query JSON objects. A query JSON object contains information about a statistics query to be executed:

Field        | Required | Type                     | Description
:------------|:---------|:-------------------------|:-----------
`provider`   | yes      | [`property`](#extended)  | A JSON object with information about the statistics provider to be queried
`parameters` | yes      | [`property`](#extended)  | A JSON object containing the parameters

The JSON object `provider` has the following fields:

Field        | Required | Type                | Description
:------------|:---------|:--------------------|:-----------
`identifier` | yes      | [`string`](#basic)  | A JSON object with information about the statistics provider to be queried

The format of the JSON object `parameters` depends on the provider type that is queried, and is described separately for
each provider in the next section.

__Example__

    [
      {
        "provider": {
          "identifier": "a-statistics-provider"
        },
        "parameters": {
          "resourceId": "93213324-5d29-428d-bbfd-369a2bae6700"
        }
      },
      {
        "provider": {
          "identifier": "a-timeseries-provider"
        },
        "parameters": {
          "resourceId": "23413432-5a15-328e-aafe-562a2bae6800",
          "from": "2019-04-10T13:45:32Z",
          "to": "2019-04-12T00:00:00Z",
          "dataResolution": "daily"
        }
      }
    ]


__Response__

`200 (OK)`: A (potentially empty) list of query results is returned as a JSON array containing JSON objects
`400 (BAD REQUEST)`: The request was not valid

Field name       |Type                                  | Description
:----------------|:-------------------------------------|:-----------
`provider`       | [`property`](#extended)              | A JSON object describing the statistics provider as described below
`parameters`     | [`property`](#extended)              | A JSON object describing the query parameters
`data`           | [`property`](#extended)              | A JSON object containing the query result


Here, a statistics provider JSON object has the following fields:

Field          | Type                                      | Description
:--------------|:------------------------------------------|:-----------
`identifier`   | [`string`](types.md#basic)                | The unique identifier of the provider
`type`\*       | [`string`](types.md#basic)                | The type of the provider
`resourceType` | [`string`](types.md#basic)                | The resource type of the provider

Note that the format of data is implied by the type of the statistics provider.

__Example__

    [
      {
        "provider": {
          "identifier": "a-statistics-provider",
          "type": "someType",
          "resourceType": "episode",
        },
        "parameters": {
          "resourceId": "93213324-5d29-428d-bbfd-369a2bae6700"
        },
        "data": {
          "value": "1"
        },
      {
        "provider": {
          "identifier": "a-timeseries-provider",
          "type": "timeseries",
          "resourceType": "episode",
        },
        "parameters": {
          "resourceId": "23413432-5a15-328e-aafe-562a2bae6800",
          "from": "2019-04-10T13:45:32Z",
          "to": "2019-04-12T00:00:00Z",
          "dataResolution": "daily"
        },
        "data": {
          "labels": ["2019-04-10T13:45:32Z", "2019-04-11T00:00:00Z", "2019-04-12T00:00:00Z"],
          "values": [20, 100, 300],
          "total": 420
        }
    ]

#### Time Series Statistics Provider

Time Series Statistics Providers (type = timeseries) support some well-defined parameters and deliver a well-defined
data format.

Parameters:

Field name       |Type                                  | Description
:----------------|:-------------------------------------|:-----------
`resourceId`     | [`string`](#basic)                   | The technical identifier of the resource the data relates to
`from`           | [`datetime`](types.md#date-and-time) | Start of time period this query relates to
`to`             | [`datetime`](types.md#date-and-time) | End of time period this query relates to
`dataResolution` | [`string`](#basic)                   | `daily`, `monthly` or `yearly` (as described by provider)

Query Result Data Field:

Field name       |Type                                  | Description
:----------------|:-------------------------------------|:-----------
`labels`         | [`array[datetime]`](#extended)       | The dates of the measurement points
`values`         | [`array[integer]`](#extended)        | The values of the measurement points
`total`          | [`integer`](#basic)                  | The sum of all values 

