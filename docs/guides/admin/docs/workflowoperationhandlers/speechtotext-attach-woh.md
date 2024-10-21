Speech to Text Attach Workflow Operation
========================================

ID: `speechtotext-attach`

Description
-----------

The speech to text attach operation can be used to attach the generated subtitles from a previously run
[`speechtotext`](speechtotext-woh.md) operation if the process was started asynchronously.

Parameter Table
---------------

| Configuration Keys       | Required | Example          | Description
|--------------------------|----------|------------------|-------------
| target-flavor            | yes      | archive          | Flavor of the produced subtitle
| target-tags              | no       | captions/source  | Tags for the subtitle file².
| target-element           | no       | track            | Define where to append the subtitles file. Possibilities are: as a 'track' or as an 'attachment' (default: `track`).


Operation Examples
------------------

```yaml
- id: speechtotext
  description: Generates subtitles for video and audio files
  configurations:
    - source-flavor: '*/source'
    - target-flavor: captions/source
    - limit-to-one: true
    - async: true

- … other operations

- id: speechtotext-attach
  description: Attach generated subtitles
  configurations:
    - target-flavor: captions/source
    - target-tags: engage-download
```
