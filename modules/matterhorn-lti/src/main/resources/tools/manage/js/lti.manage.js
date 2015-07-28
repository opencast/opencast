$(function(){
    var seriesID = $.getURLParameter("sid");

    ocRecordings.init();
    document.title = "Manage recordings";
    $("#searchBox").removeClass("ui-state-hover");
});
