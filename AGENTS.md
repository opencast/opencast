# Opencast — notes for Claude

## Building

Always use the Maven wrapper from the repository root — `./mvnw`, never a system `mvn`. It pins the
Maven version the project expects, and `${opencast.basedir}` (used e.g. for the Checkstyle config path)
resolves correctly only from the root. Target individual modules with `-pl` instead of changing
directory:

```
./mvnw -pl modules/<module> <goal>
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
