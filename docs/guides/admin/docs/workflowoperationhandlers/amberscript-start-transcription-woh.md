AmberScript Start Transcription Workflow Operation
==================================================

ID: `amberscript-start-transcription`


Description
-----------

The AmberScript Start Transcription operation invokes the AmberScript transcription service by submitting
an audio or video file to be transcribed and captioned.


Parameter Table
---------------

| configuration keys    | description                                                                          | default       | example                  |
|-----------------------|--------------------------------------------------------------------------------------|---------------|--------------------------|
| source-tag            | A tag selecting the audio or video file to be sent for translation/transcription.    | -             | engage-download          |
| source-flavor         | A flavor selecting the audio or video file to be sent for translation/transcription. | -             | \*/themed                |
| jobtype               | direct (automated, fast) or perfect (additional manual improvements, slow).          | direct        | perfect                  |
| language              | The assumed language for transcription.                                              | en            | nl                       |
| speaker               | The number of speakers in the recording.                                             | 1             | 2                        |
| transcriptiontype     | transcription, captions or translatedSubtitles                                       | transcription | captions                 |
| glossary              | An ID of a custom glossary to use.                                                   | -             | 643966c3f3e91e0c96e9e060 |
| transcriptionstyle    | cleanread or verbatim                                                                | cleanread     | verbatim                 |
| targetlanguage        | The language of the resulting transcriptions.                                        | -             | en                       |
| skip-if-flavor-exists | If this flavor already exists in the media package, skip this operation.             | captions/vtt  | captions/timedtext       |

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

### Glossaries

The `glossary` ID needs to be a valid ID for a previously created custom glossary.
See also https://amberscript.github.io/api-docs/#glossary.
If this parameter is unset, the default configured in
`etc/org.opencastproject.transcription.amberscript.AmberscriptTranscriptionService.cfg`
is used.

If you don't want to use a glossary in a specific workflow (instance)
even though a fallback is configured in that file,
you can provide an empty string as glossary ID, i.e. `glossary=""`.
