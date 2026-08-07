Configure by DCTerm Workflow Operation
======================================

ID: `configure-by-dcterm`


Description
-----------

With the ConfigureByDCTermWorkflowOperationHandler it's possible to create a workflow configuration property according
to whether a Dublin Core term in a catalog has a specific value. So for example it's possible to control a workflow so
that it will publish before editing if a certain Dublin Core term has the specified value.

In combination with [TagByDCTermWorkflowOperationHandler](tag-by-dcterm-woh.md) workflows can be controlled by the
metadata contained within the Dublin Core catalogs.


Parameter Table
---------------

Tags and flavors can be used in combination.

| configuration keys | example                              | description                                                                                        | default value |
|--------------------|--------------------------------------|----------------------------------------------------------------------------------------------------|---------------|
| dccatalog          | "episode" or "series"                | the subtype of catalog in which to search for `dcterm`                                             | EMPTY         |
| dccatalog-type     | "dublincore"                         | the type of catalog in which to search for `dcterm`                                                | "dublincore"  |
| dcterm             | "{http://purl.org/dc/terms/}creator" | the XML Expanded Name, consists of a namespace and the name of the Dublin Core term which to check | EMPTY         |
| match-value        | "Joe Bloggs"                         | the Dublin Core term value to check for                                                            | EMPTY         |
| default-value      | "Anon"                               | the implied value if the dubincore term is not present in the catalog                              | EMPTY         |
| *configProperty*   | true / false                         | a configuration property and the value it will be given if a match is found                        | EMPTY         |

### dccatalog

The flavor subtype of Dublin Core catalog in which to look for the `dcterm`. This will usually be `episode` or `series`.

### dccatalog-type

The flavor type of Dublin Core catalog in which to look for the `dcterm`. This will usually be `dublincore`.
Will default to 'dublincore'.

### dcterm

The expanded name is a pair consisting of a namespace name and a local name.
The local name is the name of the Dublin Core term to look for in the `dccatalog` and the namespace name is the name of 
the namespace it belongs to.
The Dublin Core term could be one of the terms set by Opencast or an additional term adding to the catalog.

### match-value

The value of the `dcterm` which to match against. The comparison is case sensitive.

### default-value

If `default-value` is used when the `dcterm` is not found in the catalog. If not specified the operation will treat the
match as false and not configure anything. If `default-value` is specified the operation will compare the `match-value`
to the `default-value` and set the workflow property if they match. This allows an implied value to be explicitly and
clearly defined. For example if you have mediapackages that were created before additional metadata was added to the
episode catalog you may want to imply that the `audience` term has a value of `all-enrolled`.

### "configProperty"

Specifies as the key the name of a new workflow configuration property and the boolean value to which it will be set if
the Dublin Core term matches the specified value.

Due to the way a workflow evaluates operation `if` conditions as configuration properties are created, only new
configuration properties can be used to modify the execution of subsequent operations. Also since an undefined property
will be evaluated as `false` in practice the only useful value which can set is `true`.  However operation `if`
conditions can be negated though so it is possible to skip subsequent operations on matched `dcterm`  value.


Operation Example
-----------------

```yaml
  - id: configure-by-dcterm
    description: Configure publication channel by dcterm
    configurations:
      - dccatalog: episode
      - dcterm: "{http://purl.org/dc/terms/}audience"
      - match-value: private
      - publishPrivate: true

...

  - id: publish-engage
    if: ${publishPrivate}
    description: Publish to internal audience only

  - id: publish-youtube
    if: NOT ${publishPrivate}
    description: Publish to global audience
   ...

```
