# Opencast — notes for coding agents

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
for `src/main` **and** `src/test`. Spotless runs before it (see below) and takes care of the imports.

Check a single module without a full build:

```
./mvnw -pl modules/<module> checkstyle:check
```

Sections can be exempted with `// CHECKSTYLE:OFF` … `// CHECKSTYLE:ON`
(or `// CHECKSTYLE:OFF checkstyle:<CheckName>` … `// CHECKSTYLE:ON checkstyle:<CheckName>` for a single check).

## Imports and Spotless

Import order, unused imports and the final newline are not maintained by hand: the Spotless Maven
plugin fixes them. It is bound to the `initialize` phase, so any lifecycle build (`./mvnw install`, even
with `-Dcheckstyle.skip=true`) rewrites the imports of the modules it builds. Do not hand-sort imports.

Invoking a goal directly (`checkstyle:check`, …) does **not** run Spotless, and Checkstyle no longer
checks imports. So after editing Java files without running a full build, run:

```
./mvnw -pl modules/<module> spotless:apply
```

CI runs `spotless:check` and fails on anything Spotless would change.

Never wrap an import statement over two lines, even if it exceeds 120 characters: Spotless parses
imports line by line and silently drops the continuation line.

Still enforced by Checkstyle: no star imports (`AvoidStarImport`), no redundant imports (same package /
`java.lang`), no `sun.*`.

## Other formatting rules worth remembering

- Indentation: 2 spaces; `case` 2; wrapped lines 4; `throws` continuation 8. Spaces only, no tabs.
- Max line length: 120 characters.
- Opening brace on the same line; braces mandatory for every `if`/`else`/`for`/`while`/`do`.
- Files must end with a single LF newline (LF line separators).
- Every `.java` and `.js` file needs the ECL license header from `docs/checkstyle/opencast-header.txt`
  (line 3, the copyright year line, is exempt from matching). Copy it from a neighbouring file when
  creating a new one.
- Modifier order per JLS; no redundant modifiers (e.g. `public` on interface methods).
