[TOC]

# Information

The ListProvider API is available since API version 1.10.0.

### GET /api/listproviders/providers.json

Returns a list of listproviders.

__Response__

`200 (OK)`: The listproviders are returned as a list.

Field     | Type                       | Description
:---------|:---------------------------|:-----------
`value`   | [`string`](types.md#basic) | The string used to identify a provider, e.g. "LANGUAGES"


__Example__

```
{
  "LICENSES",
  "LANGUAGES",
  "SERIES",
}
```

### GET /api/listproviders/{source}.json

Provides key-value list from the given listprovider.

__Response__

`200 (OK)`: The key-value list are returned as a JSON object.

Field     | Type                       | Description
:---------|:---------------------------|:-----------
`key `    | [`string`](types.md#basic) | Source key
`value`   | [`object`](types.md#basic) | Source value


__Example for "LICENSES"__

```
{
  "CC-BY-NC-SA": "{\"label\":\"EVENTS.LICENSE.CCBYNCSA\", \"order\":6, \"selectable\": true}",
  "CC-BY": "{\"label\":\"EVENTS.LICENSE.CCBY\", \"order\":2, \"selectable\": true}",
}
```
