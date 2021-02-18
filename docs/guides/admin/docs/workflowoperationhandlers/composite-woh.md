# Composite Workflow Operation Handler

## Description

The CompositeWorkflowOperationHandler is used to composite two videos (upper and lower) and an optional watermark into
one video, including encoding to different formats. The audio track is taken from both videos by default. Everything is
done using FFmpeg. The composition can be done in various layout formats e.g. side by side or picture in picture. The
layout has to be defined in JSON format and is described in section "Layout Definition". For some general information
about layouts see Opencast Composer Layout Module.

The internal FFmpeg command is using the following filters: scale for scaling the videos, pad for defining the output
dimension including the background color, movie for adding additional videos and images and overlay for aligning the
videos and images to the output dimension. More info can be found here: https://trac.ffmpeg.org/wiki/FilteringGuide

If both upper and lower tracks have audio, "source-audio-name" can be set to "upper", "lower" or "both"
to choose only the audio from one track or both tracks for the composite video.

### Sample complex composite filter command

    -filter:v "[in]scale=640:480,pad=1920:1080:20:20:black[lower];movie=test.mp4,scale=640:480[upper];movie=watermark.jpg[watermark];[lower][upper]overlay=200:200[video];[video][watermark]overlay=main_w-overlay_w-20:20[out]" sidebyside.mp4

## Parameter Table
Tags and flavors can be used in combination.


|configuration keys|value type (EBNF)|example|description|default value|
|------------------|-----------------|-------|-----------|-------------|
|source-audio-name|"lower", "upper" or "both" |upper|The "name" of track to use as a source audio.|both|
|source-tags-upper|String , { "," , String }    |comp,rss|The "tag" of the upper track to use as a source input.|EMPTY|
|source-flavor-upper|MediaPackageElementFlavor    |presenter/trimmed|The "flavor" of the upper track to use as a source input.|EMPTY|
|source-tags-lower|String , { "," , String }    |comp,rss|The "tag" of the lower track to use as a source input.|EMPTY|
|source-flavor-lower|MediaPackageElementFlavor    |presenter/trimmed|The "flavor" of the lower track to use as a source input.|EMPTY|
|source-tags-watermark|String , { "," , String }    |branding|The "tag" of the attachment image to use as a source input.|EMPTY|
|source-flavor-watermark|MediaPackageElementFlavor    |image/work|The "flavor" of the attachment image to use as a source input.|EMPTY|
|source-url-watermark|URL    |file:///Users/me/logo.jpg|The "URL" of the fallback image to use as a source input.|EMPTY|
|target-tags|String , { "," , String }    |composite,rss,atom,archive|The tags to apply to the compound video track.|EMPTY|
|\* **target-flavor**|MediaPackageElementFlavor|    composite/delivery    |The flavor to apply to the compound video track.|EMPTY|
|\* **encoding-profile**|String|    composite    |The encoding profile to use.|EMPTY|
|\* **output-resolution**|width , "x" , height &#124; lower &#124; higher|1920x1080|The resulting resolution of the compound video e.g. 1920x1080.|EMPTY|
|output-background|String    |red|The resulting background color of the compound video http://www.ffmpeg.org/ffmpeg-utils.html#Color.|black|
|layout|name | Json , ";" , Json , [ ";" , Json ]|The layout name to use or a semi-colon separated JSON layout definition (lower video, upper video, optional watermark). If a layout name is given than the corresponding layout-{name} key must be defined.|EMPTY|
|layout-single|name | Json , ";" , Json , [ ";" , Json ]|Layout to be used in case of one input video track (see *layout*)|EMPTY|
|layout-dual|name | Json , ";" , Json , [ ";" , Json ]|Layout to be used in case of two input video tracks (see *layout*). Defaults to value of *layout* if not set.|EMPTY|
|layout-{name}|Json , ";" , Json , [ ";" , Json ]         |Define semi-colon separated JSON layouts (lower video, upper video, optional watermark) to provide by name.|EMPTY|


\* **mandatory**

Notes:

* At least one of the configuration keys *layout*, *layout-single*, or *layout-multiple* must be set

## Output Resolution
The output resolution must be specified using the configuration key *output-resolution*. The output resolution can be
either explicitly specified (e.g. 1920x1080) or selected from the lower or upper input video (lower or higher).
In case that only a single input track is available, both part-lower and part-higher will refer to that
single input track.

## Layout Definition

The layout definitions are provided as JSON. Each definition consist of the layout specifications for the lower and
upper video and an optional specification for the watermark. The specifications have to be separated by comma.

**It is always ensured that the media does not exceed the canvas. Offset and scaling is adjusted appropriately.**

A single layout is specified as follows:

    {
      // How much of the canvas shall be covered. [0.0 - 1.0]
      // 1.0 means that the media is scaled to cover the complete width of the canvas keeping the aspect ratio.
      "horizontalCoverage": Double,
      // The offset between the anchor points of the media and the canvas
      "anchorOffset": {
        // The anchor point of the media. [0.0 - 1.0]
        // (0.0, 0.0) is the upper left corner, (1.0, 1.0) is the lower right corner.
        // (0.5, 0.5) is the center.
        "referring": {
          "left": Double,
          "top": Double
        },
        // The anchor point of the canvas.
        "reference": {
          "left": Double,
          "top": Double
        },
        // The offset between the two anchor points.
        "offset": {
          "y": Integer,
          "x": Integer
        }
      }
    }

    // Example.
    // The media is scaled to cover the whole width of the canvas and is placed in the upper left corner.
    {
      "horizontalCoverage": 1.0,
      "anchorOffset": {
        "referring": {
          "left": 0.0,
          "top": 0.0
        },
        "offset": {
          "y": 0,
          "x": 0
        },
        "reference": {
          "left": 0.0,
          "top": 0.0
        }
      }
    }

    // Example.
    // The media is scaled to cover 20% of the width of the canvas and is placed in the lower right corner
    // with an offset of -10px on both x and y axis so that it does not touch the canvas' border.
    {
      "horizontalCoverage": 0.2,
      "anchorOffset": {
        "referring": {
          "left": 1.0,
          "top": 1.0
        },
        "offset": {
          "y": -10,
          "x": -10
        },
        "reference": {
          "left": 1.0,
          "top": 1.0
        }
      }
    }

## Operation Example

    <operation
      id="composite"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Composite">
      <configurations>
        <configuration key="source-flavor-upper">presentation/trimmed</configuration>
        <configuration key="source-flavor-lower">presenter/trimmed</configuration>
        <configuration key="source-tags-upper">comp,rss</configuration>
        <configuration key="source-tags-lower">comp,rss</configuration>
        <configuration key="source-tags-watermark">branding</configuration>
        <configuration key="source-flavor-watermark">image/work</configuration>
        <configuration key="source-url-watermark">file:///Users/me/logo.jpg</configuration>
        <configuration key="encoding-profile">composite</configuration>
        <configuration key="target-tags">composite,rss,atom,archive</configuration>
        <configuration key="target-flavor">composite/delivery</configuration>
        <configuration key="output-resolution">1920x1080</configuration>
        <configuration key="output-background">red</configuration>
        <configuration key="layout">topleft</configuration>
        <configuration key="layout-topleft">
          {"horizontalCoverage":1.0,"anchorOffset":{"referring":{"left":1.0,"top":1.0},"offset":{"y":-20,"x":-20},"reference":{"left":1.0,"top":1.0}}};
          {"horizontalCoverage":0.2,"anchorOffset":{"referring":{"left":0.0,"top":0.0},"offset":{"y":-20,"x":-20},"reference":{"left":0.0,"top":0.0}}};
          {"horizontalCoverage":1.0,"anchorOffset":{"referring":{"left":1.0,"top":0.0},"offset":{"y":20,"x":20},"reference":{"left":1.0,"top":0.0}}}
        </configuration>
        <configuration key="layout-topright">
          {"horizontalCoverage":1.0,"anchorOffset":{"referring":{"left":1.0,"top":1.0},"offset":{"y":-20,"x":-20},"reference":{"left":1.0,"top":1.0}}};
          {"horizontalCoverage":0.2,"anchorOffset":{"referring":{"left":1.0,"top":0.0},"offset":{"y":-20,"x":-20},"reference":{"left":1.0,"top":0.0}}};
          {"horizontalCoverage":1.0,"anchorOffset":{"referring":{"left":0.0,"top":0.0},"offset":{"y":20,"x":20},"reference":{"left":0.0,"top":0.0}}}
        </configuration>
      </configurations>
    </operation>

