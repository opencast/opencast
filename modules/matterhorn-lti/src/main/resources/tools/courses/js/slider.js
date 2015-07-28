$(function(){
    $("#gallery-prev").live("click", function(){
        var imageWidth = $("#gallery > li:first").outerWidth(true),
            visibleImages = Math.round($("#gallery-wrap").width() / imageWidth),
            visibleWidth = visibleImages * imageWidth;

        //If the first visible images are in view.
        if( ($("#gallery").position().left + visibleWidth) === 0 ){
            $(this).attr("disabled", "disabled");
            $(this).children().css("opacity", 0.4);
            $("#gallery-next").removeAttr('disabled');
            $("#gallery-next > img").removeAttr('style');
        }else{
            $(this).removeAttr('disabled');
            $(this).children().removeAttr('style');
            $("#gallery-next").removeAttr('disabled');
            $("#gallery-next > img").removeAttr('style');
        }

        if($("#gallery").position().left < 0 && !$("#gallery").is(":animated")){
            $("#gallery").animate({left : "+=" + visibleWidth + "px"});
        }
        return false;
    });

    $("#gallery-next").live("click", function(){
        var totalImages = $("#gallery > li").length,
        imageWidth = $("#gallery > li:first").outerWidth(true),
        totalWidth = imageWidth * totalImages,
        visibleImages = Math.round($("#gallery-wrap").width() / imageWidth),
        visibleWidth = visibleImages * imageWidth,
        stopPosition = (visibleWidth - totalWidth);

        if($("#gallery").position().left > stopPosition && !$("#gallery").is(":animated")){
            $("#gallery").animate({left : "-=" + visibleWidth + "px"});
        }

        // If the last images are in view
        if( ($("#gallery").position().left - visibleWidth) <= stopPosition ){
            $(this).attr("disabled", "disabled");
            $(this).children().css("opacity", 0.4);
            $("#gallery-prev").removeAttr('disabled');
            $("#gallery-prev > img").removeAttr('style');
        }else{
            $(this).removeAttr('disabled');
            $(this).children().removeAttr('style');
            $("#gallery-prev").removeAttr('disabled');
            $("#gallery-prev > img").removeAttr('style');
        }
        return false;
    });

    $("#gallery li").live("hover", function(){
        $(this).toggleClass('hovereffect');
    });
});
