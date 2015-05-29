(function($){
    var seriesID = $.getURLParameter("sid");
    edit = $.getURLParameter("edit");

    function initialize(){
        ocScheduler.init();
        document.title = "Schedule recording";

        if( edit !== 'true' ){
            triggerCheck();
        }

        hideFields();
        autosetSeries();
        changeHTML("#i18n_additional", "<img src='/ltitools/shared/img/icons/dropdown_down.png'></img><span>Add more information</span>");
        changeInformationLabel("#i18n_agent_label", "Venue");
        changeInformationLabel("#recurAgentLabel > span:nth-child(2)", "Venue");
        changeInformationLabel("#i18n_day_short_mon", "Mon");
        changeInformationLabel("#i18n_day_short_tue", "Tues");
        changeInformationLabel("#i18n_day_short_wed", "Wed");
        changeInformationLabel("#i18n_day_short_thu", "Thurs");
        changeInformationLabel("#i18n_day_short_fri", "Fri");
        setUpLinks();
        setDefaultDuration("#durationMin");
        setDefaultDuration("#recurDurationMin");
    }

    function triggerCheck(){
        $("#agent").change(function(){
            $("#trimHold").attr("checked", "checked");
        });
        $("#recurAgent").change(function(){
            $("#trimHold").attr("checked", "checked");
        });
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

    function hideFields(){
        // Hide the processing div
        $("#processingScheduler").hide();

        // Hide the course and series div
        $("#seriesContainer").hide();

        // Hide Add more information fields
        $("#more_info > li:nth-child(1)").hide();
        $("#more_info > li:nth-child(2)").hide();
        $("#more_info > li:nth-child(3)").hide();
        $("#more_info > li:nth-child(5)").hide();

        //Hide the Day options
        $("#i18n_recording_date").hide();
        $("#i18n_day_short_sun").hide();
        $("#repeatSun").parent().hide();
        $("#i18n_day_short_sat").hide();
        $("#repeatSat").parent().hide();

        //Hide the inputs
        $("#i18n_input_label").closest("li").hide();
        $("#recurInputList").closest("li").hide();

        //Hide the capture header
        $("#singleRecordingPanel div:nth-child(1)").hide()
        $("#recurringRecordingPanel div:nth-child(1)").hide()

        //Hide the Repeats Weekly li
        $("#scheduleRepeat").parent().hide()
    }

    function changeInformationLabel(selector, label){
        $(selector).text(label);
    }

    function setUpLinks(){
        $("#manage_recordings").live('click', function(event){
            $(this).attr("href", "../manage/index.html?sid="+seriesID);
        });
    }

    function changeHTML(selector, theHTML){
        $( selector ).html( theHTML );
    }

    function setDefaultDuration(selector){
        $.each($(selector + " > option"), function(){
            if ( $(this).val() === "50" ){
                $(this).attr('selected','selected');
            }
        });
    }

    initialize();
})(jQuery);
