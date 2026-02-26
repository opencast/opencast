# GraphQL API - Technology Preview

## Introduction

The Opencast GraphQL API is a modern, flexible, and powerful alternative to the existing REST API. It is designed to provide a more efficient and flexible way to interact with Opencast. The GraphQL API is available as Tech Preview it is not yet ready for production and may change without notice.

## Architectural Overview

The GraphQL API is a single endpoint that allows clients to query and mutate data. The API is based on the [GraphQL](https://graphql.org/) query language. The API is designed to be flexible and efficient, allowing clients to request only the data they need in a single request.

### Url Space
The GraphQL API is located at the `/graphql` namespace on the Opencast admin node. This results in all requests to the
External API starting with `https://<hostname>/graphql`, where the hostname is depending on the installation and tenant
(see “Multi Tenancy”). The whole schema is available at `https://<hostname>/graphql/schema.json`.

### GraphQL UI
The Opencast-Admin node comes with a built-in UI that allows you to explore the schema and execute queries. The UI is available at `https://<hostname>/graphql-ui`.
