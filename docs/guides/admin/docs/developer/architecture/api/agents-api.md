[TOC]
# General

The Agents API is available since API version 1.1.0.

### GET /api/agents

Returns a list of capture agents.

Query String Parameter | Type                        | Description
:----------------------|:----------------------------|:-----------
`limit`                | [`integer`](types.md#basic) | The maximum number of results to return for a single request (see [Pagination](usage.md#pagination))
`offset`               | [`integer`](types.md#basic) | The index of the first result to return (see [Pagination](usage.md#pagination))


__Sample request__

```xml
https://opencast.example.org/api/agents?limit=5&offset=1
```

__Response__

`200 (OK)`: A (potentially empty) list of capture agents is returned as JSON array of JSON objects

Field      | Type                                 | Description
:----------|:-------------------------------------|:-----------
`agent_id` | [`string`](types.md#basic)           | The technical identifier of the capture agent
`status`   | [`string`](types.md#basic)           | The status of the capture agent
`inputs`   | [`array[string]`](types.md#array)    | The inputs of the capture agent
`update`   | [`datetime`](types.md#date-and-time) | The last date and time this capture agent contactec the server
`url`      | [`string`](types.md#basic)           | The URL as reported by the capture agent


__Example__

```
[
  {
    "agent_id": "ca24",
    "status": "offline",
    "inputs": ["default"],
    "update": "2018-03-12T18:17:25Z",
    "url": "127.0.0.1"
  }
]
```

### GET /api/agents/{agent_id}

Returns a single capture agent.

__Response__

`200 (OK)`: The capture agent is returned as JSON object

Field      | Type                                 | Description
:----------|:-------------------------------------|:-----------
`agent_id` | [`string`](types.md#basic)           | The technical identifier of the capture agent
`status`   | [`string`](types.md#basic)           | The status of the capture agent
`inputs`   | [`array[string]`](types.md#array)    | The inputs of the capture agent
`update`   | [`datetime`](types.md#date-and-time) | The last date and time this capture agent contactec the server
`url`      | [`string`](types.md#string)          | The URL as reported by the capture agent


`404 (NOT FOUND)`: The specified capture agent does not exist.

__Example__

```
{
  "agent_id": "ca24",
  "status": "offline",
  "inputs": ["default"],
  "update": "2018-03-12T18:17:25Z",
  "url": "127.0.0.1"
}
```
