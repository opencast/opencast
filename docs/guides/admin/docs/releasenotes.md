# Opencast 19: Release Notes

## Opencast 19.11

This version contains only a number of security issue fixes, two of which are rated critical.  The fixes, in order of
severity rating are:

- [[GHSA-87w3-phwh-q8vx](https://github.com/opencast/opencast/security/advisories/GHSA-87w3-phwh-q8vx)] -
  Path traversal through unvalidated media package and element identifiers leading to arbitrary file write, deletion,
  and remote code execution
- [[GHSA-gr3h-hxr8-phqm](https://github.com/opencast/opencast/security/advisories/GHSA-gr3h-hxr8-phqm)] -
  Server-side template injection via workflow properties in FreeMarker-rendered workflow operation settings
- [[GHSA-hgqh-jpvg-xx9f](https://github.com/opencast/opencast/security/advisories/GHSA-hgqh-jpvg-xx9f)] -
  Users can delete events they only have read access to
- [[GHSA-x4wc-h58f-2wm9](https://github.com/opencast/opencast/security/advisories/GHSA-x4wc-h58f-2wm9)] -
  Unauthenticated reflected XSS in the REST documentation endpoint (/docs.html)
- [[GHSA-qqqp-5j2v-jvfp](https://github.com/opencast/opencast/security/advisories/GHSA-qqqp-5j2v-jvfp)] -
  Cross-tenant exposure of playlists in multi-organization installations
- [[GHSA-jrcq-hch7-gvv5](https://github.com/opencast/opencast/security/advisories/GHSA-jrcq-hch7-gvv5)] -
  Unconditional Trust of X-Forwarded-* Headers (No Trusted-Proxy Allowlist)

### Potentially breaking changes

This release is unusual in the sense that there are additional checks you should run __before__
applying the upgrade.  Please read the following completely and address

__Path traversal via media package and element components (GHSA-hgqh-jpvg-xx9f)__

Media package and media package element identifiers are now validated for path traversal when a
media package is ingested or otherwise parsed from XML. The validation covers the media package
identifier and the identifiers of all of its elements, including the tracks, attachments and
catalogs nested inside a publication. Identifiers that are empty, that consist of the current or
parent directory reference (``.`` or ``..``), or that contain a path separator (``/`` or ``\``)
are rejected; such identifiers could be used to read, write or delete files outside the
configured storage roots. A ``..`` sequence *inside* an identifier (e.g. ``track..1``) is a
harmless literal file system name and keeps working. Media packages with an explicitly empty
identifier (``id=""``) are rejected at the parse boundary; media packages without an ``id``
attribute at all keep working and receive a generated identifier.

Note for existing data: media package or element identifiers that contain a path separator (e.g.
DOI-style ids like ``10.0000/2``), or that are empty or consist of ``.`` or ``..``, were
accepted by some Opencast versions, but are now rejected at the parse boundary. Media packages
with such identifiers can no longer be loaded - events or snapshots that use them cannot be
listed or retracted through the API. If your deployment uses such identifiers, the affected
events must be dealt with before the upgrade; the upgrade guide has queries for finding them.
Operations that load several media packages no longer fail because of a single affected one:
when the search index is rebuilt, when the search service
fills in missing series ids at start-up, when a series is loaded, and when the asset manager
rebuilds its index or loads several snapshots at once (e.g. all snapshots of a series), an
affected media package is skipped and an error is logged.

The working file repository rejects unsafe identifiers for media packages, elements,
collections and file names in every operation that resolves a path, including the read-only
collection operations (size, contents, cleanup). The file system asset stores reject unsafe
organization, media package and element identifiers and, for deletions, additionally verify
that the target is exactly the media package directory (or one of its version sub-directories)
below the store root. The download and the streaming distribution services reject publication
channel identifiers that are empty, ``.`` or ``..`` or contain a path separator, and verify that
every resolved file system path stays within the organization's distribution subtree (the checks
are lexical, so deployments that keep parts of the storage tree in symlinked directories, e.g.
on separate mounts, keep working). The download distribution service also rejects distribute and
retract operations in which the publication URI of an element does not refer to the channel and
media package of the operation, as well as retractions whose target is not a regular file, and
the streaming distribution service derives the location of a SMIL file from the channel, the
media package and the element flavor instead of parsing it from the element URI; it no longer
distributes an element whose URI already points to the streaming server. The workspace rejects
files that resolve outside its root.

The working file repository copy and move operations no longer delete a pre-existing
destination file when the operation fails; they report the failure instead.

No database schema or search index changes.  Prior to upgrading run the checks listed in the [upgrade guide](../upgrade#data-changes-1911-only).

__Email templates and workflow operation templating (GHSA-gr3h-hxr8-phqm)__

The FreeMarker engine used to render templates (e.g. email templates or workflow operation settings
such as the publish URL pattern) is now restricted: templates can no longer instantiate Java classes
(`?new` built-in), access the Java API (`?api` built-in) or load other templates
(`#include`/`<#include>`, `#import`/`<#import>`). Templates that relied on these features now fail to
process; the failure is logged with details to `data/log/opencast.log`.

In addition, when template processing fails, the error message shown in workflow error output no
longer contains the template source and the data model; only the template name is shown. The full
details are available in the log.

There are no database schema or search index changes.


## Opencast 19.10

This version contains minor updates for the admin, and editor submodules.  This release also updates Karaf to
[4.4.12](https://lists.apache.org/thread/wc4nszwz0csvzc3hwmxsg36zldh1nxq3), which addresses some security issues.

## Opencast 19.9

This version contains minor updates for the admin, editor, and studio submodules, along with the usual collection of
bug fixes.

## Opencast 19.8

This version contains addition updates for the admin, editor, and studio submodules, along with the usual collection of
bug fixes.  There are two things worthy of special attention:

- [#7788](https://github.com/opencast/opencast/pull/7788) and [#7790](https://github.com/opencast/opencast/pull/7790),
  which fix several issues regarding captions and their processing.  If you are using the default workflows and
  captions were not working, these will fix it.  Remember to ensure that your workflow definitions are updated!
- Patches for [GHSA-m6c8-jcw2-5r25](https://github.com/opencast/opencast/security/advisories/GHSA-m6c8-jcw2-5r25) and
  [GHSA-6f53-jp7x-gg7p](https://github.com/opencast/opencast/security/advisories/GHSA-6f53-jp7x-gg7p), both from 19.7
  are now applied in Paella 7.  The original fixes were present in Paella 8, but not Paella 7.

## Opencast 19.7

This version contains addition updates for the admin, editor, and studio submodules, along with the usual collection of
bug fixes.  There are three things worthy of special attention:

- [#7752](https://github.com/opencast/opencast/pull/7752) makes a change to a workflow, which will need to be manually
  applied by adopters if they are using the `output-background` configuration key.
- [GHSA-m6c8-jcw2-5r25](https://github.com/opencast/opencast/security/advisories/GHSA-m6c8-jcw2-5r25) resolves a XSS
  issue in Paella.
- [GHSA-6f53-jp7x-gg7p](https://github.com/opencast/opencast/security/advisories/GHSA-6f53-jp7x-gg7p) resolves a
  session fixation issue which allowed for account takeovers via malicious links.

Neither security issue requires any changes by adopters, and they are both dropin compatible with existing 19.x installs.

## Opencast 19.6

This version contains addition updates for the admin, editor, and studio submodules, along with the usual collection of
bug fixes.

### Integration notes
The `/play` route now passes through query parameters to the target player URL.
This allows parameters like `jwt=...` or `t=10` to be preserved
when redirecting to the configured player.

This is particularly useful for third-party integrations
where authentication tokens (like JWTs) or specific player settings
need to be passed through the redirect to the actual player interface,
while preserving the ability to configure the exact player on the Opencast side.

If a parameter with the same name already exists in the configured player path,
the value from the request will be appended, creating a multi-valued parameter.
This behavior ensures that parameters are never lost,
though player implementations may need to handle multiple values if they occur.

## Opencast 19.5

This version contains updates for the admin, editor, and studio submodules, along with the usual collection of bug fixes.

## Opencast 19.4

This release contains a fix for
(Episode ID Roles)[https://docs.opencast.org/r/19.x/admin/#configuration/episode-id-roles/#episode-id-roles], which
were broken in earlier 19.x releases.  Adopters affected by this issue should either reindex their search index or
republish their recordings.  If you do not use episode id roles then this can safely be ignored.

## Opencast 19.3

This version contains an important fix for the admin UI, which had broken scheduling in 19.1 and 19.2.

## Opencast 19.2

This version contains updates for the editor, along with the usual collection of bug fixes.

## Opencast 19.1

This version contains updates for both the admin ui, and the editor, along with the usual collection of bug fixes.

## Opencast 19.0

### Features
- Episodes in the search index can be filtered according to whether or not they are live. [[#7032](https://github.com/opencast/opencast/pull/7032)]
- Paella 8 is now included in Opencast as an alternative player [[#7166](https://github.com/opencast/opencast/pull/7166)]
- A Matomo statistics provider has been added [[#7134](https://github.com/opencast/opencast/pull/7134)]
- Default series ACLs can now be configured [[#7041](https://github.com/opencast/opencast/pull/7041)]


### Breaking Changes
- Target tag behaviour has been unified across all workflow operation handlers. [[#6648](https://github.com/opencast/opencast/pull/6648)]
- The Opencast presets module has been removed.  This previously allowed for org, and system wide presets to be
  defined, but was undocumented and untested.  This change was discussed in [5615](https://github.com/opencast/opencast/issues/5615).
  [[#6903](https://github.com/opencast/opencast/pull/6903)]
- The Opencast annotation service has been removed.  If you had `opencast-plugin-legacy-annotation` enabled in
  `org.opencastproject.plugin.impl.PluginManagerImpl.cfg` then this affects you, otherwise it should not.  This change
  was discussed in [5615](https://github.com/opencast/opencast/issues/5615).  The relevant database tables will be
  automatically removed from your database systems by the upgrade script.  [[#6902](https://github.com/opencast/opencast/pull/6902)]


### Configuration Changes
- Default workflows are now in yaml [[#6798](https://github.com/opencast/opencast/pull/6798)]
- The configuration key `video-source-flavor` for the Subtitle Timeshift Workflow Operation Handler changed to
  `video-source-flavors` (plural). [[#6901](https://github.com/opencast/opencast/pull/6901)]
- Configuring JWT authentication is now substantially simpler [[#7189](https://github.com/opencast/opencast/pull/7189)]
- The editor now displays less metadata by default [[#7053](https://github.com/opencast/opencast/pull/7053)]
- The plugin `opencast-plugin-legacy-annotation` has been removed from `org.opencastproject.plugin.impl.PluginManagerImpl.cfg`.
  [[#6902](https://github.com/opencast/opencast/pull/6902)]
- Target tag handling has changed with [[#6648](https://github.com/opencast/opencast/pull/6648)]:

    All workflow operation handlers adapt the same method of
    handling target tags (if they support target tags).

    The chosen behaviour is that of the tag WOH:
    "If a target-tag starts with a '-', it will be removed from
    preexisting tags, if a target-tag starts with a '+', it will
    be added to preexisting tags. If there is no prefix, all
    preexisting tags are removed and replaced by the target-tags."

    This means that all WOH that did not support "+" and "-" now do.
    THIS CONSTITUTES A BEHAVIOUR CHANGE FOR SOME WORKFLOW OPERATION HANDLERS!
    In particular, some WOH were adding tags instead of replacing them,
    even though they were not prefaced with a "+". Adopters are advised
    to check their workflows.

For more details, please take a look at the [full changelog](changelog/opencast-19.md). If you want to update Opencast
from a previous version, you should also read the [upgrade guide](upgrade.md).

## Release Schedule

| Date              | Phase                    |
|-------------------|--------------------------|
| November 19, 2025 | Release Branch Cut       |
| December 17, 2025 | Release of Opencast 18.0 |


## Release Managers

- Greg Logan (Logan IT Enterprises)
- Lukas Gehrlein (ssystems Gmbh.)
