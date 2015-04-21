# ArchiveWorkflowHandler

## Description
The ArchiveWorkflowHandler will archive the current state of the mediapackage.

## Parameter Table
<table>
<tr><th>configuration keys</th><th>example</th><th>description</th></tr>
<tr><td>source-tags</td><td>text</td><td> Specifies which media should be archived.</td></tr>
<tr><td>source-flavors</td><td>presenter/source	</td><td>Flavors that should be archived, separated by ","</td></tr>
</table>

## Operation Example

    <operation
          if="${archiveOp}"
          id="archive"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Archiving">
          <configurations>
                <configuration key="source-tags">archive</configuration>
          </configurations>
    </operation>

