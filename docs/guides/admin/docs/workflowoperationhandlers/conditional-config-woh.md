Conditional Config Workflow Operation
=====================================

ID: `conditional-config`

Description
-----------

The conditional-config operation sets a workflow configuration variable based on conditions that are
tested in sequence.

If _condition-1_ is true, _value-1_ is assigned to the variable, if _condition-2_ is true, _value-2_ is
assigned, and so on. If no conditions are true, the value specified in _no-match_ is assigned.

Conditions are tested in alphabetical order and the first condition that is true sets the variable so,
if two conditions are true, the value is set by the first one that gets evaluated.

This operation is useful to reduce workflow complexity when the same operation is repeated many times and
executed based on distinct if-conditions.


Parameter Table
---------------

|configuration key|description|example|
|-----------------|-----------|-------|
|configuration-name|Name of workflow configuration variable to be set|encoding-profile|
|condition-**X**|Condition to be tested, same syntax as workflow conditions|(${presenter_work_resolution_x} &gt; 1600)|
|value-**X**|Value to be assigned to configuration variable if condition-<X> is true|encoding-profile-x|
|no-match|Value to be assigned to configuration variable if none of the conditions are true|encoding-profile-none|

All _condition-**X**_ are sorted as strings and then evaluated in sequence e.g. condition-1, condition-2, etc.


Example
-------

Set presenter encoding profile based on presenter/work media attributes:

```xml
<operation id="conditional-config" 
           description="Evaluate media properties and set presenter-encoding-profiles configuration">
	<configurations>
		<configuration key="configuration-name">presenter-encoding-profiles</configuration>
		<configuration key="condition-1">
			(${presenter_work_framerate} == 25) AND (${presenter_work_resolution_x} &gt; 1600)
			AND (${presenter_work_bitrate} &gt; 1999999)
		</configuration>
		<configuration key="value-1">
			z-full-3m-presenter,z-threequarters-1500k-presenter,z-half-700k-presenter,
			z-quarter-300k-presenter,z-lowbr-160k-presenter,multiencode-hls
		</configuration>
		<configuration key="condition-2">
          	(${presenter_work_framerate} == 25) AND (${presenter_work_resolution_x} &gt; 1600)
          	AND (${presenter_work_bitrate} &lt; 2000000) AND (${presenter_work_bitrate} &gt; 499999)
		</configuration>
		<configuration key="value-2">
			z-full-2m-presenter,z-threequarters-1m-presenter,z-half-500k-presenter,
			z-quarter-250k-presenter,z-lowbr-160k-presenter,multiencode-hls
		</configuration>
		<!-- More conditions omitted… -->
		<configuration key="no-match">
			hls-half-res-presenter,hls-full-res-presenter,hls-threequarters-res-presenter,
			hls-quarter-res-presenter,hls-quarter-15fps-presenter,multiencode-hls
		</configuration>
	</configurations>
</operation>
```

Then, use variable set above to encode the presenter file:

```xml
<operation id="multiencode"
           description="Encode to multiple delivery formats">
	<configurations>
		<configuration key="source-flavors">presenter/work</configuration>
		<configuration key="target-flavors">*/delivery</configuration>
		<configuration key="target-tags">archive,engage</configuration>
		<configuration key="encoding-profiles">
			${presenter-encoding-profiles}
		</configuration>
		<configuration key="tag-with-profile">true</configuration>
	</configurations>
</operation>
```


