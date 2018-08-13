[1]: http://en.wikipedia.org/wiki/Basic_access_authentication
[2]: http://en.wikipedia.org/wiki/XML
[3]: http://en.wikipedia.org/wiki/JSON
[4]: http://en.wikipedia.org/wiki/ISO_8601

# Version

Since the External API is versioned, it supports specification of a version identifier as part of the standard `Accept`
HTTP request header:


Header   | Type                       | Description
:--------|:---------------------------|:-----------
`Accept` | [`string`](types.md#basic) | `application/<version>+<format>` is used to specify the API version and format

Notes:

- The External API currently only supports the format [JSON][3]

__Example__

```
Accept: application/v1.0.0+json
```

If that header is not specified, or no version information can be extracted from the header, the assumption is that the
request should be executed against the most recent version. If the version specified is not available, `406 (NOT
ACCEPTABLE)` is returned as the HTTP response code.

With every response, the API version is specified as part of the standard HTTP `Content-Type` header, as in
`application/v1.0.0+json`.

Versions should be specified as obtained from the [Base API](base-api.md#versions) call to `/versions`.


## Authentication

The External API is using basic authentication. In order to make calls to the API, the following standard request
headers need to be sent with every request:

Header          | Type                       | Description
:---------------|:---------------------------|:-----------
`Authorization` | [`string`](types.md#basic) | Sends username and password as defined in [Basic Authentication][1]


# Authorization

There are multiple ways to authorize a request - see the [authorization section](authorization.md) for more details. In
short, the External API either supports specifying the execution user, the execution user’s roles or a combination of
the two in which case the execution roles will be added to the execution user’s existing roles.

If no user is specified, Opencast’s `anonymous` user is used to execute the request, potentially enriched by the roles
provided using the `X-ROLES` request.

Header            | Type                       | Description
:-----------------|:---------------------------|:-----------
`X-API-AS-USER`   | [`string`](types.md#basic) | Id of the user in whose name the request should be executed
`X-API-WITH-ROLES`| [`string`](types.md#basic) | Set of roles, separated by whitespace, that the execution user should be assigned in addition to existing roles.


# Formats and Encoding

## Content Type

The External API currently supports [JSON][3] format only.

Header   | Type                       | Description
:--------|:---------------------------|:-----------
`Accept` | [`string`](types.md#basic) | The expected response format is `application/json`

If that header is not specified, the `Content-Type` will be `application/<version>+json`.

> Note that the same header should be used to specify the version of the API that is expected to return the response. In
> this case, the header looks like this: `application/v1+json`. See the [versioning chapter of the general
> section](index.md#versioning) for more details.

## Encoding of single objects

### JSON notation

Single objects are enclosed in curly braces "{}" and are not explicitly named.

__Example__

```
{
  "firstname": "John",
  "lastname": "Doe"
}
```

## Encoding of collections of objects

### JSON notation

Collections of objects are enclosed in braces "[ ... ]" and are not explicitly named.

__Example__

```
[
  {
    "firstname": "Jane",
    "lastname": "Doe"
  },
  {
    "firstname": "John",
    "lastname": "Doe"
  }
]
```

## Encoding of empty fields

Instead of dropping fields that do not contain a value for a specific data object from the JSON response structure, the
respective identity element should be used:

Type     | Encoding | Description
:--------|:---------|:-----------
Literals | ""       | Strings and numbers
Objects  | {}       | Non-existing objects
Arrays   | []       | Non-existing list of literals or objects

# Sorting

Sorting of result sets is supported by a set of well-defined fields per request, one at a time. Each API request
explicitly defines the fields that support sorting.

Multiple sort criteria can be specified as a comma-separated list of pairs such as: `Sort Name`:`ASC` or
`Sort Name`:`DESC`. Adding the suffix `ASC` or `DESC` sets the order as ascending or descending order and is mandatory.

Parameter | Description
:---------|:-----------
`sort`    | Comma-separated list of sort critera

__Example__

Ordering the list of events by title:

```xml
GET /api/events?sort=title:ASC,start_date:DESC
```

# Filtering

Filtering of result sets is supported by a set of well-defined fields per request. Multiple filter criteria can be
defined by specifying a comma-separated list of filters. In this case, the criteria are applied using logical
`and`.

A filter is the filter's name followed by a colon ":" and then the value to filter with so it is the form
`Filter Name`:`Value to Filter With`.

Each API request explicitly defines the fields that support filtering.

Parameter | Description
:---------|:-----------
`filter`  | A comma seperated list of filters to limit the results with

Note that filter conditions must be URL encoded.

__Example__

Filter the list of events by status and by series.

```xml
GET /api/events?filter=status%3dpublished,series%3dmath
```

# Pagination

When loading large result sets, being able to address and access the data in well-defined chunks using a limit and
offset is essential. Paging is enabled for all requests that return lists of items.

Paramter | Description
:--------|:-----------
`limit`  | The maximum number of results to return for a single request
`offset` | The index of the first record to return (counting starts on zero)

__Example__

From the list of events, return items 50-74.

```xml
GET /api/events?limit=25&offset=50
```
