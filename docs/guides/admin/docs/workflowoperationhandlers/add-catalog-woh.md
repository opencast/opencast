AddCatalogWorkflowOperationHandler
==================================

Description
-----------

This operation adds a catalog to the media package of the running workflow.
The catalog to add is specified by path. Additionally the name, flavor and tags of the catalog can be configured.
If a catalog of the same flavor already exits in the media package,
the parameter `catalog-type-collision-behavior` specifies how this case is handled.


Parameter Table
---------------

| Configuration Key                 | Example                                        | Description                                         |
|-----------------------------------|------------------------------------------------|-----------------------------------------------------|
| `catalog-path`                    | `${karaf.etc}/catalogs/default_dublincore.xml` | Path to the catalog                                 |
| `catalog-flavor`                  | `dublincore/episode`                           | Flavor of the catalog                               |
| `catalog-name`                    | `dublincore.xml`                               | Name of the catalog                                 |
| `catalog-tags`                    | `archive,dublincore`                           | List of tags, separated by commas                   |
| `catalog-type-collision-behavior` | `keep`                                         | How a collision is handled (more information below) |

All parameters are mandatory except `catalog-tags`.


### catalog-type-collision-behavior

If the flavor of the new catalog and the flavor of an already existing catalog match,
the `catalog-type-collision-behavior` specifies how this situation is handled.
There are multiple supported options:
- `keep`: The new catalog is added despite the collision. This results in two catalogs of the same type coexisting.
- `skip`: The addition of the new catalog is skipped, the new catalog is not added.
- `fail`: The workflow operation fails with an error, depending on the your configurations the complete workflow is aborted.


Operation Example
-----------------

```xml
<operation
  id="add-catalog"
  fail-on-error="true"
  exception-handler-workflow="partial-error"
  description="Add catalog to media package">
  <configurations>
    <configuration key="catalog-path">${karaf.etc}/catalogs/default_dublincore.xml</configuration>
    <configuration key="catalog-flavor">dublincore/episode</configuration>
    <configuration key="catalog-name">dublincore.xml</configuration>
    <configuration key="catalog-tags">archive,dublincore</configuration>
    <configuration key="catalog-type-collision-behavior">keep</configuration>
  </configurations>
</operation>
```
