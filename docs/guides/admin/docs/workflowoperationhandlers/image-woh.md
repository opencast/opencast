# ImageWorkflowOperation

## Description
The ImageWorkflowOperation will extract still images from a video using FFmpeg and a list of given encoding profiles.
Both absolute and relative positions can be used.

##Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------| 
|source-flavor|presenter/source|Specifies which media should be processed.|
|source-flavors|presenter/source, presentation/source|Specifies a list of media which should be processed. In case a flavor has been specified in *source-flavor*, it will be added to this list.|
|source-tags	|engage	|Specifies which media should be processed.|
|target-flavor|presenter/work|Specifies the flavor the new files will get.|
|target-tags	|engage	|Specifies the tags the new files will get.	 |
|encoding-profile	|search-cover.http	|Comma-separated list of encoding profiles to use.	 |
|time	|1	|Comma-separated list of time (in seconds or relative to source track duration) where the image should be taken.	 |
|target-base-name-format-second|thumbnail_%.0f%s|Used to control the target filenames for images extracted at absolute times. Mainly helpful when integrating third-party applications that prefer to use filename to distinguish individual images|
|target-base-name-format-percent|thumbnail_%.3f%s|Used to control the target filenames for images extracted at relative times. Mainly helpful when integrating third-party applications that prefer to use filename to distinguish individual images|
|end-margin|500|Safety margin at the end of the track. Sometimes, image extraction is critical at the end of the file. Using *end-margin* ensures, that no images are being extracted near the end of the video file to avoid problems with defective inputs.</br>(Default: 100)|

Notes:

* Absolute and relative position may be mixed up in the configuration key *time*


## Operation Example

### Basic

Extract one image at position 1 second using the encoding profile *search-cover.http*.

    <operation
          id="image"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Encoding presenter (camera) to search result preview image">
          <configurations>
                <configuration key="source-flavor">presenter/trimmed</configuration>
                <configuration key="source-tags"></configuration>
                <configuration key="target-flavor">presenter/search+preview</configuration>
                <configuration key="target-tags">engage</configuration>
                <configuration key="encoding-profile">search-cover.http</configuration>
                <configuration key="time">1</configuration>
          </configurations>
    </operation>

### Advanced

In this example, we extract images at three relative positions (*1%, 50%, 100%*) from the presenter/trimmed track. For each position, we extract three images using three different encoding profiles (*example.encoding.profile.\**). 
This operation therefore generates nine images in total. The target filenames will be formed based on the *target-base-name-format-\** configuration keys (prefix) and the configuration of the encoding profiles (file extension and possibly suffix).

    <operation id="image"
               description="Extract set of thumbnails"
               fail-on-error="true"
               exception-handler-workflow="partial-error">
      <configurations>
        <configuration key="source-flavor">presenter/trimmed</configuration>
        <configuration key="target-flavor">presenter/thumbnails</configuration>
        <configuration key="target-base-name-format-second">thumbnail_%.3f%s</configuration>
        <configuration key="target-base-name-format-percent">thumbnail_%.0f%s</configuration>
        <configuration key="encoding-profile"> example.encoding.profile.small, example.encoding.profile.medium, example.encoding.profile.large</configuration>
        <configuration key="time">1%, 50%, 100%</configuration>
        <configuration key="end-margin">1000</configuration>
      </configurations>
    </operation>