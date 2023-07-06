[TOC]

# Information

The ListProvider API is available since API version 1.10.0.

### GET /api/listproviders/languages

Returns a list of languages as configured in the listproviders.

__Response__

`200 (OK)`: The languages are returned as key value pairs in a JSON object.

Field     | Type                       | Description
:---------|:---------------------------|:-----------
`key `    | [`string`](types.md#basic) | Three letter ISO language codes
`value`   | [`string`](types.md#basic) | The translation string used in the Opencast Admin UI


__Example__

```
{
  "dan": "LANGUAGES.DANISH",
  "nor": "LANGUAGES.NORWEGIAN",
  "tur": "LANGUAGES.TURKISH",
}
```

### GET /api/listproviders/licenses

Returns a list of licenses as configured in the listproviders.

__Response__

`200 (OK)`: The licenses are returned as key value pairs in a JSON object.

Field     | Type                       | Description
:---------|:---------------------------|:-----------
`key `    | [`string`](types.md#basic) | Three license codes
`value`   | [`object`](types.md#basic) | An object containing information for display in the Opencast Admin UI


__Example__

```
{
  "CC-BY-NC-SA": "{\"label\":\"EVENTS.LICENSE.CCBYNCSA\", \"order\":6, \"selectable\": true}",
  "CC-BY": "{\"label\":\"EVENTS.LICENSE.CCBY\", \"order\":2, \"selectable\": true}",
}
```
