Whisper Transcription Engine
============================

Overview
--------

Opencast can take advantage of [Open AI's Whisper Project]() to generate automatic transcriptions on premise through 
[SpeechToText WoH](../../workflowoperationhandlers/speechtotext-woh.md).

Advantages
----------
- Transcription on more than 80 languages
- Fast processing (When using a GPU)
- Run locally, no data sent to third parties.

Enable Whisper engine
---------------------
To enable Whisper as for the `SpeechToText` WoH, follow these steps.

1. [Install whisper](https://github.com/openai/whisper#setup) on the worker nodes.
2. Enable whisper and set Job load in `org.opencastproject.speechtotext.impl.SpeechToTextServiceImpl.cfg`.
3. Set the target model to use in `org.opencastproject.speechtotext.impl.engine.WhisperEngine`.


Additional Notes
----------------

- Whisper can be run on CPU or GPU, the use of a GPU increase the performance dramatically.
- [There are five languages models available to use](https://github.com/openai/whisper#available-models-and-languages), 
from the lightest (tiny) to the most complete (large), having a
bigger model improves the accuracy but diminishes processing speed.
- It's recommended to set a Job load for each machine. 
  - In the case that one want to use only one worker node with Whisper,
one can set the job load to be bigger than the size on the non Whisper nodes. The whisper Job will only be run on 
the Whisper machines (Whose nodes have higher job load set).
  - Also, is a good idea on the Whisper node to configure it.
    - Avoid workflows failures over not enough memory with parallel transcriptions.
    - Performance bottleneck with too many parallel transcriptions.

#### Not yet implemented

This is a list of features that are not implemented, they will be added in the near future.

- Translation to English
- Automatic language recognition and tagging
