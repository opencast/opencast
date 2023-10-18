Example Installation
===========================================================================

The following instructions will focus on installing the most common Opencast setup meaning a distributed system across three nodes on Debian 12 VMs. If your setup differs, you can always refer to the linked sections for a more detailed instruction covering alternative options. The installation steps will be detailed so you can follow them step by step but for maintanance reasons of course it might make sense to automate some of the steps. This can be done for example with [ansible](ansible.md).


Hardware
---------------

For this installation three Debian 12 VMs with the following hardware will be used:

Admin node:

- Hostname: admin.example.com
- Four cores
- 8GB of memory

Worker node:

- Hostname: worker.example.com
- Four cores
- 8GB of memory

Presentation node:

- Hostname: presentation.example.com
- Four cores
- 4GB of memory

NFS share:

- Hostname: storageserver.example.com
- 5TB disk space

This setup will be sufficient for a basic distributed Opencast installation which can be used in production.


Prerequisites
------------
* For serving the Opencast data to the outside world you will need to provide valid TLS/SSL certificate for each of your nodes. This will be relevant when configuring nginx in the later steps.

* In order to be able to install Opensearch and Opencast from the official Opencast repository you need to make sure that the repository is added and activated for your system. On each node:

  * Ensure https repositories are supported:

          apt-get install apt-transport-https ca-certificates sudo wget gnupg2

  * Add Opencast repository:

          echo "deb https://pkg.opencast.org/debian {{ opencast_major_version() }}.x stable" | sudo tee /etc/apt/sources.list.d/opencast.list

      It might take some time after the release of a new Opencast version before the Debs are moved to the stable
      repository. If you need the new release prior to its promotion to stable you can use the testing repository.
      Note that the testing repository is an additional repository and still requires the stable repository to be active.

          echo "deb https://pkg.opencast.org/debian {{ opencast_major_version() }}.x stable testing" | sudo tee /etc/apt/sources.list.d/opencast.list

  * Add the repository key to your apt keyring:

          wget -qO - https://pkg.opencast.org/gpgkeys/opencast-deb.key | sudo apt-key add -

  * Update your package listing

          apt-get update

Database
------------
Opencast needs a database in order to operate and store information about events, workflows, metadata etc. For this setup a MariaDB database will be created on the Admin node but you can also use a seperate dedicated VM.

First install and start MariaDB:

```sh
% sudo apt update
% sudo apt install mariadb-server
% sudo systemctl start mariadb.service
% sudo systemctl enable mariadb.service
```

Then set secure root user credentials by running


    sudo mysql_secure_installation


The next step is to create a database for Opencast. For this start the mysql client as root


    mysql -u root -p


You will be asked for the previosly chosen password of the user root.
Next, create a database called `opencast` by executing:

```sql
CREATE DATABASE opencast CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then create a user `opencast` with a password and grant it all necessary rights:

```sql
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,DROP,INDEX,TRIGGER,CREATE TEMPORARY TABLES,REFERENCES ON opencast.*
  TO 'opencast'@'%' IDENTIFIED BY 'opencast_password';
```

Finally, leave the client and restart the database server to enable the new user(s):

    systemctl restart mariadb.service

NFS
------------

In the next step you will connect the NFS share to all of your opencast nodes.

You want to have one common user on all your systems, so that file permissions do not become an issue.. As preparation for this it makes sense to manually create an opencast user and group with a common UID and GID:

    sudo groupadd -g 1234 opencast
    sudo useradd -g 1234 -u 1234 opencast

Now you need to mount the network storage on all three servers of the Opencast clusters. To do that you need to edit the /etc/fstab on each server and add the command to mount the network storage on startup:

    storageserver.example.com:/srv/opencast /srv/opencast   nfs rw,hard,intr,rsize=32768,wsize=32768 0 0

After a reload the NFS share should be correctly mounted and ready to store the Opencast data.

    sudo mount -a


OpenSearch
------------
Opencast uses OpenSearch as a search index in order to cache and quickly access user data for the admin interface. Therefore it should be installed on the same node that serves the admin interface.

    sudo apt-get install opensearch

After installing make sure to start and enable the service:

    systemctl restart opensearch
    systemctl enable opensearch

Nginx
------------
For serving the data to the outside world this installation will focus on using nginx as a HTTP(S) proxy since it is by far the most common and also recommended approach. For a general overview of all possible setups check out the chapter [HTTPS Setup](/configuration/https/).

First of all you will need to install nginx on all Opencast nodes:

    sudo apt-get install nginx

The following configurations serves as an example for /etc/nginx/nginx.conf and can be used as a starting point for your own configuration. At minimum you will have to modify two parts of this example configuration:

1. Replace every ```(admin|worker|presentation).example.com``` with the respective hostname of the node you are currently working on.

2. Replace the lines ```ssl_certificate_key /path/to/(admin|worker|presentation).example.com.key;``` and ```ssl_certificate /path/to/(admin|worker|presentation).example.com.crt;```
    with the correct path to the corresponding TLS certificate/key.

With those changes in place, all there is left to do ist to start and
```
# Defines user and group credentials used by worker processes. If group is
# omitted, a group whose name equals that of user is used.
user    www-data;

# Defines the number of worker processes.    Setting it to the number of
# available CPU cores should be a good start. The value `auto` will try to
# autodetect that.
worker_processes auto;

# Configures logging to `/var/log/...`. Log level `error` is used by default.
error_log /var/log/nginx/error.log;

# Defines a file that will store the process ID of the main process. This needs
# to match the Systemd unit file.
pid /run/nginx.pid;

# Load dynamic modules. See /usr/share/nginx/README.dynamic.
include /usr/share/nginx/modules/*.conf;

events {
  # Sets the maximum number of simultaneous connections that can be opened by
  # a worker process.
  worker_connections 1024;
}

http {
  # Include mime types for different file extensions.
  include /etc/nginx/mime.types;

  server {
    # Enforce HTTPS by redirecting requests
    listen 80;
    listen [::]:80;
    server_name (admin|worker|presentation).example.com;

    # Serve certbot ACME requests
    location /.well-known/ {
      root /var/lib/nginx/;
    }

    # Enforce encrypted connections for everything else
    location / {
      return 301 https://(admin|worker|presentation).example.com$request_uri;
    }
  }

  server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name (admin|worker|presentation).example.com;

    ssl_certificate_key /path/to/(admin|worker|presentation).example.com.key;
    ssl_certificate     /path/to/(admin|worker|presentation).example.com.crt;

    # Accept large ingests
    client_max_body_size 0;

    location /protected {
      internal;
      alias /srv/opencast/downloads/;

      # CORS configuration
      add_header Access-Control-Allow-Origin       '$http_origin';
      add_header Access-Control-Allow-Credentials  'true';
      add_header Access-Control-Allow-Methods 'GET, OPTIONS' always;
      add_header Access-Control-Allow-Headers 'Origin,Content-Type,Accept,Authorization' always;

      if ($request_method = OPTIONS) {
        return 200;
      }
    }

    # Proxy configuration for Opencast
    location / {

      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      proxy_set_header X-Forwarded-SSL "on";

      proxy_pass http://127.0.0.1:8080;

      # Make sure to redirect location headers to HTTPS
      proxy_redirect http://$host https://$host;

      # Ensure Cookies are marked as Secure and HTTPOnly
      proxy_cookie_path / "/; HTTPOnly; Secure; SameSite=none";

      # Do not buffer responses
      proxy_buffering off;

      # Do not buffer requests
      proxy_request_buffering off;

      # CORS configuration
      add_header Access-Control-Allow-Origin       '$http_origin';
      add_header Access-Control-Allow-Credentials  'true';
      add_header Access-Control-Allow-Methods 'GET, POST, OPTIONS' always;
      add_header Access-Control-Allow-Headers 'Origin,Content-Type,Accept,Authorization' always;

      if ($request_method = OPTIONS) {
        return 200;
      }
    }

    # Include additional custom configuration
    include /etc/nginx/conf.d/extra.conf;
  }
}
```
Opencast
------------
After completing all the steps above you can now start installing the Opencast package. Start with


Essential configuration
------------

Troubleshooting
------------