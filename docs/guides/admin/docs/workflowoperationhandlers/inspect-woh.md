# InspectWorkflowOperation

## Description

The InspectWorkflowOperation is used to inspect all tracks of a media package. It tries to verify if they are valid
media tracks.
The InspectWorkflowOperation will also set the duration and creation date of the dublincore/episode catalog
(if available) to the media package duration and media package creation date.

## Parameter Table

|Configuration Key    |Type    |Description                                                                |Default |
|---------------------|--------|---------------------------------------------------------------------------|--------|
|accept-no-media      |Boolean |Whether mediapackages with no media tracks should be accepted              |false   |
|accurate-frame-count |Booelan |Whether the media inspection service should determine the exact frame count|false   |
|overwrite            |Boolean |Whether to rewrite existing metadata                                       |false   |

### Accept No Media

If the configuration key `accept-no-media`is set to `false`, the operation will fail if the media package does not
contain any media tracks. If this behavior is not appropriate, set `accept-no-media` to `true`.

### Accurate Frame Count

The media inspection service will provide the number of frames in case of video streams. Normally, this information is
extracted from the media file header. In case of incorrect media file headers, this information might not be accurate.
Using the configuration key `accurate-frame-count`, the media inspection service can be forced to perform a full
decoding of the video stream. While this does result in an exact count of frames, this is expensive in terms of
computation power.

### Overwrite

The inspection service will try to fill empty metadata fields. It will not overwrite any existing values except when
you specify the option `overwrite` as `true`.

## Operation Example

    <operation
        id="inspect"
        fail-on-error="true"
        exception-handler-workflow="error"
        description="Inspecting mediapackage track elements">
        <configurations>
            <configuration key="overwrite">false</configuration>
            <configuration key="accept-no-media">false</configuration>
            <configuration key="accurate-frame-count">false</configuration>
        </configurations>
    </operation>
