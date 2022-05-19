Metadata to ACL Workflow Operation
==================================

ID: `metadata-to-acl`

Description
-----------

The metadata-to-acl operation can be used to automatically add access rights for users mentioned in metadata fields to
the access control list.
Users are identified by their username.
The operation works like this:

1. Get values for configured field from the episode Dublin Core catalog.
2. Look up if a user with this username exists
3. Add read and write permissions for this user

Please consider carefully how you use this operation since this effectively means that anyone who can modify metadata
can also modify access rights.


Parameter Table
---------------

|Name   |Default   |Description                                         |
|-------|----------|----------------------------------------------------|
|field  |publisher |The Dublin Core element to convert to access rights |


Operation Example
-----------------

```xml
<operation
  id="metadata-to-acl"
  description="Add read/write access for `publisher`">
  <configurations>
    <configuration key="field">publisher</configuration>
  </configurations>
</operation>
```
