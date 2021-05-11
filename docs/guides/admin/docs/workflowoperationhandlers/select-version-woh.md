Select Version Workflow Operation
=================================

ID: `select-version`

Description
-----------

The *select-version* operation replaces the current media package in the running workflow with a previous
version from the asset manager. This operation should be one of the first operations in a workflow e.g. before ingest-download.

This operation is useful in situations where we want to run a workflow using an older version of the media 
package from the asset manager, such as rolling back, retrieving discarded media previously archived, etc.

There are currently two ways of specifying which version to select, which are **mutually exclusive**:

- A version number
- A combination of *source-flavors* and *no-tag* (see below).

Parameter Table
---------------

|configuration key|description|example|
|-----------------|-----------|-------|
|version|Which version number to select from the asset manager|0|
|source-flavors|The flavors where to check for non-existence of the tags passed in *no_tags*|presenter/delivery|
|no-tags|Tags that need to be absent in the elements with *source-flavors* flavor|aTag|

Examples
--------

### Example 1

Select version **0** of the media package from the asset manager:

```xml
<operation id="select-version"
           description="Select version 0 of media package to use in current workflow">
	<configurations>
		<configuration key="version">0</configuration>
	</configurations>
</operation>
```

### Example 2

Select the latest version where delivery media do not have the tags *hls-presenter-mp4* and *hls-presentation-mp4*:

```xml
<operation id="select-version"
           description="Select version with no hls tags in delivery media to use in current workflow">
	<configurations>
		<configuration key="source-flavors">presenter/delivery,presentation/delivery</configuration>
		<configuration key="no-tags">hls-presenter-mp4,hls-presentation-mp4</configuration>
	</configurations>
</operation>
```
