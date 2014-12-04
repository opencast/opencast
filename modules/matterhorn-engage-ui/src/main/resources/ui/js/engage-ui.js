$(document).ready(function() {
    var restEndpoint = "/search/"
    var mediaContainer = '<div class="col-xs-12 col-sm-6 col-md-4 col-lg-4">'
    var playerEndpoint = "/engage/";
    var page = 1;
    var restData = "";
    var active = "episodes";
    var stack = new Array();
    var visited = 1;

    var title_enterUsernamePassword = "Login";
    var placeholder_username = "Username";
    var placeholder_password = "Password";
    var placeholder_rememberMe = "Remember me";
    var msg_enterUsernamePassword = "Please enter your username and password:";
    var msg_html_sthWentWrong = "<h2> Something went wrong. Try again! </h2>";
    var msg_html_noepisodes = "<h2>No Episodes available</h2>";
    var msg_html_noseries = "<h2>No Series available</h2>";
    var msg_html_loading = "<h2>Loading...</h2>";
    var msg_html_mediapackageempty = "<h2>The mediapackage is empty</h2>";
    var msg_html_nodata = "<h2>No data available</h2>";
    var msg_loginSuccessful = "Successfully logged in. Please reload the page if the page does not reload automatically.";
    var msg_loginFailed = "Failed to log in.";
    var infoMeURL = "/info/me.json";
    var corePlayerURL = "theodul/ui/core.html?id=";
    var oldPlayerURL = "ui/watch.html?id=";
    var springSecurityLoginURL = "/j_spring_security_check";
    var springSecurityLogoutURL = "/j_spring_security_logout";
    var springLoggedInStrCheck = "<title>Opencast Matterhorn – Login Page</title>";
    var $navbarEpisodes = "#navbarEpisodes";
    var $navbarSeries = "#navbarSeries";
    var $nav_userName = "#nav-userName";
    var $nav_login = "#nav-login";
    var $nav_dropdownLoggedin = "#nav-dropdownLoggedin";
    var $navbarLogin = "#navbarLogin";
    var $nav_logoutLink = "#nav-logoutLink";
    var $headerLogo = "#headerLogo";
    var $nav_switch_li = "#nav-switch li";
    var $oc_search_form = "#oc-search-form";
    var $main_container = "#main-container";
    var $next = ".next";
    var $previous = ".previous";
    var askedForLogin = false;
    var checkLoggedOut = false;

    function initialize() {
        $.enableLogging(true);

        getInfo();
        registerHandler();

        var retrievedObject = sessionStorage.getItem("historyStack");
        if (retrievedObject != null) {
            stack = JSON.parse(retrievedObject);
            $.log("Retrieved history stack from session storage");
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
        loadEpisodes();
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
            loadEpisodes();
        };
        if (dest.active == "series") {
            $($navbarSeries).addClass("active");
            $($navbarEpisodes).removeClass("active");
            active = "series";
            loadSeries();
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
	if(!askedForLogin) {
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
			callback: function () {
			    askedForLogin = false;
			}
		    },
		    login: {
			label: "Log in",
			className: "btn-success",
			callback: function () {
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

    function setUsername(name) {
        $($nav_userName).html(name);
        $($nav_login).hide();
        $($nav_dropdownLoggedin).show();
    }

    function initLogin() {
        $($navbarLogin).click(function() {
            $($nav_login).click(login);
        });
    }

    function resetAnonymousUser() {
        $($nav_userName).html("");
        $($nav_dropdownLoggedin).hide();
        $($nav_login).show();
        initLogin();
    }

    String.prototype.endsWith = function(suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };

    function getInfo() {
        $.ajax({
            url: infoMeURL,
            dataType: "json",
            success: function(data) {
                $.log("User information loaded");

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
                            $($nav_logoutLink).click(logout);
                            $.log("User is not anonymous");
                            if (data.username) {
                                $.log("Username found: " + data.username);
                                setUsername(data.username);
                            } else {
                                $.log("Username not found");
                            }
                        } else {
                            checkLoggedOut = false;
                            $.log("User is anonymous");
                            resetAnonymousUser();
                        }
                    } else {
                        $.log("Error: No role");
                        resetAnonymousUser();
                    }
                    if (data.org && data.org.properties) {
                        var logo = data.org.properties.logo_large ? data.org.properties.logo_large : "";
                        $($headerLogo).attr("src", logo);

                        var player = data.org.properties.player ? data.org.properties.player : "";
                        if (player == "theodul") {
                            playerEndpoint = playerEndpoint + corePlayerURL;
                        } else {
                            playerEndpoint = playerEndpoint + oldPlayerURL;
                        }
                    } else {
                        $.log("Error: No info data received.");
                        resetAnonymousUser();
                    }
                }

                $.log("Chosen player: " + player);
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
                    loadEpisodes();
                    break;
                case "series":
                    active = "series";
                    page = 1;
                    pushHistory(1, "series", null);
                    loadSeries();
                    break;
                default:
                    break;
            }
        });

        /* pagination */
        $($next).on("click", function() {
            if ($(this).hasClass("disabled")) {
                return;
            };

            page++;

            if (page > 1) {
                $($previous).removeClass("disabled");
            };

            if (active == "series") {
                pushHistory(page, "series", restData);
                loadSeries();
            } else if (active == "episodes") {
                pushHistory(page, "episodes", restData);
                loadEpisodes();
            };

        });

        $($previous).on("click", function() {
            if ($(this).hasClass("disabled")) {
                return;
            };

            --page;
            if (page == 1) {
                $(this).addClass("disabled");
            };

            if (active == "series") {
                pushHistory(page, "series", restData);
                loadSeries();
            } else if (active == "episodes") {
                pushHistory(page, "episodes", restData);
                loadEpisodes();
            };
        });

        /* handle search input */
        $($oc_search_form).submit(function(event) {
            event.preventDefault();
            var data = $(this).serialize();

            restData = data;
            page = 1;

            if (active == "series") {
                pushHistory(page, "series", restData);
                loadSeries();
            } else if (active == "episodes") {
                pushHistory(page, "episodes", restData);
                loadEpisodes();
            } else {
                pushHistory(page, "episodes", restData);
                loadEpisodes()
            }

        });
        $.log("Handler registered");
    }

    function loadEpisodes() {
        var requestUrl = restEndpoint + "episode.json?limit=6&offset=" + ((page - 1)) * 6 + "&" + restData;
        $.ajax({
            url: requestUrl,
            dataType: "json",
            success: function(data) {
                // clear main grid
                $($main_container).empty();

                if (data && data["search-results"] && data["search-results"]["total"]) {
                    // number of total search results
                    var total = data["search-results"]["total"];
                    if (data["search-results"] == undefined || total == undefined) {
                        $.log("Error: Search results (total) undefined");
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

                    if (result.length < 6 || total < 6) {
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
                $.log("Error: Episode with no ID.")
                serID = "0";
            };

            var tile = mediaContainer + "<div class=\"tile\" id=\"" + serID + "\">";

            tile = tile + "<h4 class=\"title\">" + data.dcTitle + "</h4>";

            // append thumbnail 
            var thumb = "";
            var time = 0;
            var color = "A6A6A6";
            var creator = "<br>";
            var seriestitle = "<br>";
            var date = "<br>";

            if (data.mediapackage) {
                if (data.mediapackage.attachments && data.mediapackage.attachments.attachment[1].url) {
                    thumb = data.mediapackage.attachments.attachment[1].url;
                    tile = tile + "<div><img class=\"img-responsive img-rounded\" src=\"" + thumb + "\"></div>";
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
                        seconds = "0" + seconds
                    };
                    if (minutes < 10) {
                        minutes = "0" + minutes
                    };
                    if (hours < 10) {
                        hours = "0" + hours
                    };

                    tile = tile + "<div class=\"duration\">" + hours + ":" + minutes + ":" + seconds + "</div>";
                };

                tile = tile + "</div>";

                if (data.mediapackage.series) {
                    Math.seedrandom(data.mediapackage.series);
                    color = Math.ceil(Math.random() * 1000000);
                };

                tile = tile + "</div></div>";

                $($main_container).append(tile);

                $("#" + data["id"]).on("click", function() {
                    $(location).attr("href", playerEndpoint + data["id"]);
                });
            } else {
                $($main_container).html(msg_html_mediapackageempty);
            }
        } else {
            $($main_container).html(msg_html_nodata);
        }

        /* TODO: Swipe events etc. for mobile devices */
        //registerMobileEvents();
    }

    function createSeriesGrid(data) {
        if (data && data.id) {
            var tile = mediaContainer + "<div class=\"tile\" id=\"" + data.id + "\">";

            tile = tile + "<h4 class=\"title\">" + (data.dcTitle ? data.dcTitle : "Unknown title") + "</h4>";

            tile = tile + "</div></div>";

            // Set Color. TODO: Better generator, use series color for ep.
            Math.seedrandom(data.id);
            var color = Math.ceil(Math.random() * 1000000);

            $($main_container).append(tile);

            $("#" + data.id).on("click", function() {
                restData = "sid=" + data.id;
                page = 1;
                active = "episodes";
                $($navbarEpisodes).addClass("active");
                $($navbarSeries).removeClass("active");
                pushHistory(1, "episodes", restData);
                loadEpisodes();
            });
        } else {
            $.log("Error: Data for creating series grid is empty.");
        }
    }

    function loadSeries() {
        var requestUrl = restEndpoint + "/series.json?limit=6&offset=" + (page - 1) * 6 + "&" + restData;
        $.ajax({
            url: requestUrl,
            dataType: "json",
            success: function(data2) {
                if (data2 && data2["search-results"] && data2["search-results"]["total"]) {
                    $($main_container).empty();

                    var total = data2["search-results"]["total"];

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

    function registerMobileEvents() {
        // TODO
    }
});
