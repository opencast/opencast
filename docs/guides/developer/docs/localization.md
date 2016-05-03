Introduction
================================

The Opencast project uses the [Crowdin Localization Management Platform](https://crowdin.com/project/opencast-matterhorn) for translating Opencast into a variety of languages.

Essentially, the English translation (en-US) acts as master language that is translated to other languages using Crowdin.

***Important**: All translations for languages other than English (en-US) are downloaded from Crowdin. Modifications to the translation files in the Opencast code repository will be overwritten and lost!*

Versioning
----------

Crowdin supports versions managment by allowing the management of multiple branches. The relation of Opencast code repository branches to Crowdin branches follows the following convention:

The Opencast branch `r/a.b.x` corresponds to Crowdin branch `a.b.x`.

Working with Crowdin CLI
------------------------

The Crowdin CLI command line tool is used to synchronize the master language files and translations between the Opencast code repository and Crowdin.

The Crowdin CLI configuration can be found in `/.crowdin.json`

***Important**: To be able to access Crodwin using the Crowdin CLI tool, the API keys needs to be added to that configuration file.*

To upload the master translations from the Opencast code repository to Crowdin, use the following command:

    java -jar crowdin-cli.jar --config .crowdin.yaml upload sources -b <branch>

Note that the branch `<branch>` will be automatically created if it is not yet existing.


To download the translations from Crowdin, use the following command:

    java -jar crowdin-cli.jar --config .crowdin.yaml download translation -b <branch>

References
----------

 - [Crowdin Opencast Project](https://crowdin.com/project/opencast-matterhorn)
 - [Crowdin CLI Documentation](https://crowdin.com/page/cli-tool)
 - [Crowdin Versions Management Documentation](https://support.crowdin.com/articles/versions-management/)
 - [Crowdin Language Codes](https://crowdin.com/page/api/language-codes)
