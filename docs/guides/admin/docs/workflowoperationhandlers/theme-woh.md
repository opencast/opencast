# ThemeWorkflowOperationHandler

## Description
The ThemeWorkflowOperation loads workflow properties and adds elements to the media package if available. 
This information can be used within workflow definitions to actually implement themes.

**Bumpers**

The property *theme_bumper_active* indicates whether the theme defines a bumper video. If true, the bumper video
is added to the media package with the flavor *bumper-flavor* and/or tag *bumper-tag*.

**Trailers**

The property *theme_trailer_active* indicates whether the theme defines a trailer video. If true, the trailer video
is added to the media package with the flavor *trailer-flavor* and/or tag *trailer-tag*.

**Title Slide**

The property *theme_title_slide_active* indicates whether the theme defines a title slide. Additionally, the
property *theme_title_slide_uploaded* indicates whether the image needed as background for the generation
of the title slide should be extracted from a video track or has been uploaded. In the later case,
the background image is added to the media package with the flavor *title-slide-flavor* and/or tag *title-slide-tag*.

## Workflow Properties

The ThemeWorkflowOperation will set the following workflow properties:

|Property Name             |Description                                                          |
|--------------------------|---------------------------------------------------------------------|
|theme_active              |true if the theme has active settings, false or undefined otherwise  |
|theme_bumper_active       |true if the theme has an active bumper video, false otherwise        |
|theme_trailer_active      |true if the theme has an active trailer video, false otherwise       |
|theme_title_slide_active  |true if the theme has an active title slide, false otherwise         |
|theme_title_slide_uploaded|true if the theme come with an uploaded title slide, false otherwise |

Note: The property *theme_active* can be used to test whether a theme has any active settings, i.e.
at least one of the properties *theme_\*_active* is true.

## Parameter Table

|Configuration Keys |Example             |Description                    |
|-------------------|--------------------|-------------------------------|
|*bumper-flavor     |branding/bumper     |Flavor of the bumper video     |
|*bumper-tags       |bumper              |Tag of of the bumper video     |
|*trailer-flavor    |branding/trailer    |Flavor of the trailer video    |
|*trailer-tags      |trailer             |Tag of the trailer video       |
|*title-slide-flavor|branding/titleslide |Flavor of the title slide image|
|*title-slide-tag   |titleslide          |Tag of the title slide image   |

\* Mandatory configuration key

## Operation Example

    <operation
      id="theme"
      exception-handler-workflow="ng-partial-error"
      description="Apply the theme">
      <configurations>
        <configuration key="bumper-flavor">branding/bumper</configuration>
        <configuration key="bumper-tags">archive</configuration>
        <configuration key="trailer-flavor">branding/trailer</configuration>
        <configuration key="trailer-tags">archive</configuration>
        <configuration key="title-slide-flavor">branding/titleslide</configuration>
        <configuration key="title-slide-tags">archive</configuration>
      </configurations>
    </operation>


