WhisperC++ Transcription Engine
===============================

Overview
--------

Opencast can take advantage of [Open AI's Whisper Project](https://github.com/openai/whisper) to generate automatic
transcriptions on premise through [SpeechToText WoH](../../workflowoperationhandlers/speechtotext-woh.md).

WhisperC++ is a C/C++ implementation of OpenAI's Whisper automatic speech recognition (ASR) model.

Advantages
----------
- Transcription on more than 80 languages
- Translation to English
- Automatic language detection
- Faster processing than whisper when using CPUs
- Run locally, no data sent to third parties.

Enable WhisperC++ engine
------------------------

To enable WhisperC++ as for the `SpeechToText` WoH, follow these steps.

1. Install whispercpp binary and language models following the instruction on
[whisper.cpp](https://github.com/ggerganov/whisper.cpp) repository or build and use your own RPMs for RHEL-based
distributions from [whispercpp-rpmbuild](https://github.com/elearning-univie/whispercpp-rpmbuild).
2. Enable whispercpp engine and set job load in `org.opencastproject.speechtotext.impl.SpeechToTextServiceImpl.cfg`.
3. Set the target model to use in `org.opencastproject.speechtotext.impl.engine.WhisperCppEngine`.
4. WhisperC++ processes only PCM16 (wav) audio files. Therefore you probably have to add an `encode`-operation before
running `speechtotext` in your workflow and an encoder profile:

```
  - id: encode
    description: "Extract audio for processing with whispercpp"
    configurations:
      - source-flavor: "*/themed"
      - target-flavor: "*/audio+stt"
      - encoding-profile: audio-whisper

  - id: speechtotext
    description: "Generates subtitles with whispercpp"
    configurations:
      - source-flavor: "*/audio+stt"
      - target-flavor: captions/vtt+auto-#{lang}
      - target-element: attachment
      - target-tags: archive,subtitle,engage-download
      # - translate: true
```

```
# Audio-only encoding format used for whispercpp
profile.audio-whisper.name = whispercpp wav
profile.audio-whisper.input = audio
profile.audio-whisper.output = audio
profile.audio-whisper.suffix = -stt.wav
profile.audio-whisper.ffmpeg.command = -i #{in.video.path} -vn -ar 16000 -ac 1 -c:a pcm_s16le #{out.dir}/#{out.name}#{out.suffix}
profile.audio-whisper.jobload = 1.0
```


Additional Notes
----------------

- Considerations about model usage and job load configuration in the [Additional notes](whisper.md#additional-notes)
  from the `Whisper Transcription Engine` are also valid for this engine.
- WhisperC++ can be run on CPU only or (partially) on dedicated hardware (Nvidia GPU, Apple Core ML) - if whispercpp
  binary/library is built with appropriate options/modules.
- Language models are based on Whisper and are transformed to a custom ggml format. You can download already converted
  models or build and tweak your own models. For more informations visit
  [models/README.md](https://github.com/ggerganov/whisper.cpp/blob/master/models/README.md).
