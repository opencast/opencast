[1]: http://en.wikipedia.org/wiki/Basic_access_authentication
[2]: http://en.wikipedia.org/wiki/XML
[3]: http://en.wikipedia.org/wiki/JSON

# Version

Since the API is versioned, it supports specification of a version identifier as part of the standard `Accept` HTTP request header:


Header   | Type                       | Description
:--------|:---------------------------|:-----------
`Accept` | [`string`](types.md#basic) | The format is specified to use `application/<version>`, or `application/<version>+<format>` to also specify the required format.

__Example__

```
Accept: application/v1.0.0+json
```

If that header is not specified, or no version information can be extracted from the header, the assumption is that the request should be executed against the most recent version. If the version specified is not available, `400 (BAD REQUEST)` is returned as the HTTP response code.

With every response, the api version is specified as part of the standard HTTP `Content-Type` header, as in `application/v1.0.0+xml`.

Versions should be specified as obtained from the [Base API](base-api.md#versions) call to `/versions`.


## Authentication

The API is using basic authentication. In order to make calls to the API, the following standard request headers need to be sent with every request:

Header          | Type                       | Description
:---------------|:---------------------------|:-----------
`Authorization` | [`string`](types.md#basic) | Sends username and password as defined in [Basic Authentication][1]


# Authorization

There are multiple ways to authorize a request - see the [authorization section](authorization.md) for more details. In short, the Application API either supports specifying the execution user, the execution user’s roles or a combination of the two in which case the execution roles will be added to the execution user’s existing roles.

If no user is specified, Opencast’s `anonymous` user is used to execute the request, potentially enriched by the roles provided using the `X-ROLES` request.

Header            | Type                       | Description
:-----------------|:---------------------------|:-----------
`X-API-AS-USER`   | [`string`](types.md#basic) | Id of the user in whose name the request should be executed
`X-API-WITH-ROLES`| [`string`](types.md#basic) | Set of roles, separated by whitespace, that the execution user should be assigned in addition to existing roles.


# Formats and Encoding

## Content Type

The Application API currently supports [JSON][3] format only.

Header   | Type                       | Description
:--------|:---------------------------|:-----------
`Accept` | [`string`](types.md#basic) | The expected response format is `application/json`

If that header is not specified, the `Content-Type` will be `application/<version>+json`.

> Note that the same header should be used to specify the version of the api that is expected to return the response. In this case, the header looks like this: `application/v1+json`. See the [versioning chapter of the general section](index.md#versioning) for more details.

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

Instead of dropping fields that do not contain a value for a specific data object from the JSON response structure, the respective identity element should be used:

Type     | Encoding | Description
:--------|:---------|:-----------
Literals | ""       | Strings and numbers
Objects  | {}       | Non-existing objects
Arrays   | []       | Non-existing list of literals or objects

# Sorting

Sorting of result sets is supported by a set of well-defined fields per request, one at a time. Each api request explicitly defines the fields that support sorting.

## Sort field

Parameter | Description
:---------|:-----------
`sort`    | Takes the name of the field that defines the sort criteria.

__Example__

Ordering the list of events by title:

```xml
GET /api/events?sort=title
```

## Sort order

Parameter | Encoding      | Description
:---------|:--------------|:-----------
`order`   | `asc`, `desc` | The sort order. Default value is `asc`.
                  
__Example__

Ordering the list of events by title in ascending order:

```xml
GET /api/events?sort=title&order=asc
```

# Filtering

Filtering of result sets is supported by a set of well-defined fields per request. Multiple filter criteria can be defined by specifying the `filter` parameter more than once. In this case, the criteria are applied using logical `and`.

Each api request explicitly defines the fields that support filtering.

Parameter | Description
:---------|:-----------
`filter`  | The filter. Filter conditions must be URL encoded

__Example__

Filter the list of events by status and by series.

```xml
GET /api/events?filter=status%3dpublished&filter=series%3dmath
```

# Paging

When loading large result sets, being able to address and access the data in well-defined chunks using a limit and offset is essential. Paging is enabled for all requests that return lists of items.

Paramter | Description
:--------|:-----------
`limit`  | The maximum number of records to return per request
`offset` | The index of the first record to return

__Example__

From the list of events, return items 50-74.

```xml
GET /api/events?limit=25&offset=50
```
