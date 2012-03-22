// Input binder plugin
// -------------------
// Plugin to help to bind input together 
//=require <jquery>

(function($){
	// Default parameters
	var parameters = {
		subject: true,
		rebindButton: '<button type="button" class="rebind" title="rebound this input to master element"></button>',
		unboundClass: 'unbound'
	};
	
	$.fn.boundinput = function(boundClass,options){
		var self = this;
		var subject,listener;
		
		if(!this.length)
			return;
		
		this._unbind = function(element){
			
			if(!element.hasClass(parameters.unboundClass)){
				element.addClass(parameters.unboundClass);
				
				// Add a button to rebind it to the master input
				element.after(parameters.rebindButton).next().click(function(){
					element.removeClass(parameters.unboundClass);
					element.val(subject.val());
					element.trigger('change', true);
					$(this).remove();
				});
			}
			// Rebind to master input if same value
			else if(element.val() == subject.val()) {
				element.removeClass(parameters.unboundClass);
				element.next().remove();
			}	
		};
		
		// Set parameters with options, if there is some given
		$.extend(parameters, options||{});
		
		// Define if the aimed element is 
		if(parameters.subject){
			subject = this;
			listener = $(boundClass);
		}
		else{
			subject = $(boundClass);
			listener = this;
		}
			
		// Bind the master element with its listener
		subject.bind('keyup change',function(){
			listener.not('.'+parameters.unboundClass).val(subject.val()).trigger('change', true);
		});

		// Unbind already filled fields
		listener.each(function(index, value){
			var test = $(value).val();
			var test2 = subject.val();
			if($(value).val() != "" && $(value).val() != subject.val()){
				self._unbind($(value));
			}
		});
		
		// Unbound listener input after individual edition
		listener.bind('keyup change',function(event,fromSelf){
				if(fromSelf==undefined)
					self._unbind($(this));
		});
	};
})(jQuery);