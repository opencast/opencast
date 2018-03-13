[TOC]
# General

Available since API version 1.1.0.

### GET /api/agents

Returns a list of capture agents.

Query String Parameter     |Type            | Description
:--------------------------|:---------------|:----------------------------
`limit`                    | `integer`      | The maximum number of results to return for a single request.
`offset`                   | `integer`      | Number of results to skip based on the limit. 0 is the first set of results up to the limit, 1 is the second set of results after the first limit, 2 is third set of results after skipping the first two sets of results etc.


__Sample request__

```xml
https://opencast.example.org/api/agents?limit=5&offset=1
```

__Response__

`200 (OK)`: A (potentially empty) list of capture agents is returned.

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

<!--- ##################################################################### -->
### GET /api/agents/{agent_id}

Returns a single capture agent.

__Response__

`200 (OK)`: The capture agent is returned.<br/>
`404 (NOT FOUND)`: The specified capture agent does not exist.

```
{
  "agent_id": "ca24",
  "status": "offline",
  "inputs": ["default"],
  "update": "2018-03-12T18:17:25Z",
  "url": "127.0.0.1"
}
```
