//$(document).ready(function() {
define(['jquery', 'bootbox', 'underscore', 'alertify/alertify', 'bootstrap-accessibility',
        'jquery.liveSearch', 'seedrandom', 'jquery.utils',
        'dropdowns-enhancement'
    ],
function($, bootbox, _, alertify) {

        // bool
        var debug = false;
        var askedForLogin = false;
        var checkLoggedOut = false;

        // URL's and Endpoints
        var restEndpoint = "/search/";
        var playerEndpoint = "";
        var infoMeURL = "/info/me.json";
        var defaultPlayerURL = "/engage/ui/watch.html";
        var springSecurityLoginURL = "/j_spring_security_check";
        var springSecurityLogoutURL = "/j_spring_security_logout";

        // various variables
        var mediaContainer = '<div class="col-xs-12 col-sm-6 col-md-4 col-lg-4">';
        var page = 1;
        var sort = "";
        var totalEntries = -1;
        var bufferEntries = 18; // number of entries to load for one page.
        var restData = "";
        var active = "episodes";
        var stack = new Array();
        var visited = 1;
        var tabIndexNumber = 100;
        var seriesRgbMax = new Array(220, 220, 220); //color range.
        var seriesRgbOffset = new Array(20, 20, 20); //darkest possible color
        var sortMap = {};
        var sortDescription = "";

        // localization placeholder
        var title_enterUsernamePassword = "Login with your Opencast account";
        var placeholder_username = "Username";
        var placeholder_password = "Password";
        var placeholder_rememberMe = "Remember me";
        var msg_enterUsernamePassword = "Please enter your username and password:";
        var msg_html_sthWentWrong = "<h2> Something went wrong. Try again! </h2>";
        var msg_html_noepisodes = "<h2>No episodes available</h2>";
        var msg_html_noseries = "<h2>No series available</h2>";
        var msg_html_loading = "<h2>Loading...</h2>";
        var msg_html_mediapackageempty = "<h2>No episodes available</h2>";
        var msg_html_nodata = "<h2>No data available</h2>";
        var msg_loginSuccessful = "Successfully logged in. Please reload the page if the page does not reload automatically.";
        var msg_not_logged_in = "Not logged in";
        var msg_loginFailed = "Failed to log in.";
        var infoMeURL = "/info/me.json";
        var defaultPlayerURL = "/engage/ui/watch.html";
        var springSecurityLoginURL = "/j_spring_security_check";
        var springSecurityLogoutURL = "/j_spring_security_logout";
        var springLoggedInStrCheck = "j_spring_security_check";
        var $navbarEpisodes = "#navbarEpisodes";
        var $navbarSeries = "#navbarSeries";
        var $headerLogo = "#headerLogo";
        var $nav_switch_li = "#nav-switch li";
        var $oc_search_form = "#oc-search-form";
        var $oc_sort_dropdown = ".oc-sort-dropdown";
        var $main_container = "#main-container";
        var $main_grid = ".main-grid";
        var $next = ".next";
        var $previous = ".previous";
        var $first = ".first";
        var $last = ".last";
        var $more_content = "#more";
        var $no_more_content = "#no-more";
        var id_mhlogolink = "mhlogolink";
        var $nav_username = "#nav-username";
        var $nav_loginlogoutLink = "#nav-loginlogoutLink";
        var $name_loginlogout = "#name-loginlogout";
        var $glyph_loginlogout = "#glyph-loginlogout";

        var epFromGet = "",
            searchQuery = "";

        function log(args) {
            if (debug && window.console) {
                console.log(args);
            }
        }

        function detectLanguage() {
            return navigator.language || navigator.userLanguage || navigator.browserLanguage || navigator.systemLanguage || "en-US";
        }

        function getDefaultLanguage(language) {
            var lang = language.substring(0, language.indexOf("-"));
	    if (!lang || lang.length <= 0) lang = language;

            switch (lang) {
                case "en":
                    return "en-US";
                case "de":
                    return "de-DE";
                case "es":
                    return "es-ES";
                case "fr":
                    return "fr-FR";
                case "gl":
                    return "gl-ES";
                case "nl":
                    return "nl-NL";
                case "fi":
                    return "fi-FI";
                case "it":
                    return "it-IT";
                case "ja":
                    return "ja-JA";
                case "tlh":
                    return "tlh-AA";
                case "no":
                    return "no-NO";
                case "pt":
                    return "pt-BR";
                case "ru":
                    return "ru-RU";
                case "sv":
                    return "sv-SE";
                case "tr":
                    return "tr-TR";
                case "zh":
                    return "zh-CN";
                case "el":
                    return "el-GR";
                default:
                    return null;
            }
        }

        function loadAndTranslate(callbackFunction, lang) {
            log("loadAndTranslate");

            if (!lang) lang = detectLanguage();
            var selectedLanguage = lang;
            if (getDefaultLanguage(lang) !== null) {
                selectedLanguage = getDefaultLanguage(lang);
            }

            var jsonstr = window.location.origin + "/engage/ui/language/" + selectedLanguage + ".json";
            log("Detected Language: " + selectedLanguage);

            var template;

            // load template
            $.ajax({
                url: window.location.origin + "/engage/ui/template/desktop.html",
                dataType: "html"
            }).fail(function() {
                console.error("Something went wrong while loading template.");
                $("body").append("Error loading template.");
            }).done(function(tData) {
                // set template data
                template = _.template(tData);

                // load translation
                $.ajax({
                    url: jsonstr,
                    dataType: "json"
                }).fail(function() {
                    console.warn("Failed to load language data for " + selectedLanguage + ". Try to load alternative.");
                    // load default en-US
                    loadAndTranslate(callbackFunction, "en-US");

                }).done(function(data) {
                    log("Append template and set variables.");
                    setTemplateAndVariables(data, template);
                }).then(callbackFunction);

            }); // ajax translation
        }

        function GetURLParameter(sParam) {
            var sPageURL = window.location.search.substring(1);
            var sURLVariables = sPageURL.split('&');
            for (var i = 0; i < sURLVariables.length; i++) {
                var sParameterName = sURLVariables[i].split('=');
                if (sParameterName[0] == sParam) {
                    return sParameterName[1];
                }
            }
        }

        String.prototype.endsWith = function(suffix) {
            return this.indexOf(suffix, this.length - suffix.length) !== -1;
        };

        function setTemplateAndVariables(tData, template) {
            log("setTemplateAndVariables");

            $("body").append(template(tData));

            sortMap["DATE_CREATED_DESC"] = tData.recording_date_new;
            sortMap["DATE_CREATED"] = tData.recording_date_old;
            sortMap["DATE_PUBLISHED_DESC"] = tData.publishing_date_new;
            sortMap["DATE_PUBLISHED"] = tData.publishing_date_old;
            sortMap["TITLE"] = tData.title_a_z;
            sortMap["TITLE_DESC"] = tData.title_z_a;
            sortMap["CREATOR"] = tData.author_a_z;
            sortMap["CREATOR_DESC"] = tData.author_z_a;
            sortMap["CONTRIBUTOR"] = tData.contributor_a_z;
            sortMap["CONTRIBUTOR_DESC"] = tData.contributor_z_a;
            sortMap["PUBLISHER"] = tData.publisher_a_z;
            sortMap["PUBLISHER_DESC"] = tData.publisher_z_a;
            sortMap["SERIES_ID"] = tData.series;
            sortMap["LANGUAGE"] = tData.language;
            sortMap["LICENSE"] = tData.license;
            sortMap["SUBJECT"] = tData.subject;
            sortMap["DESCRIPTION"] = tData.description;

            // set variables
            title_enterUsernamePassword = tData.login_title;
            placeholder_username = tData.username;
            placeholder_password = tData.password;
            placeholder_rememberMe = tData.remember_me;
            msg_enterUsernamePassword = tData.login_request;
            msg_html_sthWentWrong = "<h2>" + tData.sthWentWrong + "<h2>";
            msg_html_noepisodes = "<h2>" + tData.no_episodes + "</h2>";
            msg_html_noseries = "<h2>" + tData.no_series + "</h2>";
            msg_html_loading = "<h2>" + tData.loading + "</h2>";
            msg_html_mediapackageempty = "<h2>" + tData.no_episodes + "</h2>";
            msg_html_nodata = "<h2>" + tData.no_data + "</h2>";
            msg_loginSuccessful = tData.login_success;
            msg_loginFailed = tData.login_failed;
            msg_not_logged_in = tData.not_logged_in;

        }

        function initialize() {
            log("Start initialize.");

            $("#" + id_mhlogolink).attr("href", location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : '') + "/engage/ui");
            getInfo();

            registerHandler();

            debug = GetURLParameter("debug");

            if (debug) {
                $.enableLogging(true);
            }

            // load series or episodes
            var loadSer = ((GetURLParameter("s") == undefined) ||
                (GetURLParameter("s") != 1)) ? false : true;

            var loadEp = ((GetURLParameter("e") == undefined) ||
                (GetURLParameter("e") != 1)) ? false : true;

            // get page from url parameter
            var pageNotGet = GetURLParameter("p") == undefined;
            var lastPage = false;
            page = pageNotGet ? 1 : parseInt(GetURLParameter("p"));

            // load episodes from specific series
            epFromGet = GetURLParameter("epFrom");
            epFromGet = epFromGet == undefined ? "" : "sid=" + epFromGet + "&";

            // search query from form
            searchQuery = GetURLParameter("q") == undefined ? "" : "q=" + GetURLParameter("q") + "&";
            log("Searching for: " + searchQuery);
            if (searchQuery != "") $("#searchInput").val(decodeURI(GetURLParameter("q")));

            // sort
            if (GetURLParameter("sort") == undefined) {
                sort = "";
            } else {
                sort = "sort=" + GetURLParameter("sort") + "&";
                sortDescription = GetURLParameter("sort");
                $("#" + sortDescription).prop("checked", true);
                $("#buttonSorting").text(sortMap[sortDescription]);
            }
            // sort = GetURLParameter("sort") == undefined ? "" : "sort="+GetURLParameter("sort")+"&";
            var prefix = location.search == "" ? "?p=" : "&p=";

            if (loadEp || (!loadEp && !loadSer)) {
                $($nav_switch_li).removeClass("active");
                $("#navbarEpisodes").addClass("active");
                // load Episodes and set pagination
                loadEpisodes(true, epFromGet + searchQuery + sort, function() {
                    if (page < Math.floor(totalEntries / bufferEntries) + 1) {
                        $("#lastPage").attr("href", (pageNotGet ? location.href + prefix + (Math.floor(totalEntries / bufferEntries) + 1) :
                            location.href.replace(/(p=[\d]*)/, "p=" + (Math.floor(totalEntries / bufferEntries) + 1))));

                        $("#nextPage").attr("href", (pageNotGet ? location.href + prefix + (page + 1) :
                            location.href.replace(/(p=[\d]*)/, "p=" + (page + 1))));
                    } else {
                        $($next).addClass("disabled");
                    }
                });

            } else if (loadSer) {
                $($nav_switch_li).removeClass("active");
                $("#navbarSeries").addClass("active");
                // load Series and set pagination
                loadSeries(true, searchQuery + sort, function() {
                    if (page < Math.floor(totalEntries / bufferEntries) + 1) {
                        $("#lastPage").attr("href", (pageNotGet ? location.href + prefix + (Math.floor(totalEntries / bufferEntries) + 1) :
                            location.href.replace(/(p=[\d]*)/, "p=" + (Math.floor(totalEntries / bufferEntries) + 1))));

                        $("#nextPage").attr("href", (pageNotGet ? location.href + prefix + (page + 1) :
                            location.href.replace(/(p=[\d]*)/, "p=" + (page - 1))));
                    } else {
                        $($next).addClass("disabled");
                    }
                });
            }

            if (page == 1) {
                $($previous).addClass("disabled");
            } else {
                $("#prevPage").attr("href", (pageNotGet ? location.href + prefix + (page - 1) :
                    location.href.replace(/(p=[\d]*)/, "p=" + (page - 1))));

                $("#firstPage").attr("href", (pageNotGet ? location.href + prefix + "1" :
                    location.href.replace(/(p=[\d]*)/, "p=1")));
            }

            endlessScrolling();
            $($main_container).html(msg_html_loading);
        }

        function endlessScrolling() {
            log("init endless scrolling");
            $(window).scroll(function() {
                $($more_content).hide();
                $($no_more_content).hide();
                $($more_content).show();

                if ($(window).scrollTop() + $(window).height() > $(document).height() - 200) {
                    $($more_content).show();
                }
                if ($(window).scrollTop() + $(window).height() == $(document).height()) {
                    $($more_content).hide();
                    $($no_more_content).hide();
                    page++;

                    if (page > 1) {
                        $($previous).removeClass("disabled");
                    };

                    if ((page - 1) * bufferEntries > totalEntries) {
                        $($no_more_content).show();
                        $($next).addClass("disabled");
                    } else {
                        log("loading data");
                        $($more_content).show();
                        if (active == "series") {
                            loadSeries(false, epFromGet + searchQuery + sort);
                        } else {
                            loadEpisodes(false, epFromGet + searchQuery + sort);
                        }
                    }

                }
            });

        }

        $(document).ready(function() {
            loadAndTranslate(initialize);
        });

        function login() {
            if (!askedForLogin) {
                askedForLogin = true;
                var username = "User";
                var password = "Password";
                var box = bootbox.dialog({
                    title: title_enterUsernamePassword,
                    message: '<form class="form-signin" onsubmit="$(\'.btn-success\').click(); return false;">' +
                        '<h3 class="form-signin-heading">' + msg_enterUsernamePassword + '</h3>' +
                        '<input id="username" type="text" class="form-control form-control-custom" name="username" placeholder="' + placeholder_username + '" required="true" />' +
                        '<input id="password" type="password" class="form-control form-control-custom" name="password" placeholder="' + placeholder_password + '" required="true" />' +
                        '<label class="checkbox">' +
                        '<input type="checkbox" value="' + placeholder_rememberMe + '" id="rememberMe" name="rememberMe" checked> ' + placeholder_rememberMe +
                        '</label>' +
                        '<input type=submit style="display: none;" />' +
                        '</form>',
                    buttons: {
                        cancel: {
                            label: "Cancel",
                            className: "btn-default",
                            callback: function() {
                                askedForLogin = false;
                            }
                        },
                        login: {
                            label: "Log in",
                            className: "btn-success",
                            callback: function() {
                                var username = $("#username").val().trim();
                                var password = $("#password").val().trim();
                                if ((username !== null) && (username.length > 0) && (password !== null) && (password.length > 0)) {
                                    $.ajax({
                                        type: "POST",
                                        url: springSecurityLoginURL,
                                        data: {
                                            "j_username": username,
                                            "j_password": password,
                                            "_spring_security_remember_me": $("#rememberMe").is(":checked")
                                        }
                                    }).done(function(msg) {
                                        password = "";
                                        if (msg.indexOf(springLoggedInStrCheck) == -1) {
                                            location.reload();
                                            alertify.success(msg_loginSuccessful + " '" + username + "'.");
                                            loadAndTranslate(initialize);
                                        } else {
                                            alertify.error(msg_loginFailed + " '" + username + "'.");
                                        }
                                        askedForLogin = false;
                                    }).fail(function(msg) {
                                        password = "";
                                        alertify.error(msg_loginFailed + " '" + username + "'.");
                                        askedForLogin = false;
                                    });
                                } else {
                                    askedForLogin = false;
                                }
                            }
                        }
                    },
                    className: "usernamePassword-modal",
                    onEscape: function() {
                        askedForLogin = false;
                    },
                    closeButton: false
                });
                box.bind('shown.bs.modal', function(){
                    box.find("input#username").focus();
                });
            }
        }

        function logout() {
            $.ajax({
                type: "GET",
                url: springSecurityLogoutURL
            }).complete(function(msg) {
                location.reload();
                loadAndTranslate(initialize);
            });
        }

        function setAnonymousUser() {
            $($nav_username).html(msg_not_logged_in);
            $($name_loginlogout).html("Login");
            $($glyph_loginlogout).removeClass("glyphicon-log-out").addClass("glyphicon-log-in");
            $($nav_loginlogoutLink).unbind("click");
            $($nav_loginlogoutLink).click(login);
        }

        function setUsername(name) {
            $($nav_username).html(name);
            $($name_loginlogout).html("Logout");
            $($glyph_loginlogout).removeClass("glyphicon-log-in").addClass("glyphicon-log-out");
            $($nav_loginlogoutLink).unbind("click");
            $($nav_loginlogoutLink).click(logout);
        }

        function getInfo() {
            log("Get info");
            $.ajax({
                url: infoMeURL,
                dataType: "json",
                success: function(data) {
                    log("User information loaded");

                    if (data) {
                        if (data.roles && (data.roles.length > 0)) {
                            var notAnonymous = false;
                            for (var i = 0; i < data.roles.length; ++i) {
                                if (data.roles[i] != "ROLE_ANONYMOUS") {
                                    notAnonymous = true;
                                }
                            }
                            if (notAnonymous) {
                                if (checkLoggedOut) {
                                    window.location = springSecurityLogoutURL;
                                }
                                log("User is not anonymous");
                                if (data.username) {
                                    log("Username found: " + data.username);
                                    setUsername(data.username);
                                } else if (data.user && data.user.username) {
                                    log("Username found: " + data.user.username);
                                    setUsername(data.user.username);
                                } else {
                                    log("Username not found");
                                }
                            } else {
                                checkLoggedOut = false;
                                log("User is anonymous");
                                setAnonymousUser();
                            }
                        } else {
                            log("Error: No role");
                            setAnonymousUser();
                        }
                        if (data.org && data.org.properties) {
                            var logo = data.org.properties.logo_mediamodule ? data.org.properties.logo_mediamodule : "";
                            $($headerLogo).attr("src", logo);

                            var player = data.org.properties.player ? data.org.properties.player : defaultPlayerURL;
                            if (player.charAt(0) != "/")
                                player = "/" + player;

                            playerEndpoint = player;
                        } else {
                            log("Error: No info data received.");
                            setAnonymousUser();
                        }
                    }

                    log("Chosen player: " + player);
                }
            })
        }

        function registerHandler() {

            /* handle search input */
            $($oc_search_form).submit(function(event) {
                if (active == "series") {
                    $("#oc-search-form .form-group").append(
                        "<input type='hidden' name='s' value='1' />"
                    );
                } else if (active == "episodes") {
                    $("#oc-search-form .form-group").append(
                        "<input type='hidden' name='e' value='1' />"
                    );
                } else {
                    console.err("Error!");
                }
                return true;
            });

            $($oc_sort_dropdown).on("change", function() {
                $($oc_search_form).submit();
            });

            log("Handler registered");
        }

        function loadEpisodes(cleanGrid, rest, callback) {
            log("Loading Episodes with: " + rest);
            active = "episodes";

            var requestUrl = restEndpoint + "episode.json?limit=" + bufferEntries +
                "&offset=" + ((page - 1)) * bufferEntries +
                "&" + rest;
            $.ajax({
                url: requestUrl,
                dataType: "json",
                success: function(data) {
                    // clear main grid
                    if (cleanGrid) {
                        $($main_container).empty();
                        window.scrollTo(0, 0);
                        tabIndexNumber = 100;
                    }

                    if (data && data["search-results"] && data["search-results"]["total"]) {
                        // number of total search results
                        totalEntries = data["search-results"]["total"];
                        var total = data["search-results"]["limit"];

                        if (data["search-results"] == undefined || total == undefined) {
                            log("Error: Search results (total) undefined");
                            $($main_container).append(msg_html_sthWentWrong);
                            return;
                        };

                        if (total == 0) {
                            $($main_container).append(msg_html_noepisodes);
                            $($next).addClass("disabled");
                            return;
                        };

                        var result = data["search-results"]["result"];

                        if (page == 1) {
                            $($previous).addClass("disabled");
                        };

                        if (result.length < bufferEntries || total < bufferEntries) {
                            $($next).addClass("disabled");
                        } else {
                            $($next).removeClass("disabled");
                        }

                        if (total == 1) {
                            buildGrid(result);
                            return;
                        };
                        $.each(result, function(index, val) {
                            buildGrid(val);
                        });
                    } else {
                        $($main_container).append(msg_html_noepisode);
                    }
                }
            }).then(callback);
        }

        function buildGrid(data) {
            if (data) {
                var serID = data["id"];

                if (data["id"] == undefined) {
                    log("Error: Episode with no ID.")
                    serID = "0";
                }

                var seriesClass = "";
                if (data.mediapackage) {
                    seriesClass = "series" + _.escape(data.mediapackage.series) + " ";
                }

                var tile = mediaContainer + "<a class=\"tile\" id=\"" + serID + "\" role=\"menuitem\" tabindex=\"" + tabIndexNumber++ + "\">" +
                    "<div class=\"" + seriesClass + "seriesindicator \"/> " +
                    "<div class=\"tilecontent\">";

                tile = tile + "<h4 class=\"title\">" + _.escape(data.dcTitle) + "</h4>";

                // append thumbnail
                var thumb = "";
                var time = 0;
                var creator = "<br>";
                var seriestitle = "<br>";
                var date = "<br>";

                if (data.mediapackage) {
                    if (data.mediapackage.attachments && data.mediapackage.attachments.attachment) {
                        // Try finding the best preview image:
                        // presentation/search+preview > presenter/search+preview > any other preview
                        for (var i = 0; i < data.mediapackage.attachments.attachment.length; i++) {
                            var att = data.mediapackage.attachments.attachment[i];
                            if (att.url && att.type.indexOf('+preview') > 0) {
                                if (thumb == '' || att.type.indexOf('/search+preview') > 0) {
                                    thumb = data.mediapackage.attachments.attachment[i].url;
                                    if (att.type == 'presentation/search+preview') {
                                        // Stop if we got presentation/search+preview. We do not want to overwrite that.
                                        break;
                                    }
                                }
                            }
                        }
                        tile = tile + '<div><img class="thumbnail img-responsive img-rounded" src="' + thumb + '"></div>';
                    }

                    tile = tile + "<div class=\"infos\">";

                    if (data.dcCreator) {
                        creator = _.escape(data.dcCreator);
                    };
                    tile = tile + "<div class=\"creator\">" + creator + "</div>";

                    if (data.mediapackage.seriestitle) {
                        seriestitle = _.escape(data.mediapackage.seriestitle);
                    };
                    tile = tile + "<div class=\"seriestitle\">" + seriestitle + "</div>";

                    if (data.dcCreated) {
                        date = new Date(data.dcCreated);
                    };
                    tile = tile + "<div class=\"date\">" + date.toLocaleDateString() + "</div>";

                    if (data.mediapackage.duration) {
                        time = data.mediapackage.duration;
                        var seconds = Math.floor((time / 1000) % 60);
                        var minutes = Math.floor((time / (1000 * 60) % 60));
                        var hours = Math.floor((time / (1000 * 60 * 60) % 60));

                        if (seconds < 10) {
                            seconds = "0" + seconds;
                        };
                        if (minutes < 10) {
                            minutes = "0" + minutes;
                        };
                        if (hours < 10) {
                            hours = "0" + hours;
                        };

                        tile = tile + "<div class=\"duration\">" + hours + ":" + minutes + ":" + seconds + "</div>";
                    };

                    tile = tile + "</div></div></div></a>";

                    $($main_container).append(tile);

                    $("#" + _.escape(data["id"])).attr("href", playerEndpoint + "?id=" + _.escape(data["id"]));

                    $("#" + _.escape(data["id"])).on("keypress", function(ev) {
                        if (ev.which == 13 || ev.which == 32) {
                            $(location).attr("href", playerEndpoint + "?id=" + _.escape(data["id"]));
                        }
                    });

                    if (data.mediapackage.seriestitle) {
                        var color = generateSeriesColor(data.mediapackage.series);
                        $("." + seriesClass).css({
                            'background': color
                        });
                    }
                } else {
                    $($main_container).html(msg_html_mediapackageempty);
                }
            } else {
                $($main_container).html(msg_html_nodata);
            }

            /* TODO: Swipe events etc. for mobile devices */
        }

        function createSeriesGrid(data) {
            log("build series grid");
            if (data && data.id) {
                var seriesClass = "series" + _.escape(data.id) + " ";
                var color = generateSeriesColor(data.id);

                var creator = "<br>";
                var contributor = "<br>";

                var tile = mediaContainer + "<a class=\"tile\" id=\"" + _.escape(data.id) + "\" role=\"menuitem\" tabindex=\"" + tabIndexNumber++ + "\"> " +
                    "<div class=\"" + seriesClass + "seriesindicator \"/> " +
                    "<div class=\"tilecontent\">";

                tile = tile + "<h4 class=\"title\">" + (data.dcTitle ? _.escape(data.dcTitle) : "Unknown title") + "</h4>";

                if (data.dcCreator) {
                    creator = _.escape(data.dcCreator);
                };
                tile = tile + "<div class=\"creator\">" + creator + "</div>";

                if (data.dcContributor) {
                    contributor = _.escape(data.dcContributor);
                };
                tile = tile + "<div class=\"contributor\">" + contributor + "</div>";

                tile = tile + "</div></div></a>";

                $($main_container).append(tile);
                $("#" + _.escape(data.id)).attr("href", "?e=1&p=1&epFrom=" + _.escape(data.id));

                $("#" + _.escape(data.id)).on("keypress", function(ev) {
                    log("keypress")
                    if (ev.which == 13 || ev.which == 32) {
                        restData = "sid=" + data.id;
                        page = 1;
                        active = "episodes";
                        $($navbarEpisodes).addClass("active");
                        $($navbarSeries).removeClass("active");
                        pushHistory(1, "episodes", restData);
                        loadEpisodes(true);
                    }
                });

                $("." + seriesClass).css({
                    'background': color
                });
                log("Series Color: " + seriesClass + " " + color);
            } else {
                log("Error: Data for creating series grid is empty.");
            }
        }

        function loadSeries(cleanGrid, rest, callback) {
            log("Loading Series with: " + rest);
            active = "series";
            var requestUrl = restEndpoint + "/series.json?limit=" + bufferEntries + "&offset=" + (page - 1) * bufferEntries + "&" + rest;
            $.ajax({
                url: requestUrl,
                dataType: "json",
                success: function(data2) {
                    if (data2 && data2["search-results"] && data2["search-results"]["total"]) {
                        if (cleanGrid) {
                            $($main_container).empty();
                            window.scrollTo(0, 0);
                            tabIndexNumber = 100;
                        }

                        totalEntries = data2["search-results"]["total"];
                        var total = data2["search-results"]["limit"];

                        if (total == 0) {
                            $($main_container).append(msg_html_noseries);
                            $($next).addClass("disabled");
                            return;
                        };

                        var result = data2["search-results"]["result"];

                        if (page == 1) {
                            $($previous).addClass("disabled");
                        };

                        if (result.length < bufferEntries || total < bufferEntries) {
                            $($next).addClass("disabled");
                        } else {
                            $($next).removeClass("disabled");
                        }

                        if (total == 1) {
                            createSeriesGrid(result);
                            return;
                        };

                        $.each(result, function(index, val) {
                            createSeriesGrid(val);
                        });
                    } else {
                        $($main_container).append(msg_html_noseries);
                    }
                }
            }).then(callback);
        }

        function generateSeriesColor(value) {
            var rgb = new Array(0, 0, 0);

            for (var i = 0; i < value.length; ++i) {
                rgb[(i % 3)] += value.charCodeAt(i);
            }

            for (var i = 0; i < 3; ++i) {
                rgb[i] = ((rgb[i] % seriesRgbMax[i]) + seriesRgbOffset[i]).toString(16);
                if (rgb[i].length < 1) {
                    rgb[i] = "0" + rgb[i];
                }
            }

            return "#" + rgb[0] + rgb[1] + rgb[2];
        }
    });
