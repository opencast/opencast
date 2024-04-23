# Imprint and Privacy Statement

Opencast allows you to provide imprint and a privacy statements for the admin-ui. Both documents are linked in the footer.

Simply edit the default files in `/etc/ui-config/mh_default_org/admin-ui/`. The filenames follow these rules:

```
imprint.%{language_code}.html
privacy.%{language_code}.html
```

The corresponding language codes can be found here: https://github.com/opencast/opencast-admin-interface/blob/admin-ui-picard/app/src/i18n/i18n.ts#L44

If a language is not provided, the english version will be displayed.

To display both links in the footer, set the following in `etc/org.opencastproject.organization-mh_default_org.cfg`:

```
prop.org.opencastproject.admin.display_about=true
```