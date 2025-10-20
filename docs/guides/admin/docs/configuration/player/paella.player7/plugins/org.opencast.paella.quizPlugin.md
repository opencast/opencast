Paella plugin: org.opencast.paella.quizPlugin
=======================================================

This plugin displays quizzes in the player, based on a JSON catalog in the event tracks.  
A quiz consist of a single question which can have multiple correct answers.

The expected flavor type of the catalog is "quizzes" (i.e. "quizzes/source").

The file is of the form:
```json
{
  "start": 2,
  "question": "Which is a fruit?",
  "answers": [
    {
      "text": "Banana",
      "correct": true
    },
    {
      "text": "Cucumber",
      "correct": false
    },
    {
      "text": "Tomato",
      "correct": true
    }
  ]
}

```

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enable the `org.opencast.paella.quizPlugin` plugin.

```json
{
    "org.opencast.paella.quizPlugin": {
        "enabled": true
    }    
}
```
