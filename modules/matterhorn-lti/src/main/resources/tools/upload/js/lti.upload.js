(function($){

    function initialize(){
        ocUpload.init();
        document.title = "Upload recording";
    }

    function hideFields(){
	changeHTML("#i18n_additional", "<img src='/ltitools/shared/img/icons/dropdown_down.png'></img><span>Add more information</span>");
        // Hide the processing div
        $("#processingRecording").hide();

        // Hide the course and series div
        $("#seriesContainer").hide();

        // Hide Add more information fields
        $("#additional-description  ul > li:nth-child(1)").hide();
        $("#additional-description  ul > li:nth-child(2)").hide();
        $("#additional-description  ul > li:nth-child(3)").hide();
        $("#additional-description  ul > li:nth-child(5)").hide();

        $("#upload_option").hide();
        $("#i18n_upload_title").hide()
        $("#regularFileSelection").hide();
        $("#uploadContainerSingle > ul>li:nth-child(3)").hide();
        $("#uploadContainerSingle > ul>li:nth-child(4)").hide();
        $("#uploadContainerSingle > ul>li:nth-child(5)").hide();
    }

    function autosetSeries(){
        var seriesID = $.getURLParameter("sid");
        $("#series").val(seriesID);
        $.ajax({
            url: '/series/'+seriesID+'.json',
                dataType: 'json',
                success: function(data)
                {
                    var media = data["http://purl.org/dc/terms/"] ? data["http://purl.org/dc/terms/"]["title"] : [],
                        series_title;

                    if(media) {
                        series_title = media[0]["value"];
                        $('#seriesSelect').val(series_title);
                    }
                }
        });
    }

    function setCancelURL(){
        // Fix cancel so that it redirects to previous page
        $("#cancelButton").click(function(){
            history.back();
        });
    }

    function changeHTML(selector, theHTML){
        $( selector ).html( theHTML );
    }

    initialize();
    hideFields();
    autosetSeries();
    setCancelURL();

})(jQuery);
