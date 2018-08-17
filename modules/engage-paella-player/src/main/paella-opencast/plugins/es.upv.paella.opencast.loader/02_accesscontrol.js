/*

User login, data and permissions: paella.AccessControl

Extend paella.AccessControl and implement the checkAccess method:

*/

var OpencastAccessControl = Class.create(paella.AccessControl, {
	_read: undefined,
	_write: undefined,
	_userData: undefined,
	
	canRead:function() {
		return paella.opencast.getEpisode()
		.then( () => { return paella_DeferredResolved(true); } )
		.catch(() => { return paella_DeferredResolved(false); } );
	},

	canWrite:function() {
		return paella.opencast.getUserInfo()
		.then(function(me) {		
			return paella.opencast.getACL()
			.then(function(acl){
				var canWrite = false;			
				var roles = me.roles;	
				if (!(roles instanceof Array)) { roles = [roles]; }
						
				if (acl.acl && acl.acl.ace) {
					var aces = acl.acl.ace;
					if (!(aces instanceof Array)) { aces = [aces]; }
	
					roles.some(function(currentRole) {					
						if (currentRole == me.org.adminRole) {
							canWrite = true;
						}
						else {				
							aces.some(function(currentAce) {
								if (currentRole == currentAce.role) {
									if (currentAce.action == "write") {canWrite = true;}
								}
								return (canWrite==true);								
							});
						}
						return (canWrite==true);
					});
				}
				return paella_DeferredResolved(canWrite);
			});
		});
	},

	userData:function() {
		var self = this;
		return new Promise((resolve, reject)=>{
			if (self._userData) {
				resolve(self._userData);
			}
			else {
				paella.opencast.getUserInfo().then(
					function(me) {
						var isAnonymous = ((me.roles.length == 1) && (me.roles[0] == me.org.anonymousRole));
						self._userData = {
							username: me.user.username,
							name: me.user.name || me.user.username || "",
							avatar: paella.utils.folders.resources() + '/images/default_avatar.png',
							isAnonymous: isAnonymous
						};
						resolve(self._userData);
					},
					function() {
						reject();		
					}
				);
			}
		});
	},

	getAuthenticationUrl:function(callbackParams) {
		return "auth.html?redirect="+encodeURIComponent(window.location.href);
	}
});
