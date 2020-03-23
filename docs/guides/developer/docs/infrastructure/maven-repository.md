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


Pushing artifacts to a Maven repository
---------------------------------------

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

#### Pushing to Maven Central

Opencast previously hosted our own Maven repository at nexus.opencast.org, however starting with Opencast 6.6 we are
transitioning to using Maven Central.  There are a few steps prior to being able to push to Sonatype's repo:

- Create a GPG key, and push the public key to a key server
- Sign up for an account on [Sonatype's JIRA instance](https://issues.sonatype.org)
- Let the QA Coordinator know about your user, they will comment on
  [our repo creation ticket](https://issues.sonatype.org/browse/OSSRH-36510) or create a new issue to give your user permissions
- Put the following in your `.m2/settings.xml` file

```xml
    <settings>
      <servers>
        <server>
          <id>ossrh</id>
          <username>$username</username>
          <password>$password</password>
        </server>
        ...
      </servers>
      <profiles>
        <profile>
          <id>ossrh</id>
          <activation>
            <activeByDefault>true</activeByDefault>
          </activation>
          <properties>
            <gpg.keyname>$gpgKeyId</gpg.keyname>
          </properties>
        </profile>
        ...
      </profiles>
    </settings>
```

##### Pushing Snapshots

Snapshots are pushed automatically by the CI servers.  For historical purposes, this is accomplished by:

```bash
mvn deploy
```

To verify, your artifacts can be found [here](https://oss.sonatype.org/content/repositories/snapshots/org/opencastproject/)
and [here](https://oss.sonatype.org/content/groups/staging/org/opencastproject/).  Note that you cannot (easily) drop
bad snapshots.  Instead, fix it and redeploy!

##### Pushing Releases

Note: Please read this section entirely before running any commands.  Maven Central does not allow you to change a
release once it has been closed!

Pushing releases is similar to snapshots, with the added requirements that you also push:

- Javadocs
- Sources
- GPG signatures for the binaries, docs, and sources

This is automated with the `release` profile.  To push a release run

```bash
mvn nexus-staging:deploy -P release
```

This creates a staging repository (https://oss.sonatype.org/content/groups/staging/org/opencastproject/) for your
artifacts.  This is always safe to do - you can still rollback all changes with

```bash
mvn nexus-staging:drop
```

If things do not look ok, fix the issue and redeploy.  Once you are confident that everything is ok, you can run

```bash
mvn nexus-staging:close
```

This closes the staging repository, and runs the Sonatype-side tests for things like GPG signatures.  If this fails,
correct the issue locally, and redeploy.  Once this succeeds, you have two options: drop (to destroy the release) or:

```bash
mvn nexus-staging:release
```

to permanently release the binaries in their current states.

##### Troubleshooting

Sometimes the deploy or close will fail, timing out after 5 minutes waiting for Sonatype.  It will complain about
violations of deploy rules - this may or may not actually be true.  If you're confident that this is caused by a simple
timeout and not something you have done use one of the following.

To reattempt a deploy use

```bash
mvn nexus-staging:deploy-staged
```

This will avoid recompiling, retesting, and resigning all of the binaries.


To reattempt a close use

```bash
mvn nexus-staging:close
```
