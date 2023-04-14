AmberScript Start Transcription Workflow Operation
==================================================

ID: `amberscript-start-transcription`


Description
-----------

The AmberScript Start Transcription operation invokes the AmberScript transcription service by submitting
an audio or video file to be transcribed and captioned.


Parameter Table
---------------

| configuration keys    | description                                                                          | default      | example            |
|-----------------------|--------------------------------------------------------------------------------------|--------------|--------------------|
| source-tag            | A tag selecting the audio or video file to be sent for translation/transcription.    | -            | engage-download    |
| source-flavor         | A flavor selecting the audio or video file to be sent for translation/transcription. | -            | \*/themed          |
| jobtype               | direct (automated, fast) or perfect (additional manual improvements, slow).          | direct       | perfect            |
| language              | The assumed language for transcription.                                              | en           | nl                 |
| skip-if-flavor-exists | If this flavor already exists in the media package, skip this operation.             | captions/vtt | captions/timedtext |

### Supported Languages

At time of writing Amberscript supports the following language codes:

- da
- de
- en
- es
- fi
- fr
- nl
- no
- sv
