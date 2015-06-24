Text Extraction Configuration
=============================

How the text extraction process works
-------------------------------------

The sequence of the Matterhorn services used during slide detection and text extraction is the following:

```
  -----> Segmentation -----> TextAnalyzerService ----------------->
                                /             \
                               /               \
                     TextExtractor          DictionaryService
                  (OCR with Tesseract)   (Filter extracted texts)
```


The segmentation will define the frames which are passed to the text analyzer. For extraction a frame from the end of a
segment is used to make sure that most of a slides text is visible.

The frame is then exported as TIFF image and passed to the text extraction service which calles an OCR engine to get the
text output. For this, the Tesseract OCR engine is used by default.

After the text extraction is done, the analysis service will pass the recognized text to the dictionary service which
may filter it to remove messed up words, unknown words, single characters or other things depending on the actual
implementation and configuration.

Finally, the the extracted text is attached to the Mediapackage as MPEG 7 XML and the Matterhorn workflow continues.



Configuration
-------------

This section describes the configuration of all involved tools and services. As this guide the configuration is for the
German language but the configuration for other languages should be equivalent and if not obvious, the differences will
be pointed out.


### OCR Engine: Tesseract

Tesseract is the default OCR engine used by Matterhorn. It will accept an image file and write the extracted text to an
output file. The command line arguments for this will be handles by Matterhorn. But apart from this, it is possible to
pass additional arguments to tesseract defining the internally used dictionary, box files and the layout analysis.

For example, for OCR on slides with German language, you want to run something like this:
```
   tesseract in.tif out.txt -l deu -psm 3
```

 - The arguments `in.tif` and `out.txt` are automatically set by Matterhorn.
 - The argument `-l deu` will specify the language files used by tesseract.  This time `deu` is used for German
   language. Multiple languages may be specified, separated by plus characters. Please make sure that you have
  installed the language pack you want to use. Using yum, this can be done by running something like `yum install
  tesseract-langpack-deu`.
 - Finally `-psm 3` will specify the layout analysis tesseract will do. The value `3` means *Fully automatic page
   segmentation, but no orientation and script detection* which is actually the default. Hence in this case, the
  argument could simply be omitted. If you know more about this input videos, you might want to use different options
  here (not likely).

In Matterhorn you can modify this options in the config.properties file setting the following option:
```
   org.opencastproject.textanalyzer.tesseract.options=-l deu -psm 3
```

It is highly recommended to configure Tesseract to use your local language. It will improve the recognition a lot and
only this will enable the recognition of special characters specific to your local language.


### Encoding (Image Preprocessing)

The text extraction works best if there is a high contrast between text and background and additionally, the text is not
too thin. Ideally, this means that you have black and white images.

At this point it is probably worth noting that despite what is often said and could also be found in the documentation
for Matterhorn, it does not matter for Tesseract if it is black text on a white background or if the colors are inverted
(white on black). Because of the way Tesseract works, that does not matter.

A lot of lecture slides are unfortunately not designed this way. Lecturers use colors, background images, etc. That is
why, to get a better result, it is a good idea to do some image preprocessing steps. Some easy ones can be included
directly into the image extraction step using FFmpeg.

For this, edit the `/etc/matterhorn/encoding/matterhorn-images.properties` and modify the command for the image
extraction:
```
   profile.text-analysis.http.ffmpeg.command = -ss #{time} -i #{in.video.path}
	   -filter:v boxblur=1:1,curves=all=0.4/0#{space}0.6/1
      -frames:v 1 -pix_fmt:v gray -r 1 #{out.dir}/#{out.name}#{out.suffix}
```

This profile would, for example, create a gray, high contrast image. The additional light blur will reduce or remove
noise and thicken the normal letters.

The kind of preprocessing you should use highly depends on the input material. Interesting filters to try out for your
material are among others the blur filters, the denoise filters, the curves filter and in some cases the color-channel
mixer.


### DictionaryService (Filtering)

The filtering you want to do on the recognized texts highly depends on what you want to use the recognized texts for.
For searching, you might want a higher degree of filtering, for users you might also want to present text with slight
errors, for testing and debugging, you want no filtering at all.

Starting with version 1.6, Matterhorn provides three different kinds of implementation for filtering which can be just
swapped out at any time:

 - matterhorn-dictionary-none
 - matterhorn-dictionary-regexp (default)
 - matterhorn-dictionary-hunspell


#### No Filtering (matterhorn-dictionary-none)

The `matterhorn-dictionary-none` module is the simplest one. It will just let the recognized texts pass through
unmodified. There is no additional configuration needed or even possible. Of course, this is also the fastest one.


#### Using a Regular Expression (matterhorn-dictionary-regexp)

Starting with 1.6, this is the default implementation for the DictionaryService. It is quite fast and easy to configure,
but is limited in terms of filtering capabilities as it will in most cases not check it a word makes sense or even if it
makes sense in this context.

The default expression for this module is `\w+` which will let upper- and lowercase characters as well as digits pass
through, but will block all other characters. For the German language for example, this would mean that all special
characters would be blocked as well. So you want to configure Matterhorn to let them pass as well.

You can do that by modifying the `pattern` in
`etc/services/org.opencastproject.dictionary.regexp.DictionaryServiceImpl.properties`:

For German, a suitable pattern could be:
```
   pattern=[\\wäöüÄÖÜß][\\wäöüÄÖÜß]+[-.,:;!?]*
```

This will for example let all words pass which contain upper- and lowercase [a-z], digits and German special characters
as well as punctuation at the end of a words. Additionally, it is required that the words are at least two characters
long which will filter out most of the common noise.

A similar pattern that could be used for Spanish would be:
```
   pattern=[¿¡(]*[\\wáéíóúÁÉÍÓÚüÜñÑ][\\wáéíóúÁÉÍÓÚüÜñÑ]+[)-.,:;!?]*
```


#### Using a Spell Checker (matterhorn-dictionary-hunspell)

Last, the `matterhorn-dictionary-hunspell` will check words based on a spell checker and a dictionary. As spell checker,
the tool `hunspell` is used which is one of the most common spell checkers on Linux and should be available from the
system repositories for most common operating systems.

For the Hunspell based DictionaryService, there are two configuration options.  One is for the binary and one for the
arguments to use for filtering.

By default Matterhorn will just call `hunspell` without an absolute path. This will work as long as hunspell is in the
systems path which should be the case unless you have built and installed it manually. In that case, the binary can be
configured using the following option in the `config.properties` file:
```
   org.opencastproject.dictionary.hunspell.binary=/usr/bin/hunspell
```

While most people wont need the binary path configuration, most people will need the filtering option which can be used
for setting the languages.  Configuration for this can be done using the following key in the `config.properties` file:
```
   org.opencastproject.dictionary.hunspell.command=-d de_DE,en_GB,en_US -G
```

Note that equivalent to the tesseract configuration, again the necessary languages have to be installed in the system.
For German, you would on RedHat based systems for example install the `hunspell-de` package from the system
repositories.

For Hunspell, you can also create custom dictionaries or add custom words to the existing ones. This might be
interesting for technical terms.


Getting Matterhorn with Specific Implementations
------------------------------------------------


### Building Matterhorn from Source

Starting with 1.6, a default build of Matterhorn will build Matterhorn with text extraction and the RegExp based
DictionaryService implementation. But replacing this with another implementation is not difficult. There is an
alternatives profile in that main pom.xml which can be used, but it is probably easier, to build the desired module
directly.

As an example, lets say that you want to replace the default RegExp based DictionaryService with the Hunspell based one.
First of all, you would simply build Matterhorn the same way you always do running:
```
   mvn clean install -Ddeplayto=/some/path/
```

This means that you would end up with all Matterhorn modules in the directory:
```
   /some/path/lib/matterhorn/
```

This includes the `matterhorn-dictionary-regexp-X.Y.Z.jar` which we want to replace. Thus you can simply delete the file
running:
```
   rm /some/path/lib/matterhorn/matterhorn-dictionary-regexp-*.jar
```

Now switch to the `modules/matterhorn-dictionary-hunspell` subdirectory of your Matterhorn source code and run:
```
   mvn clean install -Ddeplayto=/some/path/
```

This will build the curremt module only and will put the resulting JAR file in the target directory where all your other
JARs already are.

Apart from the configuration descriped above, you are now ready to go.


### Installing Specific Implementations from the RPM Repository

If you do not have an advanced knowledge of the structure of Matterhorn and the way RPM packages work, what you want to
do is basically the same thing you would so when building Matterhorn from source: First install a default installation,
then replace the module we want to replace.

Thus we start by installing Matterhorn the way we always do by running:
```
   yum install opencast-matterhorn16
```

This will install Matterhorn with all its dependencies, including all necessary modules (especially the RegExp based
DictionaryService). As we don't want this particular module, we will just remove it again by running:
```
   yum remove opencast-matterhorn16-module-dictionary-regexp
```

You will notice that yum wants to remove a profile and distribution package as well. Do not worry about that, that is
the way it should work. The profile and distribution packages do nothing except for making sure that a given set of
module-packages are installed. As you removed one, this set is not given anymore and yum will remove these metapackages
as well.

So we now have a system with one missing module: The DictionaryService implementation. For this we now choose another
one and install it using:
```
   yum install opencast-matterhorn16-module-dictionary-hunspell
```

That is it.
