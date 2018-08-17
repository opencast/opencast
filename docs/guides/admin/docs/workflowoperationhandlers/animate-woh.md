Animate Workflow Operation
==============================

ID: `animate`

Description
-----------

The animate operation can be used to generate an animated video clip using [Synfig](https://synfig.org). It can
automatically including episode and series metadata (e.g. the title) into the animation. For example, this can be used
to automatically generate custom intro videos.


Parameter Table
---------------

|configuration keys|required|description                                                                      |
|------------------|--------|---------------------------------------------------------------------------------|
|animation-files   |yes     |The source animation file to use                                                 |
|target-flavor     |yes     |Flavor of the generated video                                                    |
|width             |no      |Width of the generated video                                                     |
|height            |no      |Height of the generated video                                                    |
|fps               |no      |FPS of the generated video                                                       |
|cmd-args          |no      |Custom synfig arguments. Will override all arguments except input and output file|
|target-tags       |no      |Tags for the generated video                                                     |


Synfig Files
------------

Synfig animation files used for input must be saved uncompressed or the metadata replacement will not work. Uncompressed
files usually have the file extension `.sif` and *not* `.sifz`.


### Metadata Replacements

You can use all metadata fields present in the episode and series DublinCore catalogs of an event. In SynfigStudio, just
use placeholders of the following form:

    '{{' ['series' | 'episode'] '.' DC-FIELD '}}'

Here are some common examples:

- `{{episode.title}}`
- `{{episode.creator}}`
- `{{series.title}}`


Operation Examples
------------------

```XML
<operation
  id="animate"
  description="Create animated video clip">
  <configurations>
    <configuration key="animation-file">/path/to/animation.sif</configuration>
    <configuration key="target-flavor">presentation/intro</configuration>
    <configuration key="target-tags">archive</configuration>
  </configurations>
</operation>
```
