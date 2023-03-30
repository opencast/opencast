Configuring a Local Cluster for Testing
=======================================

A list of commands to quickly build and configure a local Opencast cluster.

First, build and extract distributions:

```sh
mvn clean install
cd build
tar xf opencast-dist-admin-10-SNAPSHOT.tar.gz
tar xf opencast-dist-presentation-10-SNAPSHOT.tar.gz
tar xf opencast-dist-worker-10-SNAPSHOT.tar.gz
```

Then configure a share storage space:

```
ln -s ../../opencast-dist-admin/data/opencast opencast-dist-presentation/data/opencast
ln -s ../../opencast-dist-admin/data/opencast opencast-dist-worker/data/opencast
```

Configure different network ports (`8080 -> admin`, `8081 -> presentation`, `8082 -> worker`) for the distributions:

```sh
sed -i 's/8080/8081/' opencast-dist-presentation/etc/org.ops4j.pax.web.cfg
sed -i 's/8080/8081/' opencast-dist-presentation/etc/custom.properties
sed -i 's/8080/8082/' opencast-dist-worker/etc/org.ops4j.pax.web.cfg
sed -i 's/8080/8082/' opencast-dist-worker/etc/custom.properties

sed -i 's_#prop.org.opencastproject.engage.ui.url=.*$_prop.org.opencastproject.engage.ui.url=http://localhost:8081_' */etc/org.opencastproject.organization-mh_default_org.cfg
sed -i 's_#prop.org.opencastproject.admin.ui.url=.*$_prop.org.opencastproject.admin.ui.url=http://localhost:8080_' */etc/org.opencastproject.organization-mh_default_org.cfg
```

Configure a MariaDB database:

```sh
sed -i 's/#org.opencastproject.db.jdbc.driver/org.opencastproject.db.jdbc.driver/' */etc/custom.properties
sed -i 's/#org.opencastproject.db.jdbc.url/org.opencastproject.db.jdbc.url/' */etc/custom.properties
sed -i 's/#org.opencastproject.db.jdbc.user/org.opencastproject.db.jdbc.user/' */etc/custom.properties
sed -i 's/#org.opencastproject.db.jdbc.pass/org.opencastproject.db.jdbc.pass/' */etc/custom.properties
```

Start Opencast:

```sh
cd opencast-dist-admin
./bin/start-opencast
cd opencast-dist-presentation
./bin/start-opencast
cd opencast-dist-worker
./bin/start-opencast
```
