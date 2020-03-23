Localization
============

Introduction
------------

The Opencast project uses the
[Crowdin Localization Management Platform](https://crowdin.com/project/opencast-community) for translating
Opencast into a variety of languages.

The English translation (en-US) acts as source language that is translated to other languages using Crowdin.
While all translation files are located in the Opencast code repository, only the English translation should
be modified in the code repository - all other translation files are downloaded from Crowdin.

**Important:** *All translation files for languages other than English (en-US) are downloaded from Crowdin.
Modifications to the translation files in the Opencast code repository will be regularly overwritten and
therefore will be lost!*

Note that Crowdin managers take care of uploading the English sources (and possibly translations) to Crowdin and
download the others translations from Crowdin.

I would like Opencast to support my language. Is this possible?
---------------------------------------------------------------

Yes, absolutely! If you are willing to take the effort to provide the translation, we are happy to include your
favorite language in Opencast!

How can I provide a language translation?
-----------------------------------------

We use the [Crowdin Localization Management Platform](https://crowdin.com/project/opencast-community) - an easy to
use web service for localization management. To provide a language translation, please perform the following steps:

1. Create a free account on [Crowdin](https://crowdin.com)
2. Visit the [Opencast project](https://crowdin.com/project/opencast-community) on Crowdin and issue a join request
3. Translate Opencast on Crowdin

Once the translation reaches at least 90% (prefarable 100%), please read the section about include and exclusion
of translations just below.

In case you have questions, we are happy to answer them on the Opencast Users mailing list.

Inclusion and Exclusion of Translations
---------------------------------------

Opencast supports a number of languages right out-of-the-box. Please find the criteria for inclusion and exlusion of
language translations in Opencast releases below:

1.  A not yet supported translation is included into the next major release if it is translated at least 90% at the
    time when the release branch is cut. The release managers will take the review if no other reviewer can be found.

2.  A not yet supported translation may be included in the current release branch anytime if it is translated to 100%
    and a reviewer is found. It will then be part of the next minor release and major release if feasible

3.  An endangered translation is a supported translation that is translated less than 80% at the time when the release
    branch of the next major release is cut. The release managers will publish a list of endangered languages if any

4.  An endangered translation will be removed with the next major release if it is not saved. The release managers take
    care of the removal in case no other person will

5.  An endangered translation may be saved by reaching at least 90% translated until at least two weeks before the
    release date of the next major release and a reviewer is found

Note that [Crowdin](https://crowdin.com/project/opencast-community) is displaying the percentage translated for
each language. It is the percentages shown on that page that act as reference.
Considering the dates when releases branch are cut, the respective releases schedules act as reference.


Crowdin Management And Administration
-------------------------------------

Crowdin managers are persons with privileged access to Crowdin needed to upload new files to be translated to Crowdin.
The rest of document should help future Crowdin managers to get familiar with Crowdin quickly.

Accepting Translators
---------------------

We ask that Crowdin users who wish to help translate Opencast send a brief, understandable sentence regarding why they
wish to help translate Opencast. Users who do not send this in should be asked via the Crowdin messaging system.
Something as simple as 'I want to help translate $project into [language]' would more than suffice.

### Versioning

Crowdin supports versions management by allowing the management of multiple branches. The relation of
Opencast code repository branches to Crowdin branches follows the following convention:

The Opencast branch `r/a.x` corresponds to Crowdin branch `a.x`.

Crowdin does automatically detect equal strings across branches so there is no need to configure anything when
a new branch is created.

Right after a cut of a new Opencast release branch `r/a.x` (from `develop`), the following actions must be performed:

1. Download the translations from Crowdin branch `develop`
2. Commit the downloaded translations into the Opencast branch `r/a.x`
    * Respect the rules described in the section [Inclusion and Exclusion of Translations](#inclusion-and-exclusion-of-translations)
3. Upload translation sources of Opencast branch `r/a.x` to Crowdin branch `a.x`
    * Crowdin will find duplicate strings (added in previous versions) and add the translations to these
    * Only new strings will be shown to translators

**Important:** Do not upload translations to Crowdin!

To keep the Opencast code repository in sync with Crowdin, the translation sources should be uploaded to Crowdin as soon
as possible.

### Working with Crowdin CLI

The Crowdin CLI command line tool is used to synchronize the source language files and
translations between the Opencast code repository and the Crowdin project Opencast.

The Crowdin CLI configuration can be found in `/.crowdin.yaml`

Please perform the following steps to get the tool running on your local host:

1. Install [Crowdin CLI tool](https://support.crowdin.com/cli-tool)
2. [Get the API key for the project Opencast
  ](https://crowdin.com/project/opencast-community/settings#integration)
3. Add the following line to your local Crowdin configuration file (`~/.crowdin.yaml`):
the first line:

        api_key: <secret key>

Now you can use the Crowdin CLI command line tool to upload source language files and download translations.

To upload the translation sources from the Opencast code repository to Crowdin, use the following command:

    crowdin --config .crowdin.yaml upload sources -b <branch>

Note that the branch `<branch>` will be automatically created if it is not yet existing.

To download the translations from Crowdin, use the following command:

    crowdin --config .crowdin.yaml download -b <branch>

To show a list of files in the current project, use the following command:

    crowdin --config .crowdin.yaml list project -b <branch>

Note: Do not upload translations to Crowdin. Our current subscription plan will exceeded otherwise.


### To consider when adding a translation

* Add a SVG with the flag under modules/admin-ui/src/main/webapp/img/lang/
* Add a case to the switch-case in
    * modules/engage-theodul-core/src/main/resources/ui/js/engage/core.js
    * modules/engage-ui/src/main/resources/ui/js/app/engage-ui.js


Further Information
-------------------

* [Crowdin Opencast Project](https://crowdin.com/project/opencast-community)
* [Crowdin CLI Documentation](https://support.crowdin.com/cli-tool/#usage)
* [Crowdin Versions Management Documentation](https://support.crowdin.com/versions-management/)
* [Crowdin Language Codes](https://support.crowdin.com/api/language-codes/)
