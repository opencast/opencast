Extract Text Workflow Operation
=======================================

ID: `extract-text`


Description
-----------

The extract-text operation will try to extract test from a video using Tesseract OCR.


Parameter Table
---------------

|configuration keys|example          |description|
|------------------|-----------------|-----------|
|source-flavor     |presentation/work|Specifies which media should be processed|
|source-tags       |text             |Specifies which media should be processed|
|target-tags       |engage           |Specifies the tags for the produces media|


Operation Example
-----------------

```yaml
  - id: extract-text
    description: Extracting text from presentation segments
    configurations:
      - source-flavor: presentation/trimmed
      - source-tags: ''
      - target-tags: engage,archive
```
