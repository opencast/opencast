[TOC]

# Information

The Playlists API is available since API version 1.11.0.

### GET /api/playlists/playlist.json

Returns a playlist.

__Response__

`200 (OK)`: A playlist as JSON.  
`400 (BAD REQUEST)`: The request is invalid or inconsistent.  
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.  
`404 (NOT FOUND)`: The specified playlist does not exist.

| Field              | Type                          | Description                                               |
|--------------------|-------------------------------|-----------------------------------------------------------|
| `id`               | [`String`](types.md#basic)    | The playlist identifier                                   |


__Example__

```json
{
  "playlist": {
    "id": 551,
    "organization": "mh_default_org",
    "entries": [
      {
        "id": 553,
        "contentId": "ID-about-opencast",
        "type": "EVENT"
      },
      {
        "id": 554,
        "contentId": "ID-3d-print",
        "type": "EVENT"
      }
    ],
    "title": "Opencast Playlist",
    "description": "This is a playlist about Opencast",
    "creator": "Opencast",
    "updated": 1701854481056,
    "accessControlEntries": {
      "allow": true,
      "role": "ROLE_USER_BOB",
      "action": "read"
    }
  }
}
```

### GET /api/playlists/playlists.json

Get playlists. Playlists that you do not have read access to will not show up.

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
{
  "playlist": [
    {
      "playlist": {
        "id": 551,
        "organization": "mh_default_org",
        "entries": [
          {
            "id": 553,
            "contentId": "ID-about-opencast",
            "type": "EVENT"
          },
          {
            "id": 554,
            "contentId": "ID-3d-print",
            "type": "EVENT"
          }
        ],
        "title": "Opencast Playlist",
        "description": "This is a playlist about Opencast",
        "creator": "Opencast",
        "updated": 1701854481056,
        "accessControlEntries": {
          "allow": true,
          "role": "ROLE_USER_BOB",
          "action": "read"
        }
      }
    },
    {
      "playlist": {
        "id": 1051,
        "organization": "mh_default_org",
        "entries": [
          {
            "id": 1053,
            "contentId": "ID-about-opencast",
            "type": "EVENT"
          },
          {
            "id": 1054,
            "contentId": "ID-3d-print",
            "type": "EVENT"
          }
        ],
        "title": "Opencast Playlist",
        "description": "This is a playlist about Opencast",
        "creator": "Opencast",
        "updated": 1701856455007,
        "accessControlEntries": {
          "allow": true,
          "role": "ROLE_USER_BOB",
          "action": "read"
        }
      }
    }
  ]
}
```

### PUT /api/playlists/update.json

Updates a playlist or creates a new one.

__Response__

`200 (OK)`: The updated playlist.
`400 (BAD REQUEST)`: The request is invalid or inconsistent.  
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.  

| Field       | Type                            | Description             |
|-------------|---------------------------------|-------------------------|
| `playlist`  | [`Playlist`](types.md#Playlist) | Playlist in JSON format |


__Example__

```json
{
  "playlist": {
    "id": 551,
    "organization": "mh_default_org",
    "entries": [
      {
        "id": 553,
        "contentId": "ID-about-opencast",
        "type": "EVENT"
      },
      {
        "id": 554,
        "contentId": "ID-3d-print",
        "type": "EVENT"
      }
    ],
    "title": "Opencast Playlist",
    "description": "This is a playlist about Opencast",
    "creator": "Opencast",
    "updated": 1701854481056,
    "accessControlEntries": {
      "allow": true,
      "role": "ROLE_USER_BOB",
      "action": "read"
    }
  }
}
```

### DELETE /api/playlists/remove

Removes a playlist.

__Response__

`200 (OK)`: The removed playlist.
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.  
`404 (NOT FOUND)`: The specified playlist does not exist.

| Field | Type                         | Description             |
|-------|------------------------------|-------------------------|
| `id`  | [`String`](types.md#basic)   | The playlist identifier |


### POST /api/playlists/updateEntries.json

Updates the entries of a playlist.

__Response__

`200 (OK)`: The updated playlist.
`400 (BAD REQUEST)`: The request is invalid or inconsistent.  
`403 (FORBIDDEN)`: The user doesn't have the rights to make this request.  
`404 (NOT FOUND)`: The specified playlist does not exist.

| Field             | Type                                            | Description             |
|-------------------|-------------------------------------------------|-------------------------|
| `id`              | [`String`](types.md#basic)                      | The playlist identifier |
| `playlistEntries` | [`List<PlaylistEntry>`](types.md#PlaylistEntry) | Playlist in JSON format |


__Example__

```json
[
    {
        "contentId": "ID-about-opencast",
        "type": "EVENT"
    },
    {
        "contentId": "ID-3d-print",
        "type": "EVENT"
    }
]
```
