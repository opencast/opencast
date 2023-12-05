/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

/*

User login, data and permissions: paella.AccessControl

Extend paella.AccessControl and implement the checkAccess method:

*/
/*global paella_DeferredResolved*/

class OpencastAccessControl extends paella.AccessControl {

  constructor() {
    super();
    this._read = undefined,
    this._write = undefined,
    this._userData = undefined;
  }

  getName() { return 'es.upv.paella.opencast.OpencastAccessControl'; }

  canRead() {
    return paella.opencast.getEpisode()
    .then( () => { return paella_DeferredResolved(true); } )
    .catch(() => { return paella_DeferredResolved(false); } );
  }

  canWrite() {
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
                  if (currentAce.action == 'write') {canWrite = true;}
                }
                return (canWrite == true);
              });
            }
            return (canWrite == true);
          });
        }
        return paella_DeferredResolved(canWrite);
      });
    });
  }

  userData() {
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
              name: me.user.name || me.user.username || '',
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
  }

  getAuthenticationUrl(callbackParams) {
    return 'auth.html?redirect=' + encodeURIComponent(window.location.href);
  }
}
