Vosk Transcription Engine
=========================

Overview
--------
[Vosk](https://alphacephei.com/vosk/) is an offline open-source speech recognition toolkit. It enables speech recognition for 20+ languages.
This engine is use by [SpeechToText WoH](../../workflowoperationhandlers/speech-to-text-woh.md)

Enable Vosk in Opencast
-----------------------

Vosk is enabled by default in Opencast but it needs to be installed 
[vosk-cli](https://github.com/elan-ev/vosk-cli#installation) to work.

Configure Vosk
--------------

The command that executes Vosk, the main STT engine or the job load can be modified in, 
`org.opencastproject.speechtotext.impl.SpeechToTextServiceImpl.cfg` and 
`org.opencastproject.speechtotext.impl.engine.VoskEngine.cfg`
