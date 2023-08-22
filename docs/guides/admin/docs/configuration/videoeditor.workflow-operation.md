
## Workflow Operations

A short list of editor related workflow operations. For more and information and example usage check their respective
documentation pages, or see their usage in the default workflows.

### Waveform Operation
The [**Waveform** operation](../workflowoperationhandlers/waveform-woh.md) creates an image showing the temporal audio activity within the recording. T

### Silence Operation

The [**Silence** operation](../workflowoperationhandlers/silence-woh.md) performs a silence detection on an audio-only input file.

#### Silence Detection Configuration

The settings regarding the sensitivity of the silence detection can be changed in
`etc/org.opencastproject.silencedetection.impl.SilenceDetectionServiceImpl.cfg`.

### Editor Operation

The [**Editor** operation](../workflowoperationhandlers/editor-woh.md) cuts the source file(s) according to the given *SMIL* file(s). SMIL file(s) are usually generated
by the video editor frontend.

#### Editor Configuration

The FFmpeg properties for the edtior operation can be modified in
`etc/org.opencastproject.videoeditor.impl.VideoEditorServiceImpl.cfg`. Usually there should be no reason to touch this
file.

To configure the trimming of the start/end of a video by the editor operation, check the bottom of
`etc/org.opencastproject.organization-mh_default_org.cfg`.
(TODO: Confirm if these values are actually still used by the editor?!)
