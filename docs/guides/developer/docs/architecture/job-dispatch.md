# Job Dispatch

For a general introduction, consider checking out [this webinar on Job Dispatching and Job Loads](https://explore.opencast.org/webinars/v/PGom1gu6yO4).

## Overview

The job dispatcher (`JobDispatcher`, in the `serviceregistry` module) periodically looks for queued jobs and
assigns each one to the least loaded service capable of handling it. It is a scheduled polling loop that
queries the database.

## Automatic dispatcher election

Job dispatching is enabled by default on every node that runs this component. The cluster automatically elects a single
active dispatcher among all candidate nodes at runtime, and fails over to another candidate automatically if the current
one goes down.

Leader election is implemented as a lease stored in a single database row (`oc_dispatch_lock`). Every candidate node's
dispatch tick first tries to acquire or renew this lease (`DispatchLeaseManager.tryAcquireOrRenew()`) before doing
anything else; only the node holding a valid lease actually dispatches jobs that round. Acquisition is a single,
portable JPQL bulk update:

```sql
UPDATE DispatchLock d
   SET d.owner = :me, d.leaseExpires = :newExpiry
 WHERE d.owner = :me OR d.leaseExpires < CURRENT_TIMESTAMP
```

The comparison uses the database's own `CURRENT_TIMESTAMP` rather than any node's local clock, so every competing
node judges lease freshness against one shared clock instead of against each other. Race safety comes entirely from
the database engine's row-level locking during the update itself - no advisory locks, no pessimistic entity
locking, and no vendor-specific SQL, so this works identically on both supported databases (MariaDB and PostgreSQL)
as well as the H2 database used for testing.

The lease timeout (`dispatch.lock.timeout`, default `3 * dispatch.interval`) should be a small multiple of the
dispatch interval, so that one missed tick doesn't cause an unnecessary failover, while a genuinely dead node is
still detected and replaced quickly. On clean shutdown, a node proactively releases its lease
(`DispatchLeaseManager.release()`) so a standby node can take over immediately rather than waiting out the full
timeout.

As a second line of defense, JPA optimistic locking (the `@Version` column on `JpaJob`) still guards against
double-dispatch of the same job even in the unlikely event that two nodes both believe they hold the lease during
a narrow race window.

To exclude a specific node from ever becoming the dispatcher, set `dispatch.interval=0` on that node in
`org.opencastproject.serviceregistry.impl.JobDispatcher.cfg`.
