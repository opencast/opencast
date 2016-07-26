# How to store data in the browser persistently

The Theodul Pass Player uses basil.js for storing persistent data such as the volume and the playback rate.

Basil.js unifies localstorage, cookies and session storage and provides an easy-to-use JavaScript API.

## Example Usage

In your plugin you just have to require the basil lib which is being distributed globally:

    define([..., "basil", ...], function(..., Basil, ...) {
        ...
    }

After that basil needs to be set up:

    var basilOptions = {
        namespace: 'mhStorage'
    };
    Basil = new window.Basil(basilOptions);

The default plugins have "mhStorage" as their namespace, feel free to set your own. The default storage is the localstorage; if the localstorage is not available, a cookie is being used and so on.

After setting up basil, the usage is straightforward:

    Basil.set("someKey", "someValue); // set a value
    Basil.get("someKey"); // get a value
