Transfer Metadata Workflow Operation
====================================

Description
-----------

The transfer metadata operation allows to transfer arbitrary metadata fields from one metadata catalog to another one.

Parameter Table
---------------

|configuration key|example                           |description
|-----------------|----------------------------------|-------------------------------
|source-flavor    |dublincore/episode                |The catalog from which the metadata is copied
|target-flavor    |myterms/episode                   |The catalog to which the data is copied
|source-element   |{http://purl.org/dc/terms/}creator|The XML element to copy
|target-element   |{http://purl.org/dc/terms/}creator|The XML element to which the values are copied
|force            |false                             |Overwrite existing targets
|concat           |,                                 |Join multiple values by set delimiter
|target-prefix    |dcterms                           |Prefix to use for the given namespace


### force

By default, the operation will fail if a target element already exists at the specified location. If `force` is set, all
existing target elements will be removed before copying the new elements.


### concat

If multiple source elements are selected (e.g. the title in multiple languages), by default, all elements are copied to
the destination. The language information are preserved in this operation. If `concat` is defined, the value of this
option will be used as a delimiter for joining all selected source elements and only ever one element will be written.
All language information for this combined element will be discarded in the process.


### target-prefix

This option lets you specify the XML namespace identifier for the target elements namespace. For example, Opencast
usually uses `dcterms` for elements from the set of DublinCore terms, resulting in elements like `<dcterms:creator>`.


Operation Example
-----------------

```xml
<operation
  id="transfer-metadata"
  description="Transfer dcterms:creator to myterms:owner">
  <configurations>
    <configuration key="source-flavor">dublincore/episode</configuration>
    <configuration key="target-flavor">myterms/episode</configuration>
    <configuration key="source-element">{http://purl.org/dc/terms/}creator</configuration>
    <configuration key="target-element">{http://my-institution.edu/metadata}owner</configuration>
    <configuration key="force">true</configuration>
  </configurations>
</operation>
```
