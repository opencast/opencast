// jQuery File Tree Plugin
//
// Version 1.01
// 24 March 2008
// Visit http://abeautifulsite.net/notebook.php?article=58 for more information
//
// TERMS OF USE
// This plugin is dual-licensed under the GNU General Public License and the MIT License and
// is copyright 2008 A Beautiful Site, LLC. 
//
if(jQuery) (function($){
	
	$.ajax({
		url:'/config/rest',
		success: function(data) {
			//console.log(data);
			configs_tree = data;
	    }		
	});

	$.extend($.fn, {
		fileTree: function(o, h) {
			// Defaults
			if( !o ) var o = {};
			if( o.root == undefined ) o.root = '';
			if( o.folderEvent == undefined ) o.folderEvent = 'click';
			if( o.expandSpeed == undefined ) o.expandSpeed= 500;
			if( o.collapseSpeed == undefined ) o.collapseSpeed= 500;
			if( o.expandEasing == undefined ) o.expandEasing = null;
			if( o.collapseEasing == undefined ) o.collapseEasing = null;
			if( o.multiFolder == undefined ) o.multiFolder = true;
			if( o.loadMessage == undefined ) o.loadMessage = 'Loading...';

			console.log("configTree owner: " + o.owner);
			console.log("configTree vars: " + workflow_data);
			
			$(this).each( function() {
				
				function showTree(c, t) {
					$(c).addClass('wait');
					$(".jqueryFileTree.start").remove();
					
					var data = "<ul class=\"jqueryFileTree\" style=\"display: none;\">";
					
					for (file in configs_tree) {
						
						if (configs_tree[file]["parent_id"] == t) {
							
							if (configs_tree[file]["type"] == "file") {
								
								data += "<li class=\"file ext_" + "file" + "\"><a href=\"#\" rel=\"" + configs_tree[file]["parent_id"] 
								+ "/" + configs_tree[file]["id"] + "\">"
								+ configs_tree[file]["id"] + "</a></li>"
							
							}
							
							else {
								data += "<li class=\"directory collapsed\"><a href=\"#\" rel=\"" + configs_tree[file]["parent_id"]
								+ configs_tree[file]["id"] + "\">"
								+ configs_tree[file]["id"] + "</a></li>";
							}
						}
					}
					
					data += "</ul>";
					
					bindTree(c);
					$(c).find('.start').html('');
					$(c).removeClass('wait').append(data);
					if( o.root == t ) 
						$(c).find('UL:hidden').show(); 
					else 
						$(c).find('UL:hidden').slideDown({ duration: o.expandSpeed, easing: o.expandEasing });
					bindTree(c);	
				}
				
				function bindTree(t) {
					$(t).find('LI A').bind(o.folderEvent, function() {
						if( $(this).parent().hasClass('directory') ) {
							if( $(this).parent().hasClass('collapsed') ) {
								// Expand
								if( !o.multiFolder ) {
									$(this).parent().parent().find('UL').slideUp({ duration: o.collapseSpeed, easing: o.collapseEasing });
									$(this).parent().parent().find('LI.directory').removeClass('expanded').addClass('collapsed');
								}
								$(this).parent().find('UL').remove(); // cleanup
								showTree( $(this).parent(), $(this).attr('rel'));
								$(this).parent().removeClass('collapsed').addClass('expanded');
							} else {
								// Collapse
								$(this).parent().find('UL').slideUp({ duration: o.collapseSpeed, easing: o.collapseEasing });
								$(this).parent().removeClass('expanded').addClass('collapsed');
							}
						} else {
							h($(this).attr('rel'));
						}
						return false;
					});
					// Prevent A from triggering the # on non-click events
					if( o.folderEvent.toLowerCase != 'click' ) $(t).find('LI A').bind('click', function() { return false; });
				}
				// Loading message
				$(this).html('<ul class="jqueryFileTree start"><li class="wait">' + o.loadMessage + '<li></ul>');
				// Get the initial file list
				showTree( $(this), escape(o.root) );
			});
		}
	});
	
})(jQuery);
