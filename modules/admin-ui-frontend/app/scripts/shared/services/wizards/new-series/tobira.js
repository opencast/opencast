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
'use strict';

angular.module('adminNg.services')
.factory('NewSeriesTobira', ['AuthService', 'Notifications', 'NewSeriesTobiraResource', function (AuthService,
  Notifications, NewSeriesTobiraResource) {
  var Tobira = function () {
    var me = this;
    var notifications = {};

    me.reset = function () {
      me.ud = {
        breadcrumbs: []
      };
      if (AuthService.userIsAuthorizedAs('ROLE_UI_SERIES_DETAILS_TOBIRA_EDIT')) {
        me.visible = true;
        me.goto({ path: '/' });
      } else {
        me.visible = false;
      }
    };

    me.isValid = function () {
      var valid = true;
      function check(type, key, context, callback) {
        var toggle = callback();
        toggleNotification(toggle, type, key, context, -1);
        if (toggle && type !== 'info') {
          valid = false;
        }
      }

      check('info', 'TOBIRA_OVERRIDE_NAME', 'series-tobira', function () {
        return me.ud.selectedPage && me.ud.selectedPage.title;
      });

      if (!me.editing) {
        clearNotifications('series-tobira-new');
        return valid;
      }

      var newPage = me.currentPage.children[me.currentPage.children.length - 1];

      check('warning', 'TOBIRA_NO_PATH_SEGMENT', 'series-tobira-new', function () {
        return !newPage.segment;
      });

      check('warning', 'TOBIRA_PATH_SEGMENT_INVALID', 'series-tobira-new', function () {
        return newPage.segment && (newPage.segment.length <= 1 || [
          // eslint-disable-next-line no-control-regex
          /[\u0000-\u001F\u007F-\u009F]/u,
          /[\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]/u,
          /[<>"[\\\]^`{|}#%/?]/u,
          /^[-+~@_!$&;:.,=*'()]/u
        ].some(function (regex) {
          return regex.test(newPage.segment);
        }));
      });

      check('warning', 'TOBIRA_PATH_SEGMENT_UNIQUE', 'series-tobira-new', function () {
        return me.currentPage.children.some(function (child) {
          return child != newPage && child.segment == newPage.segment;
        });
      });

      return valid;
    };

    me.goto = function (page) {
      function goto(page) {
        me.select(null);
        me.currentPage = page;
        me.ud.breadcrumbs.push(page);
      }

      clearNotifications('series-tobira');
      me.error = false;

      if (page.new) {
        goto(page);
      } else {
        NewSeriesTobiraResource.get(page, goto, function (response) {
          switch (response.status) {
          case 404:
            me.reset();
            // If we somehow lose our place in the navigation,
            // we just go back home. This can happen for example
            // if the page tree changes under us.
            addNotification('warning', 'TOBIRA_PAGE_NOT_FOUND', 'series-tobira', -1);
            break;
          case 503:
            me.visible = false;
            break;
          default:
            addNotification('error', 'TOBIRA_SERVER_ERROR', 'series-tobira', -1);
            me.error = true;
          }
        });
      }
    };

    me.back = function (index) {
      me.goto(
        me.ud.breadcrumbs.splice(index)[0]
      );
    };

    me.select = function (page) {
      if (!page || !page.new) {
        me.stopEditing();
      }
      if (!page || me.ud.selectedPage === page) {
        me.ud.selectedPage = null;
      } else {
        me.ud.selectedPage = page;
      }
    };

    me.addChild = function () {
      me.editing = true;
      var newPage = { new: true, children: [] };
      me.currentPage.children.push(newPage);
      me.select(newPage);
    };

    me.stopEditing = function () {
      if (me.editing) {
        me.currentPage.children.pop();
      }
      me.editing = false;
    };

    me.updatePath = function (page) {
      page.path = me.ud.breadcrumbs
        .concat(page).map(function (page) {
          return page.segment;
        })
        .join('/');
    };

    me.reset();


    // Wrappers around `Notifications` for easier handling
    function addNotification(type, key, context, duration) {
      var contextNotifications = notifications[context] || {};
      notifications[context] = contextNotifications;
      if (contextNotifications[key] == null) {
        contextNotifications[key] = Notifications.add(type, key, context, duration);
      }
    }

    function removeNotification(key, context) {
      var contextNotifications = notifications[context];
      if (!contextNotifications) {
        return;
      }
      var notification = contextNotifications[key];
      delete contextNotifications[key];
      Notifications.remove(notification, context);
    }

    function toggleNotification(toggle, type, key, context, duration) {
      if (toggle) {
        addNotification(type, key, context, duration);
      } else {
        removeNotification(key, context);
      }
    }

    function clearNotifications(context) {
      if (notifications[context]) {
        Notifications.removeAll(context);
        delete notifications[context];
      }
    }
  };

  return new Tobira();
}]);
