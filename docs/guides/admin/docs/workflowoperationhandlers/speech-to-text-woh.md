Speech to Text Workflow Operation
==============================

ID: `speechtotext`

Description
-----------

The speech to text operation can be used to generate subtitles for Videos or Audio files. Currently, there is
only [Vosk](https://alphacephei.com/vosk/) available as STT Engine. The subtitles file format ist WebVTT.


Parameter Table
---------------

|configuration keys|required|description                                                                      |
|------------------|--------|---------------------------------------------------------------------------------|
|source-flavor     |yes     |The source media package to use                                                  |
|target-flavor     |yes     |Flavor of the produced subtitle file                                             |
|target-element    |no      |Define where to append the subtitles file. Possibilities are: as a 'track' or as an 'attachment'. The default is "attachment".|
|language-code     |no      |The language of the video or audio source (default is "eng). It has to match the name of the language model directory. See 'vosk-cli'.|
|target-tags       |no      |Tags for the subtitle file                                                       |


Requirements
------------

In order for it to work, you have to install the vosk-cli package. An installation instruction can be found here: 
https://github.com/elan-ev/vosk-cli. 

Further requirements:
- ffmpeg
- vosk (with pip - package installer for python)
- webvtt-py (pip)


Operation Examples
------------------

If you want to display the subtitles in the Paella player, the WebVTT files have to be published and
the target SubFlavor has to be configured like this: ```vtt+LANG```, where ```LANG``` is
the language option, that will be displayed in the Paella player. Please note the description above
for the ```language-code``` field.

```XML
<operation
    id="speechtotext"
    description="Generates subtitles for video and audio files">
  <configurations>
    <configuration key="source-flavor">*/source</configuration>
    <configuration key="target-flavor">captions/vtt+en</configuration>
    <configuration key="target-element"> attachment | track </configuration>
    <configuration key="target-tags">archive,subtitle,engage-download</configuration>
    <configuration key="language-code">eng</configuration>
  </configurations>
</operation>
```
