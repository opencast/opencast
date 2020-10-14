# Image-Convert Workflow Operation

## Description
The Image-Convert workflow operation allows source images to be converted into target images with different encoding
settings. One example is the conversion of preview images into different formats.

This operation expects an attachment as input and creates one attachment as output.


## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-tags*|preview+player,preview+search|A comma separated list of source image(s) tags.|EMPTY|
|source-flavors*|*/image|A comma separated list of source image(s) flavors.|EMPTY|
|tags-and-flavors|false|An boolean value whether to select elements with tags and flavors (then set this option to true) or either tags or flavors (then set this option to false).|false|
|target-tags|+preview-converted,-preview+player|Apply these (comma separated) tags to the output attachments. If a target-tag starts with a '-', it will be removed from preexisting tags, if a target-tag starts with a '+', it will be added to preexisting tags. If there is no prefix, all preexisting tags are removed and replaced by the target-tags.|EMPTY|
|target-flavor*|*/image+converted|Apply these flavor to the output attachments.|EMPTY|
|encoding-profiles*|jpeg-player,jpeg-search|A comma separated list of encoding profiles to be applied to each input image.|EMPTY|

\* mandatory configuration key

Note: At least `source-tags` or `source-flavors` parameter must be set.

## Operation Example

This operation would convert all image attachments with flavor matches `*/preview` and tag `player` into two different
formats described by the encoding profiles `preview-regular.image` and `preview-small.image`.
The produced image attachments will have an flavor with the subtype `preview+player`.

    <operation
      id="image-convert"
      exception-handler-workflow="error"
      description="Resize images to fixed size">
      <configurations>
        <configuration key="source-tags">player</configuration>
        <configuration key="source-flavors">*/preview</configuration>
        <configuration key="tags-and-flavors">true</configuration>
        <configuration key="target-tags"></configuration>
        <configuration key="target-flavor">*/preview+player</configuration>
        <configuration key="encoding-profiles">preview-regular.image,preview-small.image</configuration>
      </configurations>
    </operation>

### Encoding Profile Example

    # Player preview image regular size
    profile.preview-regular.image.name = player preview image regular size
    profile.preview-regular.image.input = image
    profile.preview-regular.image.output = image
    profile.preview-regular.image.suffix = -preview-regular.jpg
    profile.preview-regular.image.mimetype = image/jpeg
    profile.preview-regular.image.ffmpeg.command = -i #{in.video.path} -vf scale=480:-2 #{out.dir}/#{out.name}#{out.suffix}
