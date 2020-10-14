Text Extraction Configuration
=============================

How the text extraction process works
-------------------------------------

The sequence of the Opencast services used during slide detection and text extraction is the following:

    -----> Segmentation -----> TextAnalyzerService ----------------->
                                  /             \
                                 /               \
                       TextExtractor          DictionaryService
                    (OCR with Tesseract)   (Filter extracted texts)


The segmentation will define the frames which are passed to the text analyzer. For extraction, a frame from the end of a
segment is used to make sure that most of a slides text is visible.

The frame is then exported as image and passed to the text extraction service which calls an OCR engine to get the text
output. For this, the Tesseract OCR engine is used by default.

After the text extraction is done, the analysis service will pass the recognized text to the dictionary service which
may filter it to remove messed up words, unknown words, single characters or other things depending on the actual
implementation and configuration.

Finally, the extracted text is attached to the media package as MPEG 7 XML and the Opencast workflow continues.



Configuration
-------------

This section describes the configuration of all involved tools and services. In this guide, German is used as target
language but the configuration for other languages should be similar. If necessary, important differences will be
pointed out.


### OCR Engine: Tesseract

Tesseract is the default OCR engine used by Opencast. It will accept an image file and write the extracted text to an
output file. The command line arguments for this will be handled by Opencast. But apart from these mandatory ones, it is
possible to pass additional arguments to Tesseract, defining the internally used dictionary, box files and the layout
analysis.

For example, if you want OCR for German content, you want to run something like this:

    tesseract in.tif out.txt -l deu --psm 3

* The arguments `in.tif` and `out.txt` are automatically set by Opencast.
* The argument `-l` specifies the language files used by Tesseract. `deu` specifies the German language. Multiple
  languages may be specified, separated by plus characters. Please make sure that you have installed the language packs
  you want to use on every worker (E.g. `yum install tesseract-langpack-deu`).
* Finally `--psm 3` specifies the layout analysis for Tesseract. The value `3` means *Fully automatic page segmentation,
  but no orientation and script detection* which is actually the default. Hence in this case, the argument could simply
  be omitted. If you know more about your input videos, you might want to use different options here (not likely).
  This option might be just `-psm` with only one dash in your system.

In Opencast, you can modify this options in the `custom.properties` file by setting the following option:

    org.opencastproject.textanalyzer.tesseract.options=-l deu --psm 3

It is highly recommended to configure Tesseract to use your local language. It will improve the recognition a lot and
only this will enable the recognition of special characters specific to your local language.

If you supply a reference to a list of additional words using `--user-words`, the path to that file must not be enclosed
in quotation marks.

Newer versions of Tesseract come with additional neural nets LSTM. Its performance might be significantly different
from the previous Tesseract engine. Its usage might be specified using `--oem N` with `N` being a number documented in
your Tesseract manual.


### Encoding (Image Preprocessing)

The text extraction works best if there is a high contrast between text and background and additionally, the text is not
too thin. Ideally, this means that you have black and white images with a clear, bold font.

At this point, it is probably worth noting that despite what is often said and could also be found in the old
documentation for Opencast, it does not matter for Tesseract if it is black text on a white background or if the colors
are inverted (white on black). Because of the way Tesseract works, that does not matter.

A lot of lecture slides are unfortunately not designed this way. Lecturers use colors, background images, etc. That is
why, to get a better result, it is a good idea to do some image preprocessing steps. Some easy ones can be included
directly into the image extraction step using FFmpeg.

For this, edit the `/etc/opencast/encoding/opencast-images.properties` and modify the command for the image
extraction:

    profile.text-analysis.http.ffmpeg.command = -ss #{time} -i #{in.video.path} \
      -filter:v boxblur=1:1,curves=all=0.4/0#{space}0.6/1 \
      -frames:v 1 -pix_fmt:v gray -r 1 #{out.dir}/#{out.name}#{out.suffix}

This profile will create a gray, high contrast image. The additional light blur will reduce or remove noise and thicken
the normal letters.

The kind of preprocessing you should use highly depends on the input material. Interesting filters to try out for your
material are among others the blur filters, the denoise filters, the curves filter and in some cases the color-channel
mixer.


### DictionaryService (Filtering)

The filtering you want to do on the recognized texts highly depends on what you want to use the recognized texts for.
For searching, you might want a higher degree of filtering, for users you might also want to present text with slight
errors, for testing and debugging, you want no filtering at all.

Starting with version 1.6, Opencast provides three different kinds of implementation for filtering which can be just
swapped out at any time:

* dictionary-none
* dictionary-regexp (default)
* dictionary-hunspell


#### No Filtering (dictionary-none)

The `dictionary-none` module is the simplest one. It will just let the recognized texts pass through
unmodified. There is no additional configuration needed or even possible. Of course, this is also the fastest one.


#### Using a Regular Expression (dictionary-regexp)

Starting with 1.6, this is the default implementation for the DictionaryService. It is limited in terms of filtering
capabilities as it will not check if a recognized word actually makes sense. Here is how this service works: If
configured with a valid pattern that compiles to a regular expression, then this pattern is used, otherwise a fall-
back to the default expression `\w+`. The pattern is repeatedly applied to the extracted text, until the end is
reached. Each match is returned, separated by a space character.

The default expression for this module is `\w+` which will let upper- and lowercase characters as well as digits pass
through, but will block all other characters. For the German language, for example, this would mean that all special
characters would be blocked as well. So you want to configure Opencast to let them pass as well.

Example:
    * pattern: `\w+`
    * text input: "aäa bbb"
    * text output: "a a bbb"

If this is undesired, modify the `pattern` in
`etc/org.opencastproject.dictionary.regexp.DictionaryServiceImpl.cfg`:

For German, a suitable pattern could be:

    pattern=[\\wäöüÄÖÜß][\\wäöüÄÖÜß]+[-.,:;!?]*

This expression will let all words pass which contain upper- and lowercase [a-z], digits and German special characters
as well as punctuation at the end of a word. Additionally, it requires that the words are at least two characters long
which will filter out most of the common noise. Note the double-escaping of `\w`.

A similar pattern that could be used for Spanish would be:

    pattern=[¿¡(]*[\\wáéíóúÁÉÍÓÚüÜñÑ][\\wáéíóúÁÉÍÓÚüÜñÑ]+[)-.,:;!?]*


#### Using a Spell Checker (dictionary-hunspell)

Last, the `dictionary-hunspell` will check words based on a spell checker and a dictionary. As spell checker,
the tool `hunspell` is used which is one of the most common spell checkers on Linux and should be available from the
system repositories for most common operating systems.

For the Hunspell based DictionaryService, there are two configuration options: One specifies the location of the binary
and one is for the arguments to use for filtering.

By default, Opencast will just call `hunspell` without an absolute path. This will work as long as hunspell is in the
systems path which should be the case unless you have built and installed it manually. In that case, the binary can be
configured using the following option in the `custom.properties` file:

    org.opencastproject.dictionary.hunspell.binary=/usr/bin/hunspell

While most people will not need the binary path configuration, most people will need the filtering option which can be
used for setting the languages. Configuration for this can be done using the following key in the `custom.properties`
file:

    org.opencastproject.dictionary.hunspell.command=-d de_DE,en_GB,en_US -G

Note that equivalent to the Tesseract configuration, again the necessary languages have to be installed in the system.
On RedHat based systems, for German, you would install the `hunspell-de` package from the system repositories.

For Hunspell, you can also create custom dictionaries or add custom words to the existing ones. This might be
interesting for technical terms.
