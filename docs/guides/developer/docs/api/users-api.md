[TOC]

# General

### GET /api/users

Returns a list of users.

Parameters                 |Type            | Description
:--------------------------|:---------------|:----------------------------
`search`                   | String         | Search by name

__Response__

`200 (OK)`: A (potentially empty) list of users.

```
[
  {
    "username": "john.doe",
    "name": "John Doe"
  },
  {
    "username": "jean.dupont",
    "name": "Jean Dupont"
  }
]
```
> Note: The above is an example on what the detailed documentation will look like and does not represent the actual request or response.

<!--- ##################################################################### -->
### GET /api/users/{user_id}

Returns a single user.

__Response__

`200 (OK)`: The user is returned.<br/>
`404 (NOT FOUND)`: The specified user does not exist.

```
{
  "username": "john.doe",
  "name": "John Doe"
}
```

<!--- ##################################################################### -->
### POST /api/users

Creates a user.

Form Parameters                 |Type            | Description
:-------------------------------|:---------------|:----------------------------
`user`                          | String         | The user in JSON format

__Sample__

user:
```
{
  "username": "john.doe",
  "name": "John Doe",
  "email": "john@mail.com",
  "password": "123456",
  "roles": [
    "ROLE_ADMIN",
    "ROLE_API"
  ]
}
```

__Response__

`201 (CREATED)`: A new user is created.<br/>
`400 (BAD REQUEST)`: The request is invalid or inconsistent.

<!--- ##################################################################### -->
### PUT /api/users/{user_id}

Updates a user.

Form Parameters                 |Type            | Description
:-------------------------------|:---------------|:----------------------------
`user`                          | String         | The user in JSON format

__Sample__

user:
```
{
  "username": "john.doe",
  "name": "John Doe Junior",
  "email": "john_junior@mail.com",
  "password": "123456",
  "roles": [
    "ROLE_ADMIN",
    "ROLE_API"
  ]
}
```

__Response__

`200 (OK)`: The user has been updated.<br/>
`404 (OK)`: The specified user does not exist.<br/>

<!--- ##################################################################### -->
### DELETE /api/users/{user_id}

Deletes a user.

__Response__

`204 (NO CONTENT)`: The user has been deleted.<br/>
`404 (OK)`: The specified user does not exist.
