
# Paella Player 7 Opencast Module

## Requisites

Node.js version 14 or higher is required.

## Debug and develop

To debug Paella Player, a tool based on webpack debug server is provided, which allows to run Opencast through a proxy.

### Install Paella Player in standalone mode

First of all, install the Paella Player dependencies using npm:

```sh
cd [opencast-root]/modules/engage-paella-7
npm ci
```

### Run Paella Player using the webpack proxy

By default, Paella Player will point to `http://localhost:8080` to redirect all the requests to Opencast, so if you compile and run Opencast on your local machine. To do it, you simply have to execute the following npm command:

```sh
npm run dev
```

But you also can debug and develop Paella Player without having an Opencast installation on your local machine. To do it, simply specify the `server` environment variable when you launch the npm command:

```sh
npm run dev -- --env server=https://develop.opencast.org
```

### Run debugger

You can debug access the webpack proxy using the `7070` port:

```other
http://localhost:7070/engage/ui/index.html
```

If Paella Player 7 is not the default player in your Opencast settings, you can access it using the following URL:

```other
http://localhost:7070/paella7/ui/watch.html?id=the-video-identifier
```

### Paella Player configuration

The configuration files and their resources are located outside the Paella Player 7 module, in this location:

```sh
[opencast-root]/etc/ui-config/mh_default_org/paella7
```

### Check Paella Player code

You can use the following npm commands to check your Paella Player code:

```sh
npm run eslint
npm run html-linter
npm run html-validate
```

Or you can use this command to execute all the previous commands together:

```sh
npm run check
```
