$(document).ready(function() {
    var debug = false;
    var restEndpoint = "/search/";
    var mediaContainer = '<div class="col-xs-12 col-sm-6 col-md-4 col-lg-4">';
    var playerEndpoint = "";
    var page = 1;
    var totalEntries = -1;
    var bufferEntries = 6; // number of entries to load for one page.
    var restData = "";
    var active = "episodes";
    var stack = new Array();
    var visited = 1;
    var tabIndexNumber = 100;
    var seriesRgbMax = new Array(220, 220, 220); //color range. 
    var seriesRgbOffset = new Array(20, 20, 20); //darkest possible color 
    var title_enterUsernamePassword = "Login with your Matterhorn account";
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
    var msg_loginFailed = "Failed to log in.";
    var infoMeURL = "/info/me.json";
    var defaultPlayerURL = "/engage/ui/watch.html?id=";
    var springSecurityLoginURL = "/j_spring_security_check";
    var springSecurityLogoutURL = "/j_spring_security_logout";
    var springLoggedInStrCheck = "<title>Opencast Matterhorn – Login Page</title>";
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
    var askedForLogin = false;
    var checkLoggedOut = false;
    var $more_content = "#more";
    var $no_more_content = "#no-more";
    var id_mhlogolink = "mhlogolink";
    var $nav_username = "#nav-username";
    var $nav_loginlogoutLink = "#nav-loginlogoutLink";
    var $name_loginlogout = "#name-loginlogout";
    var $glyph_loginlogout = "#glyph-loginlogout";

    function log(args) {
        if (debug && window.console) {
            console.log(args);
        }
    }

    String.prototype.endsWith = function(suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };

    function initialize() {
        $.enableLogging(true);

        $("#" + id_mhlogolink).attr("href", location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : ''));
        getInfo();
        registerHandler();

        var retrievedObject = sessionStorage.getItem("historyStack");
        if (retrievedObject != null) {
            stack = JSON.parse(retrievedObject);
            log("Retrieved history stack from session storage");
        } else {
            stack.push({
                "page": 1,
                "active": "episodes",
                "rest": null
            });
        }
        $($main_container).html(msg_html_loading);
        $($navbarEpisodes).addClass("active");
        $($navbarSeries).removeClass("active");
        active = "episodes";
        loadEpisodes(true);
        endlessScrolling();

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
                        loadSeries(false);
                    } else {
                        loadEpisodes(false);
                    }
                }

            }
        });

    }

    $(window).load(function() {
        initialize();
    });

    $(window).on("popstate", function(event) {
        if (window.history.state == null && stack.length == 1) {
            return
        };

        var choose = window.history.state - 1;
        if (choose < 0) {
            choose = 0;
        };

        var dest = stack[choose];

        if (dest == undefined) {
            return;
        };

        page = dest.page;
        restData = dest.rest;

        $("input").val("");

        if (dest.active == "episodes") {
            $($navbarEpisodes).addClass("active");
            $($navbarSeries).removeClass("active");
            active = "episodes";
            loadEpisodes(true);
        };
        if (dest.active == "series") {
            $($navbarSeries).addClass("active");
            $($navbarEpisodes).removeClass("active");
            active = "series";
            loadSeries(true);
        };
    });

    function pushHistory(page, active, rest) {
        stack.push({
            "page": page,
            "active": active,
            "rest": rest
        });
        visited++;
        history.pushState(visited, "", "");
        sessionStorage.setItem("historyStack", JSON.stringify(stack));
    }

    function login() {
        if (!askedForLogin) {
            askedForLogin = true;
            var username = "User";
            var password = "Password";
            bootbox.dialog({
                title: title_enterUsernamePassword,
                message: '<form class="form-signin">' +
                    '<h2 class="form-signin-heading">' + msg_enterUsernamePassword + '</h2>' +
                    '<input id="username" type="text" class="form-control form-control-custom" name="username" placeholder="' + placeholder_username + '" required="true" autofocus="" />' +
                    '<input id="password" type="password" class="form-control form-control-custom" name="password" placeholder="' + placeholder_password + '" required="true" />' +
                    '<label class="checkbox">' +
                    '<input type="checkbox" value="' + placeholder_rememberMe + '" id="rememberMe" name="rememberMe" checked> ' + placeholder_rememberMe +
                    '</label>' +
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
                                        initialize();
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
        }
    }

    function logout() {
        $.ajax({
            type: "GET",
            url: springSecurityLogoutURL,
        }).done(function(msg) {
            checkLoggedOut = true;
            initialize();
        }).fail(function(msg) {
            $($nav_logoutLink).attr("href", springSecurityLogoutURL);
        });
    }

    function setAnonymousUser() {
        $($nav_username).html("Not logged in");
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
                        var logo = data.org.properties.logo_large ? data.org.properties.logo_large : "";
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
        /* register handler for navbar */
        $($nav_switch_li).click(function(event) {
            $($nav_switch_li).removeClass("active");
            $(this).addClass("active");

            restData = "";
            $("input").val("");

            switch ($(this).attr("data-search")) {
                case "episodes":
                    active = "episodes";
                    page = 1;
                    pushHistory(1, "episodes", null);
                    loadEpisodes(true);
                    break;
                case "series":
                    active = "series";
                    page = 1;
                    pushHistory(1, "series", null);
                    loadSeries(true);
                    break;
                default:
                    break;
            }
            $(".navbar-collapse").collapse('hide');
        });
        
        $($nav_switch_li).on("keypress", function(ev) {
            if (ev.which == 13 || ev.which == 32) {
                $($nav_switch_li).removeClass("active");
                $(this).addClass("active");

                restData = "";
                $("input").val("");

                switch ($(this).attr("data-search")) {
                    case "episodes":
                        active = "episodes";
                        page = 1;
                        pushHistory(1, "episodes", null);
                        loadEpisodes(true);
                        break;
                    case "series":
                        active = "series";
                        page = 1;
                        pushHistory(1, "series", null);
                        loadSeries(true);
                        break;
                    default:
                        break;
                }
                $(".navbar-collapse").collapse('hide');                
            }
        });      

        /* pagination */
        $($next).on("click", function() {
            if ($(this).hasClass("disabled")) {
                return;
            };

            if ($(this).hasClass("last")) {
                page = Math.floor(totalEntries / bufferEntries) + 1;
            } else {
                page++;
            }

            if (page > 1) {
                $($previous).removeClass("disabled");
            };

            if (active == "series") {
                pushHistory(page, "series", restData);
                loadSeries(true);
            } else if (active == "episodes") {
                pushHistory(page, "episodes", restData);
                loadEpisodes(true);
            };

        });
        
        $($next).on("keypress", function(ev) {
            if (ev.which == 13 || ev.which == 32) {
                if ($(this).hasClass("disabled")) {
                    return;
                };

                if ($(this).hasClass("last")) {
                    page = Math.floor(totalEntries / bufferEntries) + 1;
                } else {
                    page++;
                }

                if (page > 1) {
                    $($previous).removeClass("disabled");
                };

                if (active == "series") {
                    pushHistory(page, "series", restData);
                    loadSeries(true);
                } else if (active == "episodes") {
                    pushHistory(page, "episodes", restData);
                    loadEpisodes(true);
                };                
            }   
        });

        $($previous).on("click", function() {
            if ($(this).hasClass("disabled")) {
                return;
            };

            if ($(this).hasClass("first")) {
                page = 1;
            } else {
                --page;
            }

            if (page == 1) {
                $(this).addClass("disabled");
            };

            if (active == "series") {
                pushHistory(page, "series", restData);
                loadSeries(true);
            } else if (active == "episodes") {
                pushHistory(page, "episodes", restData);
                loadEpisodes(true);
            };
        });

        $($previous).on("keypress", function(ev) {
            if (ev.which == 13 || ev.which == 32) {
                if ($(this).hasClass("disabled")) {
                    return;
                };

                if ($(this).hasClass("first")) {
                    page = 1;
                } else {
                    --page;
                }

                if (page == 1) {
                    $(this).addClass("disabled");
                };

                if (active == "series") {
                    pushHistory(page, "series", restData);
                    loadSeries(true);
                } else if (active == "episodes") {
                    pushHistory(page, "episodes", restData);
                    loadEpisodes(true);
                };
            }
        });        

        /* handle search input */
        $($oc_search_form).submit(function(event) {
            event.preventDefault();
            var data = $(this).serialize();

            restData = data;
            page = 1;

            if (active == "series") {
                pushHistory(page, "series", restData);
                loadSeries(true);
            } else if (active == "episodes") {
                pushHistory(page, "episodes", restData);
                loadEpisodes(true);
            } else {
                pushHistory(page, "episodes", restData);
                loadEpisodes(true);
            }

            $(".navbar-collapse").collapse('hide');
        });

        $($oc_sort_dropdown).on("change", function() {
            log("submiting");
            $($oc_search_form).submit();
        });

        log("Handler registered");
    }

    function loadEpisodes(cleanGrid) {
        var requestUrl = restEndpoint + "episode.json?limit=" + bufferEntries +
            "&offset=" + ((page - 1)) * bufferEntries +
            "&" + restData;
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
                    var total = data["search-results"]["total"];
                    totalEntries = total;
                    if (data["search-results"] == undefined || total == undefined) {
                        log("Error: Search results (total) undefined");
                        $($main_container).append(msg_html_sthWentWrong);
                        return;
                    };

                    if (total == 0) {
                        $($main_container).append(msg_html_noepisodes);
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
        });
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
                seriesClass = "series" + data.mediapackage.series + " ";
            }

            var tile = mediaContainer + "<div class=\"tile\" id=\"" + serID + "\" role=\"menuitem\" tabindex=\"" + tabIndexNumber++ + "\">" +
                "<div class=\"" + seriesClass + "seriesindicator \"/> " +
                "<div class=\"tilecontent\">";

            tile = tile + "<h4 class=\"title\">" + data.dcTitle + "</h4>";

            // append thumbnail 
            var thumb = "";
            var time = 0;
            var creator = "<br>";
            var seriestitle = "<br>";
            var date = "<br>";

            if (data.mediapackage) {
                if (data.mediapackage.attachments && data.mediapackage.attachments.attachment[1].url) {
                    thumb = data.mediapackage.attachments.attachment[1].url;
                    tile = tile + "<div><img class=\"thumbnail img-responsive img-rounded\" src=\"" + thumb + "\"></div>";
                };

                tile = tile + "<div class=\"infos\">";

                if (data.dcCreator) {
                    creator = data.dcCreator;
                };
                tile = tile + "<div class=\"creator\">" + creator + "</div>";

                if (data.mediapackage.seriestitle) {
                    seriestitle = data.mediapackage.seriestitle;
                };
                tile = tile + "<div class=\"seriestitle\">" + seriestitle + "</div>";

                if (data.mediapackage.start) {
                    date = new Date(data.mediapackage.start);
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

                tile = tile + "</div></div></div></div>";

                $($main_container).append(tile);

                $("#" + data["id"]).on("click", function() {
                    $(location).attr("href", playerEndpoint + data["id"]);
                });

                $("#" + data["id"]).on("keypress", function(ev) {
                    if (ev.which == 13 || ev.which == 32) {
                        $(location).attr("href", playerEndpoint + data["id"]);
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
        if (data && data.id) {
            var seriesClass = "series" + data.id + " ";
            var color = generateSeriesColor(data.id);

            var creator = "<br>";
            var contributor = "<br>";

            var tile = mediaContainer + "<div class=\"tile\" id=\"" + data.id + "\" role=\"menuitem\" tabindex=\"" + tabIndexNumber++ + "\"> " +
                "<div class=\"" + seriesClass + "seriesindicator \"/> " +
                "<div class=\"tilecontent\">";

            tile = tile + "<h4 class=\"title\">" + (data.dcTitle ? data.dcTitle : "Unknown title") + "</h4>";

            if (data.dcCreator) {
                creator = data.dcCreator;
            };
            tile = tile + "<div class=\"creator\">" + creator + "</div>";

            if (data.dcContributor) {
                contributor = data.dcContributor;
            };
            tile = tile + "<div class=\"contributor\">" + contributor + "</div>";

            tile = tile + "</div></div></div>";

            $($main_container).append(tile);

            $("#" + data.id).on("click", function() {
                restData = "sid=" + data.id;
                page = 1;
                active = "episodes";
                $($navbarEpisodes).addClass("active");
                $($navbarSeries).removeClass("active");
                pushHistory(1, "episodes", restData);
                loadEpisodes(true);
            });
            
            $("#" + data.id).on("keypress", function(ev) {
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

    function loadSeries(cleanGrid) {
        var requestUrl = restEndpoint + "/series.json?limit=6&offset=" + (page - 1) * 6 + "&" + restData;
        $.ajax({
            url: requestUrl,
            dataType: "json",
            success: function(data2) {
                if (data2 && data2["search-results"] && data2["search-results"]["total"]) {
                    if (cleanGrid) {
                        $($main_container).empty();
                        window.scrollTo(0, 0);
                        tabIndexNumber=100;
                    }

                    var total = data2["search-results"]["total"];
                    totalEntries = total;

                    if (total == 0) {
                        $($main_container).append(msg_html_noseries);
                        return;
                    };

                    var result = data2["search-results"]["result"];

                    if (page == 1) {
                        $($previous).addClass("disabled");
                    };

                    if (result.length < 6 || total < 6) {
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
        });
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
