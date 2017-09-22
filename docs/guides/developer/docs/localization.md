Introduction
============

The Opencast project uses the
[Crowdin Localization Management Platform](https://crowdin.com/project/opencast-community) for translating
Opencast into a variety of languages.

The English translation (en-US) acts as source language that is translated to other languages using Crowdin.
While all translation files are located in the Opencast code repository, only the English translation should
be modified in the code repository - all other translation files are downloaded from Crowdin.

**Important:** *All translation files for languages other than English (en-US) are downloaded from Crowdin.
Modifications to the translation files in the Opencast code repository will be regularly overwritten and
therefore will be lost!*

Note that Crowdin managers take care of uploading the English sources (and possibly translations) to Crowdin and download the
others translations from Crowdin.

Crowdin Managers
================

Crowdin managers are persons with privileged access to Crowdin needed to upload new files to be translated to Crowdin.
The rest of document should help future Crowdin managers to get familiar with Crowdin quickly.

Versioning
----------

Crowdin supports versions management by allowing the management of multiple branches. The relation of
Opencast code repository branches to Crowdin branches follows the following convention:

The Opencast branch `r/a.b.x` corresponds to Crowdin branch `a.b.x`.

Crowdin does automatically detect equal strings across branches so there is no need to configure anything when
a new branch is created.

When releasing a new version `a.b.c` of Opencast, the following actions must be performed to keep the Opencast code
repository in sync with Crowdin:

1. Download the translations from Crowdin branch `a.b.x`
2. Commit the downloaded translations into the Opencast branch `r/a.b.x`
3. Release Opencast `a.b.c`
4. Merge Opencast branch `r/a.b.x` into Opencast branch `develop`
5. Upload sources of Opencast branch `develop` to Crowdin branch `develop`
6. Upload translations of Opencast branch `develop` to Crodwin branch `develop`

Working with Crowdin CLI
------------------------

The Crowdin CLI command line tool is used to synchronize the source language files and
translations between the Opencast code repository and the Crowdin project *Opencast*. 

The Crowdin CLI configuration can be found in `/.crowdin.yaml`

Please perform the following steps to get the tool running on your local host:

1. Download the [Crowdin CLI tool](https://crowdin.com/page/cli-tool)
2. Get the API key for the project *Opencast* 
[here](https://crowdin.com/project/opencast-community/settings#integration)
3. Add the following line to the Crowdin configuration file found in the Opencast code repository just after
the first line:

        api_key: <secret key>

Now you can use the Crowdin CLI command line tool to upload source language files and download translations.

To upload the sources from the Opencast code repository to Crowdin, use the following command:

    java -jar crowdin-cli.jar --config .crowdin.yaml upload sources -b <branch>

In case you need to upload the translations from the Opencast code repository branch `<branch>`, use the command:

    java -jar crowdin-cli.jar --config .crowdin.yaml upload translations -b <branch>

Note that the branch `<branch>` will be automatically created if it is not yet existing.

To download the translations from Crowdin, use the following command:

    java -jar crowdin-cli.jar --config .crowdin.yaml download -b <branch>

References
----------

 - [Crowdin Opencast Project](https://crowdin.com/project/opencast-community)
 - [Crowdin CLI Documentation](https://crowdin.com/page/cli-tool)
 - [Crowdin Versions Management Documentation](https://support.crowdin.com/articles/versions-management/)
 - [Crowdin Language Codes](https://crowdin.com/page/api/language-codes)
