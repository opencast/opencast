SeriesWorkflowOperationHandler
==============================

Description
-----------

The SeriesWorkflowOperation will apply a series to the mediapackage.


Parameter Table
---------------

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|series            |`0d06537e-09d3-420c-8314-a21e45c5d032`      |The optional series identifier. If empty the current series of the medipackage will be taken.||
|attach            |`creativecommons/*,dublincore/*`            |The flavors of the series catalogs to attach to the mediapackage.||
|apply-acl         |`true`                                      |Whether the ACL should be applied or not.|`false`|
|copy-metadata     |`{http://purl.org/dc/terms/}title, isPartOf`|A comma-separated list of metadata fields (possibly "expanded") to be transferred from the series catalog to the episode catalog if they do not exist in the latter.||
|default-namespace |http://purl.org/dc/elements/1.1/|The default namespace to use when the metadata fields in the `copy-metadata` property are not fully "expanded".|`http://purl.org/dc/terms/` (DublinCore Term namespace)|


### About Expanded Names

Expanded names are qualified XML terms where the prefix has been expanded to the full namespace it represents. For
convention, they are written as:

    {namespace}localname

… where `namespace` is the full namespace (not a prefix like in XML documents) and `localname` is the term itself.

Some examples of expanded names are:

* `{http://purl.org/dc/terms/}title`
* `{http://mediapackage.opencastproject.org}mediapackage`
* `{}term-with-an-empty-namespace`

The value of the `copy-metadata` may contain expanded and non-expanded names. In the latter case, the names will be
expanded using the provided namespace, if any, or the DublinCore namespace by default.

Please note that:

1. An empty namespace (such as in `{}example`) is still a namespace. That means that the default namespace will not be
   substituted in this case and the term will be handled "as-is", i.e. with an empty namespace.
2. Most of the terms used by Opencast belong to the DublinCore namespace, so using non-expanded names and the default
   namespace should be sufficient. However, custom metadata fields may be in a different namespace which must be
   explicitly specified.


Allowed Namespaces
------------------

For technical reasons, namespaces need to be pre-registered in Opencast to be used. That is why only a defined set of
namespaces can be used in this operation. The allowed namespaces are:

* DublinCore Terms: `http://purl.org/dc/terms/`
* DublinCore Elements 1.1: `http://purl.org/dc/elements/1.1/`
* Opencast Properties: `http://www.opencastproject.org/`


Operation Examples
------------------

```XML
<operation
  id="series"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Applying series to mediapackage">
  <configurations>
    <configuration key="series">0d06537e-09d3-420c-8314-a21e45c5d032</configuration>
    <configuration key="attach">*</configuration>
    <configuration key="apply-acl">true</configuration>
  </configurations>
</operation>
```

```XML
<operation
  id="series"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Applying series to mediapackage">
  <configurations>
    <configuration key="attach">*</configuration>
    <configuration key="apply-acl">false</configuration>
    <configuration key="copy-metadata">contributor, license</configuration>
  </configurations>
</operation>
```

```XML
<operation
  id="series"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Applying series to mediapackage">
  <configurations>
    <configuration key="attach">dublincore/*</configuration>
    <configuration key="apply-acl">false</configuration>
    <configuration key="copy-metadata">{http://purl.org/dc/terms/}contributor custom1 custom2</configuration>
    <configuration key="default-namespace">http://www.opencastproject.org/</configuration>
  </configurations>
</operation>
```
