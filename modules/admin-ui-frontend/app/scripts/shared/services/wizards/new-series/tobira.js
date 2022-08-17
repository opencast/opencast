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
    var validationNotifications = {};
    var serverNotifications = {};

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
      if (!me.editing) {
        clearNotifications(validationNotifications, 'series-tobira-new');
        return true;
      }

      var valid = true;
      var newPage = me.currentPage.children[me.currentPage.children.length - 1];

      function check(key, callback, level) {
        level = level || 'warning';
        if (callback()) {
          Notifications.remove(validationNotifications[key], 'series-tobira-new');
          delete validationNotifications[key];
        } else {
          if (!validationNotifications[key]) {
            validationNotifications[key] = Notifications.add(level, key, 'series-tobira-new', -1);
          }
          if (level !== 'info') {
            valid = false;
          }
        }
      }

      check('TOBIRA_OVERRIDE_NAME', function () {
        return me.ud.selectedPage && !me.ud.selectedPage.title;
      }, 'info');

      check('TOBIRA_NO_PATH_SEGMENT', function () {
        return newPage.segment;
      });

      check('TOBIRA_PATH_SEGMENT_INVALID', function () {
        return !newPage.segment || newPage.segment.length > 1 && ![
          // eslint-disable-next-line no-control-regex
          /[\u0000-\u001F\u007F-\u009F]/u,
          /[\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]/u,
          /[<>"[\\\]^`{|}#%/?]/u,
          /^[-+~@_!$&;:.,=*'()]/u
        ].some(function (regex) {
          return regex.test(newPage.segment);
        });
      });

      check('TOBIRA_PATH_SEGMENT_UNIQUE', function () {
        return me.currentPage.children.every(function (child) {
          return child == newPage || child.segment != newPage.segment;
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

      clearNotifications(serverNotifications, 'series-tobira');
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
            serverNotifications.realmNotFound = Notifications.add('warning', 'TOBIRA_PAGE_NOT_FOUND', 'series-tobira',
              -1);
            break;
          case 503:
            me.visible = false;
            break;
          default:
            serverNotifications.error = Notifications.add('error', 'TOBIRA_SERVER_ERROR', 'series-tobira', -1);
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
  };

  function clearNotifications(notifications, scope) {
    angular.forEach(notifications, function (notification, key) {
      Notifications.remove(notification, scope);
      delete notifications[key];
    });
  }

  return new Tobira();
}]);
