# Composite workflow Operation Handler

## Description
The CompositeWorkflowOperationHandler is used to composite two videos (upper and lower) and an optionally watermark into one video, including encoding to different formats. The audio track is always taken from the lower video. Everything is done using FFmpeg. The composition can be done in various layout formats e.g. side by side or picture in picture. The layout has to be defined in JSON format and is described in section "Layout Definition". For some general information about layouts see Matterhorn Composer Layout Module.
 
The internal ffmpeg command is using the following filters: scale for scaling the videos, pad for defining the output dimension including the background color, movie for adding additional videos and images and overlay for aligning the videos and images to the output dimension. More info can be found here: https://trac.ffmpeg.org/wiki/FilteringGuide

### Sample complex composite filter command

    -filter:v "[in]scale=640:480,pad=1920:1080:20:20:black[lower];movie=test.mp4,scale=640:480[upper];movie=watermark.jpg[watermark];[lower][upper]overlay=200:200[video];[video][watermark]overlay=main_w-overlay_w-20:20[out]" sidebyside.mp4

## Parameter Table
Tags and flavors can be used in combination.

<table>
<tr><th>configuration keys</th><th>value type (EBNF)</th><th>example</th><th>description</th><th>default value</th></tr> 
<tr><td>source-tags-upper</td><td>String , { "," , String }	</td><td>comp,rss</td><td>The "tag" of the upper track to use as a source input.</td><td>EMPTY</td></tr>
<tr><td>source-flavor-upper</td><td>MediaPackageElementFlavor	</td><td>presenter/trimmed</td><td>The "flavor" of the upper track to use as a source input.</td><td>EMPTY</td></tr>
<tr><td>source-tags-lower</td><td>String , { "," , String }	</td><td>comp,rss</td><td>The "tag" of the lower track to use as a source input.</td><td>EMPTY</td></tr>
<tr><td>source-flavor-lower</td><td>MediaPackageElementFlavor	</td><td>presenter/trimmed</td><td>The "flavor" of the lower track to use as a source input.</td><td>EMPTY</td></tr>
<tr><td>source-tags-watermark</td><td>String , { "," , String }	</td><td>branding</td><td>The "tag" of the attachment image to use as a source input.</td><td>EMPTY</td></tr>
<tr><td>source-flavor-watermark</td><td>MediaPackageElementFlavor	</td><td>image/work</td><td>The "flavor" of the attachment image to use as a source input.</td><td>EMPTY</td></tr>
<tr><td>source-url-watermark</td><td>URL	</td><td>file:///Users/me/logo.jpg</td><td>The "URL" of the fallback image to use as a source input.</td><td>EMPTY</td></tr>
<tr><td>target-tags</td><td>String , { "," , String }	</td><td>composite,rss,atom,archive</td><td>The tags to apply to the compound video track.</td><td>EMPTY</td></tr>
<tr><td>\* **target-flavor**</td><td>MediaPackageElementFlavor</td><td>	composite/delivery	</td><td>The flavor to apply to the compound video track.</td><td>EMPTY</td></tr>
<tr><td>\* **encoding-profile**</td><td>String</td><td>	composite	</td><td>The encoding profile to use.</td><td>EMPTY</td></tr>
<tr><td>\* **output-resolution**</td><td>width , "x" , height	</td><td>1900x1080</td><td>The resulting resolution of the compound video e.g. 1900x1080.</td><td>EMPTY</td></tr>
<tr><td>output-background</td><td>String	</td><td>red</td><td>The resulting background color of the compound video http://www.ffmpeg.org/ffmpeg-utils.html#Color.</td><td>black</td></tr>
<tr><td>\* **layout**</td><td>name | Json , ";" , Json , [ ";" , Json ]	</td><td>topleft	</td><td>The layout name to use or a semi-colon separated JSON layout definition (lower video, upper video, optional watermark). If a layout name is given than the corresponding layout-{name} key must be defined.</td><td>EMPTY</td></tr>
<tr><td>layout-{name}</td><td>Json , ";" , Json , [ ";" , Json ]	 	</td><td>Define semi-colon separated JSON layouts (lower video, upper video, optional watermark) to provide by name.</td><td>EMPTY</td></tr>
</table>

\* **mandatory**

## Layout Definition

The layout definitions are provided as JSON. Each definition consist of the layout specifications for the lower and upper video and an optional specification for the watermark. The specifications have to be separated by comma.

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

##Operation Example
 
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
        <configuration key="output-resolution">1900x1080</configuration>
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

