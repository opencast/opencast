/**
 * Copyright 2009-2011 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*jslint browser: true, nomen: true*/
/*global define, CustomEvent*/
define(['require', 'jquery', 'underscore', 'backbone'], function(require, $, _, Backbone) {
    var PluginInfoModel = Backbone.Model.extend({
        // URL of the search enpoint
        urlRoot: PLUGIN_MANAGER_PATH,
        initialize: function() {

        },
        defaults: {
            "pluginlist": {
                "plugins": {}
            }
        }
    });

    var PluginModelCollection = Backbone.Collection.extend({});

    /*
     * Model with informations about the current user and the current MH configuration
     */
    var MeInfoModel = Backbone.Model.extend({
        urlRoot: "../../../info/me.json",
        initialize: function() {
            this.ready = false;
            this.fetch({
                success: function(me) {
                    me.ready = true;
                    var hotkeys = new Array(); // hardcoded hotkeys
                    var customHotkeys = new Array(); // custom hotkeys
                    // extract hotkey infos
                    $.each(me.attributes.org.properties, function(key, value) { // hardcoded regex
                        if (key.match("theodul.hotkey.(pause|jumpToBegin|nextChapter|prevChapter|volUp|volDown|mute|jumpToX|fullscreen|nextEpisode|prevEpisode)") !== null) {
                            hotkeys.push({
                                name: key.substring(15, key.length),
                                key: value
                            })
                        } else if (key.match("theodul.hotkey.") !== null) { // else hotkeys are customs
                            var cSplit = key.substring(15, key.length).split("."); // cut key string and split into app(0) and func(1) string, examp. theodul.hotkey.annotation.add
                            customHotkeys.push({
                                app: cSplit[0],
                                func: cSplit[1],
                                key: value
                            });
                        }
                    });
                    me.set("hotkeys", hotkeys);
                    me.set("hotkeysCustom", customHotkeys);
                }
            });
        }
    });

    /*
     * Main Model Prototype
     */
    var EngageModel = Backbone.Model.extend({
        initialize: function() {
            // parse url parameters
            var match, pl = /\+/g, // regex for replacing addition symbol
                // with a space
                search = /([^&=]+)=?([^&]*)/g,
                decode = function(s) {
                    return decodeURIComponent(s.replace(pl, " "));
                },
                query = window.location.search.substring(1);

            var urlParams = {}; // stores url params
            while (match = search.exec(query)) {
                urlParams[decode(match[1])] = decode(match[2]);
            }
            this.set("urlParameters", urlParams);
            // set players debug mode
            if (this.get("urlParameters").debug == "true") {
                this.set("isDebug", true);
            } else {
                this.set("isDebug", false);
            }
            if (this.get("urlParameters").debugEvents == "true") {
                this.set("isEventDebug", true);
            } else {
                this.set("isEventDebug", false);
            }
            // check mode, if no mode param given try to discover browser
            if (this.get("urlParameters").mode == "desktop") {
                this.set("mode", "desktop");
            } else if (this.get("urlParameters").mode == "embed") {
                this.set("mode", "embed");
            } else if (this.get("urlParameters").mode == "mobile") {
                this.set("mode", "mobile");
            } else {
                var isMobile = false;;
                // code from http://detectmobilebrowsers.com/, update regex expression from time to time!
                (function(a) {
                    isMobile = /(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a) || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0, 4))
                })(navigator.userAgent || navigator.vendor || window.opera);
                if (isMobile) {
                    this.set("mode", "mobile");
                } else {
                    this.set("mode", "desktop");
                }
            }
            if (window.console) {
                console.log("EngageModel: Player mode: " + this.get("mode"));
            }
        },
        defaults: {
            "pluginsInfo": new PluginInfoModel(),
            "pluginModels": new PluginModelCollection(),
            "meInfo": new MeInfoModel(),
            "urlParameters": {}
        }
    });

    return EngageModel;
});
