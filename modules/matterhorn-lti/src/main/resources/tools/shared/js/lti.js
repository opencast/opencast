$(function(){
    var seriesID = $.getURLParameter("sid"),
    pageURL = $(location).attr('href'),
    uploadSetting = $.getURLParameter("upload");


    function setupLinks(){
        // Set the href attribute for all the links
        $(".lti_links > ul > li").each(function(){
            var urls = {
                Recordings : "/ltitools/courses/index.html?",
                Manage : "/ltitools/manage/index.html?",
                Upload : "/ltitools/upload/index.html?",
                Schedule : "/ltitools/schedule/index.html?"
            },
            images = {
                Recordings : "/ltitools/shared/img/icons/recordings.png",
                Manage : "/ltitools/shared/img/icons/manage.png",
                Upload : "/ltitools/shared/img/icons/upload.png",
                Schedule : "/ltitools/shared/img/icons/schedule.png"
            };

            label = $(this).find("a").text();
            $(this).find("a").attr( "href", urls[label] + "sid="+ seriesID + "&upload=" + uploadSetting);
            $(this).find("a").prepend( $("<img src=' " + images[label] +"'></img>") );

            // Grey out the URL if you are on the page
            if ( pageURL.indexOf(urls[label]) > 0 ){
                $(this).find("a").attr('href', "").css({'color' : "grey", 'text-decoration' : "none"});
            }

            //Set the class to "instructor" for some of the links
            if(label !== "Recordings"){
                $(this).find("a").addClass("instructor");
            }
        });
    }

    // Hide Upload if the setting is not true
    function hideUploadLink(){
        if(uploadSetting !== "true"){
            $(".lti_links > ul > li:nth-child(3)").hide();
        }
    }

    // Fix cancel so that it redirects to previous page
    function setCancelURL(){
        $("#cancelButton").click(function(){
            window.location.href = "/ltitools/manage/index.html?sid=" + seriesID + "&upload=" + uploadSetting;
        });
    }

    // Check for Series write rights
    function checkSeriesRights(){
        $.ajax({
            url: "/info/me.json",
                dataType: 'json',
                success: function(data)
                {
                    var user_roles = data.roles || [];
                    $.ajax({
                            url: "/series/"+ seriesID +"/acl.json",
                                dataType: 'json',
                                success: function(acl_data)
                                {
                                    var series_roles = acl_data.acl.ace || [];
                                    var series_write = "false";

                                    $.each( user_roles, function(i, value){
                                            for(var i=0; i < series_roles.length; i+=1){
                                                if( value === series_roles[i].role && series_roles[i].action === "write" && series_roles[i].allow === true){
                                                    series_write = "true";
                                                }
                                            }
                                        });

                                    if(series_write === "true"){
                                        setupLinks();
                                        $(".instructor").show();
                                    }else{
                                        $(".lti_links").hide();
                                    }
                                }
                        });
                }
        });
    }

    function toggleAddInfo(){
        $("#i18n_additional").click(function(){
            var content$ = $("#more_info > li:nth-child(4)");
            content$.toggle('fast', function(){
                if( content$.is(':hidden') ){
                    $("#i18n_additional").html("<img src='/ltitools/shared/img/icons/dropdown_down.png'></img><span>Add more information</span>")
                }else{
                    $("#i18n_additional").html("<img src='/ltitools/shared/img/icons/dropdown_up.png'></img><span>Add more information</span>");
                }
            });
        });
    }

    hideUploadLink();
    checkSeriesRights();
    setCancelURL();
    toggleAddInfo();
});
