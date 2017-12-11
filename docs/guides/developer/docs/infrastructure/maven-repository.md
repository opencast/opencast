Opencast Maven Repository
=========================

The Maven repository server maintains a copy of all the Java dependencies used by Opencast.


Adding Libraries To The Repository
----------------------------------

1. Login as an administrator on the [Opencast Nexus Master](https://nexus.opencast.org)
2. Select repository
3. Select the artifact upload tab
4. Fill in the details and upload the file


Setting-up Another Maven Repository
-----------------------------------

Having a repository server run in your local network can significantly improve the speed artifacts are retrieved while
building Opencast.


### Docker

There is a preconfigured Docker image for a Nexus server set-up for Opencast. To run an Opencast Nexus using Docker,
follow these steps:

    docker run \
        --name mvncache \
        -p 8000:8000 \
        docker.io/lkiesow/opencast-maven-repository

- The `-p` option will map the internal port of the server in Docker to the port on the host machine.


Prefer a Specific Repository
----------------------------

If you did set-up a local repository or just want to select a specific global repository by default, you can use a
custom Maven configuration. To do that, create asettings file in `~/.m2/settings.xml` like this:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
  http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>opencast-osna</id>
      <name>Osnabrück Opencast Repository</name>
      <url>https://nexus.opencast.org/nexus/content/groups/public</url>
      <mirrorOf>opencast</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

This example would add a mirror for the primary Opencast Maven repository, causing the Osnabrück repository to be the
preferred repository to use. You can find some example configurations in `docs/maven/`.


Pushing artifacts to Maven
--------------------------

#### Pushing to your local Maven repository

The following command will add a file to your local Maven repository.  This is useful for testing if your artifacts are
correctly placed prior to pushing to the mainline Nexus repository.

    mvn install:install-file \
     -Dfile=$filename \
     -DgroupId=$groupId \
     -DartifactId=$artifactId \
     -Dpackaging=$packaging \
     -Dversion=$version \
     -DgeneratePom=$generatePom

Variable Map

Variable    | What it does                                                              | Example
------------|---------------------------------------------------------------------------|--------------------
filename    | The path to the local file you want in your repository                    | audio.mp2
groupId     | The Opencast group ID                                                     | org.opencastproject
artifactId  | The artifact ID. This is the name of the artifact according to Maven      | audio
packaging   | The file type (effectively), this should match the filename's extension   | mp2
version     | The artifact's version                                                    | 1.1
generatePom | Whether or not to generate a pom file automatically                       | true

#### Pushing to the remote Nexus repository

The following command will push a file to the remote Nexus repository.  Normally builds are pushed to to the remote
automatically as part of the CI server build, however if there is a need to push to the repo this is the command you
need. To deploy to the remote repository you will first need a username and password. This can be obtained from the QA
coordinator. Once you have that, put them in your `.m2/settings.xml` file. It should look like this

    <settings>
      <servers>
        <server>
          <id>opencast</id>
          <username>$username</username>
          <password>$password</password>
        </server>
        ...
      </servers>
      ....
    </settings>

The command to push the file looks like this. Not that pushing files from your local Maven repository directly is not
possible, instead you must copy them *outside* the repository and push from there. See below for help on that.

    mvn deploy:deploy-file \
      -DrepositoryId=$repo_id \
      -Durl=$url \
      -Dfile=$filename \
      -DgroupId=$groupId \
      -DartifactId=$artifactId \
      -Dpackaging=$packaging \
      -Dversion=$version \
      -DgeneratePom=$generatePom

Variable Map

Variable    | What it does                                                               | Example
------------|----------------------------------------------------------------------------|--------------------
repo\_id    | Identifies which set of credentials from your .m2/settings.xml file to use | opencast
url         | Where to push the file                                                     | http://nexus.virtuos.uos.de:8081/nexus/content/repositories/snapshots
filename    | The path to the local file you want in your repository                     | audio\_out.mp2
groupId     | The Opencast group ID                                                      | org.opencastproject
artifactId  | The artifact ID. This is the name of the artifact according to Maven       | audio
packaging   | The file type (effectively), this should match the filename's extension    | mp2
version     | The artifact's version                                                     | 1.1
generatePom | Whether or not to generate a pom file automatically                        | true

#### Help with push to the remote Nexus repository

Uploading to Nexus is more difficult than it should be: You can't just run deploy:deploy-file. This script is handy
when you need to manually upload something like a previous release.  Make a copy of
`~/.m2/repository/org/opencastproject/*` to `$SOURCE_FILES` inside of your git clone, check out the version you're
uploading, then run this script.  There will be numerous errors as it processes things that either don't have
artifacts, or don't have artifacts in the version you're uploading, but those can be ignored.

    #!/bin/bash

    CORE_NEXUS="nexus.virtuos.uos.de:8081"
    SOURCE_FILES="nexus_copy"

    uploadVersion() {
      ls $1 | while read line
      do
        mvn deploy:deploy-file \
          -DrepositoryId=opencast \
          -Durl=http://$CORE_NEXUS/nexus/content/repositories/releases \
          -Dfile=$SOURCE_FILES/$line/$2/$line-$2.jar \
          -DgroupId=org.opencastproject -DartifactId=$line \
          -Dversion=$2 \
          -DgeneratePom=true \
          -Dpackaging=jar
      done
    }

    git checkout <VERSION>
    uploadVersion ~/.m2/repository/org/opencastproject <VERSION>
