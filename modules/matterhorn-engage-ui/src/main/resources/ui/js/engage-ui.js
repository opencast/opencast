$(document).ready(function(){

	/* Declare some Variables */
	var restEndpoint = "/search/"

	var mediaContainer = '<div class="col-xs-12 col-sm-6 col-md-4 col-lg-4">'

	var playerEndpoint = "/engage/";

	var page = 1;

	var restData = "";

	var active = "episodes";

	var stack = new Array();

	var visited = 1;

	$(window).load(function(){
		getInfo();
		registerHandler();
		var retrievedObject = sessionStorage.getItem('historyStack');
		if (retrievedObject != null) {
			stack = JSON.parse(retrievedObject);
		}else{
			stack.push({"page":1, "active":"episodes", "rest":null});
		}
		loadEpisodes();
	});

	$(window).on('popstate', function (event) {

		if (window.history.state == null && stack.length == 1) {return};

		console.log(window.history.state);

		var choose = window.history.state - 1;
		if (choose < 0) {
			choose = 0;
		}; 

		var dest = stack[choose];

		page = dest.page;
		restData = dest.rest;

		$('input').val('');

		if (dest.active == "episodes") {
			$('#navbarEpisodes').addClass('active');
			$('#navbarSeries').removeClass('active');
			active = "episodes";
			loadEpisodes();
		};
		if (dest.active == "series") {
			$('#navbarSeries').addClass('active');
			$('#navbarEpisodes').removeClass('active');
			active = "series";
			loadSeries();
		};

	})

	function pushHistory (page, active, rest) {
		stack.push({"page":page, "active":active, "rest":rest});
		visited++;
		history.pushState(visited, '', '');
		sessionStorage.setItem('historyStack', JSON.stringify(stack));
	}
	/* Get Infos */
	function getInfo () {
		$.ajax({
			url: '/info/me.json',
			dataType: 'json',
			success: function (data) {
				var logo = data.org.properties.logo_large;
				var player = data.org.properties.player;

				$('.page-header h1').before("<img src='"+logo+"' class='img-responsive'>");

				/* Choose Player */
				if (player == "theodul") {
					playerEndpoint = playerEndpoint + "theodul/ui/core.html?id=";
				}
				else if(player == "paella") {

				}
				else{
					playerEndpoint = playerEndpoint + "ui/watch.html?id=";
				}
			
			}
		})
	}

	function registerHandler () {
		/* Register Handler for Navbar */
		$('#nav-switch li').click(function (event) {
			$('#nav-switch li').removeClass('active');
			$(this).addClass('active');

			restData = "";
			$('input').val('');

			switch($(this).attr('data-search')){
				case 'episodes': 
					active = "episodes";
					page = 1;
					pushHistory(1, "episodes", null);
					loadEpisodes(); 
				break;
				case 'series': 
					active = "series";
					page = 1;
					pushHistory(1, "series", null);
					loadSeries();
				break;
				default: 
			}
		});

		/* Pagination */
		$('.next').on("click", function () {

			if ($(this).hasClass('disabled')) {
				return;
			};

			page++;

			if (page > 1) {
				$('.previous').removeClass('disabled');
			};

			if (active == "series") {
				pushHistory(page, "series", restData);
				loadSeries();
			} else if (active == "episodes") {
				pushHistory(page, "episodes", restData);
				loadEpisodes();
			};

		});
		
		$('.previous').on("click",function () {
			if ($(this).hasClass('disabled')) {
				return;
			};

			page--;
			if (page == 1) {
				$(this).addClass('disabled');
			};

			if (active == "series") {
				pushHistory(page, "series", restData);
				loadSeries();
			} else if (active == "episodes") {
				pushHistory(page, "episodes", restData);
				loadEpisodes();
			};
		});

		/* Handle Search Input */
		$('#oc-search-form').submit(function (event) {
			event.preventDefault();
			var data = $(this).serialize();

			restData = data;
			page = 1;

			if (active == "series") {
				pushHistory(page, "series", restData);
				loadSeries();
			}else if(active == "episodes"){
				pushHistory(page, "episodes", restData);
				loadEpisodes();
			}else{
				pushHistory(page, "episodes", restData);
				loadEpisodes()
			}

		});
	}

	function loadEpisodes (data) {
		var requestUrl = restEndpoint + "episode.json?limit=6&offset="+(page-1)*6+"&" + restData;
		$.ajax({
			url: requestUrl,
			dataType: "json",
			success: function(data){

				// Clear Main Grid
				$('#main-container').empty();

				// Number of total Search Results
				var total = data["search-results"]["total"];

				// Keine Episoden vorhanden
				if (total == 0) {
					$('#main-container').append("<h2> No Episodes </h2>");
					return;
				};

				// Results
				var result = data["search-results"]["result"];

				if (page == 1) {
					$('.previous').addClass('disabled');
				};

				if (result.length < 6 || total < 6) {
					$('.next').addClass('disabled');
				}else {
					$('.next').removeClass('disabled');
				}

				if (total == 1) {
					buildGrid(result);
					return;
				};

				$.each(result, function (index, val) {
					buildGrid(val);
				});
			}
		});
	}

	function buildGrid (data) {
		var tile = mediaContainer + '<div class="tile" id="'+data["id"]+'">';

		tile = tile + '<h4 class="title">' + data.dcTitle + "</h4>";

		// Append Thumbnail 
		var thumb = "";
		var time = 0;
		var color = "A6A6A6";
		var creator = "\n";
		var seriestitle = "\n";
		var date = "\n";


		if(data.mediapackage.attachments.attachment[1].url){
			thumb = data.mediapackage.attachments.attachment[1].url;
			tile = tile + "<div><img class='img-responsive img-rounded' src='"+thumb+"'></div>";
		};

		tile = tile + "<div class='infos'>";

		if (data["dcCreator"]) {
			creator = data["dcCreator"];
			tile = tile + "<div class='creator'>"+creator+"</div>";
		};

		if (data.mediapackage.seriestitle) {
			seriestitle = data.mediapackage.seriestitle;
			tile = tile + "<div class='seriestitle'>"+seriestitle+"</div>";
		};

		if (data.mediapackage["start"]) {
			date = new Date(data.mediapackage["start"]);
			tile = tile + "<div class='date'>"+date.toLocaleDateString()+"</div>";
		};

		if(data["mediapackage"]["duration"]){
			time = data["mediapackage"]["duration"];
			var seconds = Math.floor((time/1000)%60);
			var minutes = Math.floor((time/(1000*60)%60));
			var hours = Math.floor((time/(1000*60*60)%60));

			if (seconds < 10) {seconds = "0"+seconds};
			if (minutes < 10) {minutes = "0"+minutes};
			if (hours < 10) {hours = "0"+hours};

			tile = tile + "<div class='duration'>"+hours+":"+minutes+":"+seconds+"</div>";
		};

		tile = tile + "</div>";

		if (data.mediapackage.series) {
			Math.seedrandom(data.mediapackage.series);
			color = Math.ceil(Math.random() * 1000000);
		};


		tile = tile + '</div></div>';

		$('#main-container').append(tile);

		$('#'+data["id"]).css("background", "linear-gradient(to bottom, #"+color+" 10%,#FFFFFF 10%,#FFFFFF 50%,#FFFFFF 100%)");

		$('#'+data["id"]).on('click', function () {
			$(location).attr('href',playerEndpoint+data["id"]);
		});

		$('#'+data["id"]).on('mouseenter', function () {
			$(this).css("background", "linear-gradient(to top, #FBB900 90%, #FFFFFF 90%, #FFFFFF 10%,#"+color+" 10%)");
		});
		$('#'+data["id"]).on('mouseleave', function () {
			$(this).css("background", "linear-gradient(to bottom, #"+color+" 10%,#FFFFFF 10%, #FFFFFF 50%,#FFFFFF 100%)");
		});

		registerMobileEvents();
	}

	function createSeriesGrid (data) {
		var tile = mediaContainer + '<div class="tile" id="'+data["id"]+'">';

		tile = tile + '<h4 class="title">' + data.dcTitle + "</h4>";

		tile = tile + '</div></div>';

		// Set Color. TODO: Better Generator, use Series Color for Ep.
		Math.seedrandom(data["id"]);
		var color = Math.ceil(Math.random() * 1000000);

		$('#main-container').append(tile);
		$('#'+data["id"]).css("background", "linear-gradient(to bottom, #"+color+" 10%,#FFFFFF 10%,#FFFFFF 50%,#FFFFFF 100%)");

		$('#'+data["id"]).on('click', function () {
			restData = "sid="+data["id"];
			page = 1;
			active = "episodes";
			$('#navbarEpisodes').addClass('active');
			$('#navbarSeries').removeClass('active');
			pushHistory(1, "episodes", restData);
			loadEpisodes();
		});
	}

	function loadSeries (data) {
		var requestUrl = restEndpoint + "/series.json?limit=6&offset=" + (page-1)*6 + "&" +restData;
		$.ajax({
			url: requestUrl,
			dataType: "json",
			success: function(data){

				// Clear Main Grid
				$('#main-container').empty();

				// Number of total Search Results
				var total = data["search-results"]["total"];

				// Keine Episoden vorhanden
				if (total == 0) {
					$('#main-container').append("<h2> No Series </h2>");
					return;
				};

				// Results
				var result = data["search-results"]["result"];

				if (page == 1) {
					$('.previous').addClass('disabled');
				};

				if (result.length < 6 || total < 6) {
					$('.next').addClass('disabled');
				}else {
					$('.next').removeClass('disabled');
				}
				
				if (total == 1) {
					createSeriesGrid(result);
					return;
				};

				$.each(result, function (index, val) {
					createSeriesGrid(val);
				});
			}
		});
	}

	function registerMobileEvents() {
		$('.tile').on('taphold', function(){
			alert("Alarm");
		});
	}
});