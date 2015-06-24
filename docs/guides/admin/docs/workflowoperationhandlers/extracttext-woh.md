# ExtractTextWorkflowOperation

## Description
The ExtractTextWorkflowOperation will try to extract test from a video using Tesseract OCR.
## Parameter Table

|configuration keys|example|description|
|------------------|-------|-----------|
|source-flavor|presentation/work|Specifies which media should be processed.|
|source-tags	|text	 |Specifies which media should be processed.	 |
|target-tags 	|engage  |Specifies the tags for the produces media. 	 |

## Operation Example

    <operation
          id="extract-text"
          fail-on-error="false"
          exception-handler-workflow="error"
          description="Extracting text from presentation segments">
          <configurations>
                <configuration key="source-flavor">presentation/trimmed</configuration>
                <configuration key="source-tags"></configuration>
                <configuration key="target-tags">engage,archive</configuration>
          </configurations>
    </operation>
 
