# SeriesWorkflowOperationHandler

## Description
The SeriesWorkflowOperation will apply a series to the mediapackage.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|series|"0d06537e-09d3-420c-8314-a21e45c5d032"|The optional series identifier. If empty the current series of the medipackage will be taken.|EMPTY|
|attach|"creativecommons/\*,dublincore/\*"|The flavors of the series catalogs to attach to the mediapackage.|EMPTY|
|apply-acl|"true"|"false"|Whether the ACL should be applied or not.|"false"|
|copy-metadata|{http://purl.org/dc/terms/}title, isPartOf|A comma-separated list of metadata fields (possibly "expanded") to be transferred from the series catalog to the episode catalog if they do not exist in the latter.|EMPTY|
|default-namespace|http://purl.org/dc/elements/1.1/|The default namespace to use when the metadata fields in the `copy-metadata` property are not fully "expanded".|http://purl.org/dc/terms/ (the DublinCore namespace)|

## About *Expanded* names

*Expanded* names are qualified XML terms where the prefix has been "expanded" to the full Namespace it represents. For convention, they are written as:

    {namespace}localname

, where `namespace` is the full namespace (**not a prefix like in XML documents**) and `localname` is the term itself.
Some examples of expanded names are:

* `{http://purl.org/dc/terms/}title`
* `{http://mediapackage.opencastproject.org}mediapackage`
* `{}term-with-an-empty-namespace`

The value of the `copy-metadata` may contain expanded and non-expanded names. In the latter case, the names will be
expanded using the provided Namespace, if any, or the DublinCore Namespace by default.

Please note that:

1. An empty Namespace (such as in `{}example`) **is still a Namespace**. That means that the default Namespace will not
be substituted in this case and the term will be handled "as-is", i.e. with an empty Namespace.
2. Most of the terms used by Opencast belong to the DublinCore Namespace, so using non-expanded names and the default
Namespace should be sufficient. However, custom metadata fields may be in a different Namespace which must be
explicitly specified.

## Allowed Namespaces

The Opencast modules that generate XML need to register a mapping between a Namespace and its prefix (e.g. the elements
in the Namespace `http:example.com` are always prefixed with `foo:`), otherwise an exception will be thrown when the
XML document is serialized. 

Implementing a mechanism to map custom Namespaces and prefixes in this operation seems like an overkill. Instead, three
Namespaces are internally registered, and, therefore, they are the only ones available to use, either in expanded terms
or as default Namespace.

The allowed Namespaces are:

* Default DublinCore Namespace: `http://purl.org/dc/terms/`
* DublinCore's "Elements 1.1" Namespace: `http://purl.org/dc/elements/1.1/`
* Opencast Properties' Namespace: `http://www.opencastproject.org/matterhorn/`

## Operation Examples

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

    <operation
          id="series"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Applying series to mediapackage">
          <configurations>
            <configuration key="attach">*</configuration>
            <configuration key="apply-acl">false</configuration>
            <configuration key="copy-metadata">contributor license</configuration>
          </configurations>
    </operation>

    <operation
          id="series"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Applying series to mediapackage">
          <configurations>
            <configuration key="attach">dublincore/*</configuration>
            <configuration key="apply-acl">false</configuration>
            <configuration key="copy-metadata">{http://purl.org/dc/terms/}contributor custom1 custom2</configuration>
            <configuration key="default-namespace">http://www.opencastproject.org/matterhorn/</configuration>
          </configurations>
    </operation>
