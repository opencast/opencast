Basic Statistics
==========

Opencast ships with its own mechanism for gathering and reporting basic statistics, e.g. video view counts.
To that end external applications can send *raw events* to Opencast.

- Raw events: low level user actions, with timestamp, pseudonomized for current day, fully anonymized for past days.

Opencast offers two endpoints for adding events, `/basicstatistics/clientPush` and `/basicstatistics/trustedPush`.

`POST /basicstatistics/clientPush`
----------------------------

Can be used without authentication

The request must contain a body containing a JSON object with the following fields:
- `events`: array of objects with the following fields, which correspond directly to the fields of a raw event with the same name:
    - `timestamp`: string containing an RFC3339 formatted date-time, with millisecond precision (i.e. 3 digits after `.`). Example: `"2026-04-27T14:56:38.415Z"`
    - `itemType`: string
    - `itemId`: string
    - `eventType`: string
    - `eventPayload`: optional, `null` or object


**Example**

Request (JS):

```js
await fetch("https://opencast.tld/basicstatistics/clientPush", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
        // Note: the browser will automatically add `User-Agent` header
    },
    body: JSON.stringify({
        events: [
            {
                timestamp: "2026-04-27T14:56:38.415Z",
                itemType: "video",
                itemId: "2ea94d36-e5aa-4069-af43-75515772d2c2",
                eventType: "VIDEO_PLAY",
                // eventPayload might be omitted or set to `null` here
            },
            {
                timestamp: "2026-04-27T14:56:41.123Z",
                itemType: "video",
                itemId: "2ea94d36-e5aa-4069-af43-75515772d2c2",
                eventType: "banana", // unknown type
            },
            {
                timestamp: "2026-04-27T14:56:57.987Z",
                itemType: "video",
                itemId: "2ea94d36-e5aa-4069-af43-75515772d2c2",
                eventType: "VIDEO_SEEK",
                eventPayload: {
                    to: 42856,
                    flux: "compensated", // unknown field
                },
            },
        ],
    })
});
```

This would result in two events being saved, all with the same session hash:
- `VIDEO_PLAY` with `null` payload
- `VIDEO_SEEK` with `{ "to": 42856 }` as payload (the unknown field is ignored)

Response:

```json
{
    "accepted": 2,
    "rejected": [
        {
            "index": 1,
            "error": "unknown event_type 'banana'"
        }
    ]
}
```

`POST /basicstatistics/trustedPush`
----------------------------
This is intended for servers/nodes that Opencast trust, like octoka (which delivers files). 
Authentication is required.

This endpoint is very similar to the "Client Push" endpoint.
Apart from authentication and API path, there are the following differences:
- Each object in the request array must contain two additional fields:
    - `addr`: IP address (as string)
    - `ua`: user agent string
- During input verification:
    - All `event_types` are allowed (including `FETCH_FILE`)
    - The `timestamp` may be arbitrarily far in the past (i.e. `MAX_CLIENT_PUSH_DELAY` is not used)

**Example**

Request (JS just for demonstration purposes, likely not sent by JS):

```js
await fetch("https://opencast.tld/basicstatistics/trustedPush", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
    },
    body: JSON.stringify({
        events: [
            {
                timestamp: "2026-04-27T15:29:31.456Z",
                addr: "105.59.238.2",
                ua: "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:149.0) Gecko/20100101 Firefox/149.0",
                itemType: "video",
                itemId: "307c7327-d7e4-47b5-b01d-6779e2422f9f",
                eventType: "file-fetch",
                eventPayload: {
                    elem: "1f6cca9c-0d4c-4bad-ab82-53b6b0514508",
                    from: 0, 
                    to: 262144,
                },
            },
        ],
    })
});
```

### Event type and data

The following events and their respective event data (payloads) are defined.
If no payload is specified, the event data field must be null.
Otherwise, the field must be a JSON object with the fields specified below.

- `VIDEO_PLAY`: user has clicked "play" on a video to start watching. No payload. Note that this represents only the
  first click on the play button. Further clicks are represented by resume. 
- `VIDEO_PAUSE`: user has paused video playback. Payload:
    - `at` (`video_timestamp`): when the user paused
- `VIDEO_RESUME`: user has resumed video playback. Payload:
    - `at` (`video_timestamp`): where the user resumed playback
- `VIDEO_SEEK`: user jumped to somewhere in the video. Payload:
    - `to` (`video_timestamp`): time in the video that was jumped to
- `VIDEO_WATCHED`: the user has fully watched part of the video.
  The event timestamp is the "end" time when the part has already been watched.
  Payload:
    - `from` (`video_timestamp`)
    - `to` (`video_timestamp`)
- `FETCH_FILE`: a file was (partially) downloaded.
  The event timestamp is the time when the first request was first received.
  Payload:
    - `elem` (string): (file) element ID, which is the path segment after the video ID.
    - `from` (uint): start of byte range of what was downloaded.
      Non-range-request specify 0.
    - `to` (nullable, uint): end of byte range of what was downloaded.
      `null` if the request does not specify an end and the file server has no way to check what bytes were actually sent to the client.

#### Notes for frontends
Sending video events to the backend should be buffered.
[`sendBeacon`](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/sendBeacon) should be used to send all remaining buffered events when the user closes the page.
Note however, that `sendBeacon` is not fully reliable, so the buffer duration should be kept reasonably short, to avoid losing too many reports.
Events must not be buffered for longer than `MAX_CLIENT_PUSH_DELAY`.

None of the statistic requests should influence the main functionality, i.e. even if requests are super slow, or fail, main functionality must remain.

Frontends should debounce video events to clean user behavior a bit.
For example, for multiple seek operations in a short amount of time, reporting only the last one is likely a good call (imagine a user trying to jump far forward by pressing the +10s button many times).
The same is true for pause/resume actions.

To report the `VIDEO_WATCHED` action, frontends have to have their own small logic, and they should try to always report the largest possible range, or in other words: merge adjacent ranges.
For example, instead of reporting one `VIDEO_WATCHED` event for every second watched, only one event for each consecutive section watched should be reported.
Of course, we still want to report progress somewhat regularly (to avoid losing these events on page close), so `VIDEO_WATCHED` events of neighboring ranges will still end up in the DB.

#### Notes for file servers
Requests for consecutive byte ranges of the same file should be merged already, to reduce the number of events stored.
Ideally, file servers should report byte ranges of data actually sent to the client instead of the request `Range` parameters.
For example, clients just sending requests and immediately closing the connection wouldn't count, or at least only for one packet worth of bytes.
