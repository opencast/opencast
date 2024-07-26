[TOC]

# Information

The LifeCycle Management API is available since API version 1.12.0.

### GET /api/lifecyclemanagement/policy/{id}

Returns a lifecycle policy.

__Response__

`200 (OK)`: A policy as JSON.  
`400 (BAD REQUEST)`: The request is invalid or inconsistent.  
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.  
`404 (NOT FOUND)`: The specified policy does not exist.

__Example__

```json
{
  "actionParameters": "{ workflowId: noop }",
  "timing": "SPECIFIC_DATE",
  "action": "START_WORKFLOW",
  "targetType": "EVENT",
  "id": "601",
  "title": "My first policy",
  "isActive": false,
  "actionDate": "2023-11-30T16:16:47Z",
  "targetFilters": "",
  "accessControlEntries": [
    {
      "allow": true,
      "role": "ROLE_USER_BOB",
      "action": "read",
      "id": 602
    }
  ]
}
```

### GET /api/lifecyclemanagement/policies

Get lifecycle policies. Policies that you do not have read access to will not show up.

__Response__

`200 (OK)`: A JSON object containing an array.
`400 (BAD REQUEST)`: The request is invalid or inconsistent.

| Field                    | Type                       | Description                                                                                                                                                                                                                                                                                     |
|--------------------------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `limit`                  | [`int`](types.md#basic)    | The maximum number of results to return for a single request                                                                                                                                                                                                                                    |
| `offset`                 | [`int`](types.md#basic)    | The index of the first result to return                                                                                                                                                                                                                                                         |
| `sort`                   | [`string`](types.md#basic) | Sort the results based upon a sorting criteria. A criteria is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory. Sort Name is case sensitive. Supported Sort Names are 'updated' |

__Example__

```json
[
    {
      "actionParameters": "{ workflowId: noop }",
      "timing": "SPECIFIC_DATE",
      "action": "START_WORKFLOW",
      "targetType": "EVENT",
      "id": "601",
      "title": "My first policy",
      "isActive": false,
      "actionDate": "2023-11-30T16:16:47Z",
      "targetFilters": "",
      "accessControlEntries": [
        {
          "allow": true,
          "role": "ROLE_USER_BOB",
          "action": "read",
          "id": 602
        }
      ]
    }
]
```

### POST /api/lifecyclemanagement/policy

Creates a new policy.

__Response__

`201 (CREATED)`: The policy was successfully created.
`400 (BAD REQUEST)`: The request is invalid or inconsistent.  
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.

| Field                  | Type                                                     | Description                                                                                                            |
|------------------------|----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `title`                | [`string`](types.md#basic)                               | Policy Title                                                                                                           |
| `targetType`           | [`enumeration`]                                          | Which entities the policy should affect. Possible values: EVENT                                                        |
| `action`               | [`enumeration`]                                          | The action that should be performed. Possible values: START_WORKFLOW                                                   |
| `actionParameters`     | [`string`](types.md#LifeCycle Policy Action Parameters)  | JSON. Depends on the type of action                                                                                    |
| `actionDate`           | [`string`](types.md#basic)                               | Required if timing is SPECIFIC_DATE. Should be an ISO string                                                           |
| `cronTrigger`          | [`string`](types.md#basic)                               | Required if timing is REPEATING. https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.htm |
| `timing`               | [`enumeration`]                                          | When the policy should be applied. Possible values: SPECIFIC_DATE, REPEATING, ALWAYS                                   |
| `filters`              | [`string`](types.md#basic)                               | The filter(s) used to select applicable entities. JSON. Depends on the type of action                                  |
| `accessControlEntries` | [`string`](types.md#acl)                                 | JSON for ACL.                                                                                                          |

### PUT /api/lifecyclemanagement/policy/{id}

Updates a policy.

__Response__

`200 (OK)`: The policy was successfully updated.
`400 (BAD REQUEST)`: The request is invalid or inconsistent.  
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.

| Field                  | Type                                                     | Description                                                                                                            |
|------------------------|----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `title`                | [`string`](types.md#basic)                               | Policy Title                                                                                                           |
| `targetType`           | [`enumeration`]                                          | Which entities the policy should affect. Possible values: EVENT                                                        |
| `action`               | [`enumeration`]                                          | The action that should be performed. Possible values: START_WORKFLOW                                                   |
| `actionParameters`     | [`string`](types.md#LifeCycle Policy Action Parameters)  | JSON. Depends on the type of action                                                                                    |
| `actionDate`           | [`string`](types.md#basic)                               | Required if timing is SPECIFIC_DATE. Should be an ISO string                                                           |
| `cronTrigger`          | [`string`](types.md#basic)                               | Required if timing is REPEATING. https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.htm |
| `timing`               | [`enumeration`]                                          | When the policy should be applied. Possible values: SPECIFIC_DATE, REPEATING, ALWAYS                                   |
| `filters`              | [`string`](types.md#basic)                               | The filter(s) used to select applicable entities. Format: 'filter1:value1,filter2:value2'                              |
| `accessControlEntries` | [`string`](types.md#acl)                                 | JSON for ACL.                                                                                                          |

### DELETE /api/lifecyclemanagement/policy/{id}

Removes a policy.

__Response__

`200 (OK)`: The policy was successfully deleted.
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.  
`404 (NOT FOUND)`: The specified policy does not exist.
