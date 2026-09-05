# Opencast — notes for Claude

## Building

Always use the Maven wrapper from the repository root — `./mvnw`, never a system `mvn`. It pins the
Maven version the project expects, and `${opencast.basedir}` (used e.g. for the Checkstyle config path)
resolves correctly only from the root. Target individual modules with `-pl` instead of changing
directory:

```
./mvnw -pl modules/<module> <goal>
```

## Running a development Opencast (build → dependencies → start → test)

Recipe for getting an actually running Opencast to test against. All of it is verified to work.

### 1. Build the distribution

Full build (needed at least once, and whenever many modules changed):

```
./mvnw clean install -Pdev -DskipTests -Dcheckstyle.skip=true
```

`-Pdev` is the profile that produces a development distribution.

If everything has been built before and only a single module was modified and rebuilt, re-assembling is
enough — no need to rebuild the world:

```
cd assemblies && ../mvnw clean install -Pdev
```

Either way the result is a distribution directory:

```
./build/opencast-dist-develop-<version>-SNAPSHOT/
```

### 2. Start the runtime dependencies (containers)

Compose files live in `docs/scripts/devel-dependency-containers/`. Use `docker compose` or
`podman compose` / `podman-compose`, whichever is installed — check with `which podman docker`.

First tear down anything left over. Always use `docker-compose-all-sql.yml` for this: it lists every
service, so it also removes containers started from the more complex compose files.

```
cd docs/scripts/devel-dependency-containers
podman compose -f docker-compose-all-sql.yml down --timeout 0
```

`Error: no container with name or ID "…_mariadb_1" found` and friends are expected here — those
services were simply never started.

Then bring up the actual runtime dependency, OpenSearch:

```
podman compose -f docker-compose.yml up -d
```

The other files (`docker-compose-mariadb.yml`, `docker-compose-postgresql.yml`,
`docker-compose-all-sql.yml`) add databases and are only needed when explicitly testing against those;
`docker-compose.yml` is the normal case. No data is persisted — every start is a clean system.

Sanity check: `curl http://127.0.0.1:9200` returns the OpenSearch version JSON.

### 3. Start Opencast

**First check whether something is already listening on port 8080:**

```
ss -tulpen sport :8080
```

If there is, do not start a second Opencast and do not kill the running one — it may well be a
long-lived instance the user is working with. Ask the user how to proceed (reuse it, or have them stop
it). Only start Opencast yourself if the port is free or you started that instance in this session.

Everything from here happens inside the distribution directory:

```
cd build/opencast-dist-develop-*-SNAPSHOT
./bin/start-opencast
```

This runs in the **foreground** with a Karaf console attached — start it in the background when
scripting it. (`./bin/start-opencast server` runs without the local console, `daemon` detaches.)

Startup takes a while. Opencast is up once `http://127.0.0.1:8080/` answers; the login is
`admin` / `opencast`, e.g.:

```
curl -u admin:opencast http://127.0.0.1:8080/info/me.json
```

Logs, relative to the distribution directory:

```
data/log/opencast.log
data/log/karaf.log
```

### 4. Ingest real media as an event (often unnecessary)

Real media is only needed when the change actually touches media handling, processing, playback or
distribution. For most work — API responses, admin UI, search, ACLs, metadata — an event without real
media, or no event at all, is enough, and skipping the ingest saves a lot of waiting. Think about
whether you need it before doing it.

If you do, use the ingest service's
`addMediaPackage/fast` endpoint. It creates the media package, attaches the track, and starts the
`fast` workflow in one call — no need to fiddle with the admin UI:

```
curl -u admin:opencast http://localhost:8080/ingest/addMediaPackage/fast \
  -F 'flavor=presenter/source' \
  -F mediaUri=https://radosgw.public.os.wwu.de/opencast-test-media/goat.mp4 \
  -F title=Test \
  -F identifier=test
```

`mediaUri` is downloaded by Opencast itself, so any reachable URL works (the one above is a small
public test video). `identifier` fixes the event id, which makes it easy to address the event in
follow-up API calls; drop it to have one generated. Repeat the call with different `flavor`/`mediaUri`
form fields — they are read pairwise — to attach more tracks.

Processing takes a moment; watch `data/log/opencast.log` or poll the event:

```
curl -u admin:opencast http://localhost:8080/api/events/test
```

### 5. Stop again

```
./bin/stop-opencast
```

and, back in `docs/scripts/devel-dependency-containers/`:

```
podman compose -f docker-compose-all-sql.yml down --timeout 0
```

## Checkstyle

Style is enforced by `docs/checkstyle/opencast-checkstyle.xml` (Checkstyle 8.36.2), bound to the Maven
`validate` phase with `failOnViolation=true` — so a style violation breaks the build before compilation,
for `src/main` **and** `src/test`.

Check a single module without a full build:

```
./mvnw -pl modules/<module> checkstyle:check
```

Sections can be exempted with `// CHECKSTYLE:OFF` … `// CHECKSTYLE:ON`
(or `// CHECKSTYLE:OFF checkstyle:<CheckName>` … `// CHECKSTYLE:ON checkstyle:<CheckName>` for a single check).

## Import order (the rule that trips up generated code)

```xml
<module name="ImportOrder">
  <property name="groups" value="org.opencastproject,com,net,org,java,javax"/>
  <property name="ordered" value="true"/>
  <property name="separated" value="true"/>
  <property name="option" value="top"/>
  <property name="sortStaticImportsAlphabetically" value="true"/>
</module>
```

Resulting layout — **exactly one blank line between blocks, none inside a block**:

1. **All static imports**, in one block at the very top (`option=top`), sorted alphabetically across
   *all* packages (`sortStaticImportsAlphabetically`). Static imports are **not** split into the groups
   below: `static java.lang.String.format`, `static javax.…`, `static org.apache.…`,
   `static org.opencastproject.…` all live in the same block in plain alphabetical order.
2. `org.opencastproject.*`
3. `com.*`
4. `net.*`
5. `org.*` (everything else: `org.apache`, `org.junit`, `org.slf4j`, `org.osgi`, `org.easymock`, …)
6. `java.*`
7. `javax.*`
8. **Everything else** — anything matching no group (`io.*`, `edu.*`, `de.*`, …) goes in a final block
   *after* `javax`. This is Checkstyle's implicit trailing group; it is easy to get wrong.

Group matching is longest-prefix, which is why `org.opencastproject` (group 2) wins over `org` (group 5).

Sorting inside a group is a **case-sensitive ASCII compare of the full import string**. Consequences:

- Uppercase sorts before lowercase: `javax.ws.rs.PUT` < `javax.ws.rs.Path` < `javax.ws.rs.PathParam`;
  `org.opencastproject.util.UrlSupport` < `org.opencastproject.util.data.Tuple`
  (i.e. classes in a package come *before* its subpackages).
- `_` (0x5F) sorts after uppercase letters: `…SC_NOT_FOUND` < `…SC_NO_CONTENT`.

Do not sort with a locale/case-insensitive comparator — use plain byte order (`LC_ALL=C sort`).

Also enforced: no star imports (`AvoidStarImport`), no unused imports (`UnusedImports`), no redundant
imports (same package / `java.lang`), no `sun.*`.

### Example

```java
import static org.easymock.EasyMock.expect;
import static org.opencastproject.util.DateTimeSupport.fromUTC;

import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;

import com.google.gson.JsonObject;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.restassured.http.ContentType;
```

## Other formatting rules worth remembering

- Indentation: 2 spaces; `case` 2; wrapped lines 4; `throws` continuation 8. Spaces only, no tabs.
- Max line length: 120 characters.
- Opening brace on the same line; braces mandatory for every `if`/`else`/`for`/`while`/`do`.
- Files must end with a single LF newline (LF line separators).
- Every `.java` and `.js` file needs the ECL license header from `docs/checkstyle/opencast-header.txt`
  (line 3, the copyright year line, is exempt from matching). Copy it from a neighbouring file when
  creating a new one.
- Modifier order per JLS; no redundant modifiers (e.g. `public` on interface methods).
