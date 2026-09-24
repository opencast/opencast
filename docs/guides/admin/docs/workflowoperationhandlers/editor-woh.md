Video Editor Workflow Operation
===============================

ID: `editor`

Description
-----------

The editor operation cuts the source file(s) according to the given *SMIL* file(s). SMIL file(s) are usually generated
by the video editor frontend.

It expects a SMIL catalog (`smil-flavors`) describing one or more segments to keep, and one or more source tracks
(`source-flavors`) to cut. For every matching source track it produces a new, derived track whose flavor subtype is
replaced with `target-flavor-subtype` (e.g. `presenter/source` becomes `presenter/trimmed`), containing only the
segments the SMIL defines. The SMIL catalog itself is rewritten in place under `target-smil-flavor` with the source
tracks it references resolved to the actual tracks that were cut.

If `skip-if-not-trimmed` is enabled and the SMIL contains only a single segment spanning the entire recording (i.e.
nothing was actually cut), no encoding is performed. Instead, the tracks named by `skipped-flavors` are cloned
directly to the target flavor, unchanged. The same cloning happens if the operation is configured to run
non-interactively and no SMIL catalog has been created for the media package yet, in which case cutting is skipped
entirely.

## Parameter Table

|configuration keys|example    |description                                                    |
|------------------|-----------|---------------------------------------------------------------|
|source-flavors    |`*/work`   |The flavor(s) of all media files to process                    |
|smil-flavors      |`*/smil`   |The flavor(s) of the SMIL file(s) to be used                   |
|skipped-flavors   |`*/work`   |(Optional) The flavor(s) of the media files to clone to the target flavor when the editor operation is skipped (see `skip-if-not-trimmed`) or run non-interactively without ever holding for edits. Defaults to the value of `source-flavors`. This only needs to be set to something different from `source-flavors` when the tracks used for interactive editing are not the tracks that should continue through the workflow when no trimming happens, e.g. when `source-flavors` points at lower-resolution `*/preview` tracks used just for the editor UI, but skipping should pass through the full-quality `*/work` tracks instead.|
|target-flavor-subtype|`trimmed`|The flavor subtype to be applied to all resulting videos, e.g. for a value of `baz`, a track with flavor `foo/bar` will generate another track with flavor `foo/baz`|
|target-smil-flavor| `smil/cutting` |the flavor of the SMIL file containing the final video segments.<br/>Should be the same as the `smil.catalog.flavor` property in `etc/org.opencastproject.editor.EditorServiceImpl.cfg`|
|skip-if-not-trimmed|`false`       |(Optional) if set to `true`, the track encoding will be skipped if no trimming points were defined (i.e. there is only one segment from the very beginning to the very end of the video). Defaults to `true`|
|skip-processing|`true`|Do not do the actual encoding, just create the smil file and exit. This option is used with *process-smil* workflow operation, which will use the smil to run the encodings then. Default is false. |
|*preview_flavors*|*`*/preview`*|*(Legacy) Flavors used to preview the video in the editor.*<br/>***Currently has no effect. Preview flavors are now configured in the file `etc/org.opencastproject.editor.EditorServiceImpl.cfg`***|
|*interactive*|*`false`*|*(Legacy) If `true` make the operation interactive, i.e. pause and wait for user input.*<br/>***Do not use. Interactive operations are deprecated in the current API.***|


Operation Example
-----------------

````yaml
  - id: editor
    description: Waiting for user to review / video edit recording
    configurations:
      - source-flavors: '*/work'
      - skipped-flavors: '*/work'
      - smil-flavors: '*/smil'
      - target-smil-flavor: smil/cutting
      - target-flavor-subtype: trimmed
```
