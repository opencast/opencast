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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('EventsCtrl', ['$scope', 'Stats', 'Table', 'EventsResource', 'ResourcesFilterResource',
  'ResourcesListResource', 'Notifications', 'ConfirmationModal', 'RelativeDatesService', 'AuthService',
  'CommentResource','IdentityResource',
  function ($scope, Stats, Table, EventsResource, ResourcesFilterResource, ResourcesListResource, Notifications,
    ConfirmationModal, RelativeDatesService, AuthService, CommentResource, IdentityResource) {

    $scope.stats = Stats;

    var statsConfiguration = ResourcesListResource.queryRecursive({ resource: 'STATS' });
    statsConfiguration.$promise.then(function (configuration) {

      var statsConfigurationValues = configuration.map(function(element) {
        return element.value;
      });

      $scope.stats.configure({
        stats: statsConfigurationValues,
        resource:   'events',
        apiService: EventsResource
      });
    }).catch(angular.noop);

    AuthService.getUser().$promise.then(function (user) {
      $scope.editorUrl = user.org.properties['admin.editor.url'];
      if (!$scope.editorUrl) {
        $scope.editorUrl = '/editor-ui/index.html?id=$id';
      }
    }).catch(angular.noop);

    $scope.table = Table;
    $scope.table.configure({
      columns: [{
        name:  'title',
        label: 'EVENTS.EVENTS.TABLE.TITLE',
        sortable: true
      }, {
        template: 'modules/events/partials/eventsPresentersCell.html',
        name:  'presenter',
        label: 'EVENTS.EVENTS.TABLE.PRESENTERS',
        sortable: true
      }, {
        template: 'modules/events/partials/eventsSeriesCell.html',
        name:  'series_name',
        label: 'EVENTS.EVENTS.TABLE.SERIES',
        sortable: true
      }, {
        template: 'modules/events/partials/eventsDateCell.html',
        name:  'date',
        label: 'EVENTS.EVENTS.TABLE.DATE',
        sortable: true
      }, {
        name:  'start_date',
        label: 'EVENTS.EVENTS.TABLE.START',
        sortable: true
      }, {
        name:  'end_date',
        label: 'EVENTS.EVENTS.TABLE.STOP',
        sortable: true
      }, {
        template: 'modules/events/partials/eventsLocationCell.html',
        name:  'location',
        label: 'EVENTS.EVENTS.TABLE.LOCATION',
        sortable: true
      }, {
        name:  'publication',
        label: 'EVENTS.EVENTS.TABLE.PUBLISHED',
        template: 'modules/events/partials/publishedCell.html',
        sortable: true
      }, {
        template: 'modules/events/partials/eventsStatusCell.html',
        name:  'event_status',
        label: 'EVENTS.EVENTS.TABLE.STATUS',
        sortable: true
      }, {
        template: 'modules/events/partials/eventActionsCell.html',
        label:    'EVENTS.EVENTS.TABLE.ACTION'
      }, {
        template: 'modules/events/partials/eventsNotesCell.html',
        label: 'EVENTS.EVENTS.TABLE.ADMINUI_NOTES',
        deactivated: true,
      }],
      caption:    'EVENTS.EVENTS.TABLE.CAPTION',
      resource:   'events',
      category:   'events',
      apiService: EventsResource,
      multiSelect: true,
      postProcessRow: function (row) {
        angular.forEach(row.publications, function (publication, index) {
          if (angular.isDefined($scope.publicationChannels[publication.id])) {
            var record = JSON.parse($scope.publicationChannels[publication.id]);
            publication.label = record.label ? record.label : publication.name;
            publication.icon = record.icon;
            publication.hide = record.hide;
            publication.description = record.description;
            publication.order = record.order ? record.order : 999 + index;
          } else {
            publication.label = publication.name;
            publication.order = 999 + index;
          }
        });
        row.checkedDelete = function() {
          ConfirmationModal.show('confirm-modal',Table.delete,row);
        };
        row.embeddingCode = function() {
          ConfirmationModal.show('embedding-code',Table.fullScreenUrl,row);
        };

        row.editorUrl = $scope.editorUrl.replace('$id', row.id);
      }
    });

    $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
    $scope.publicationChannels = ResourcesListResource.get({ resource: 'PUBLICATION.CHANNELS' });

    $scope.table.dateToFilterValue = function(dateString) {
      var date = new Date(dateString),
          from = new Date(date.setHours(0, 0, 0, 0)),
          to = new Date(date.setHours(23, 59, 59, 999));
      return from.toISOString() + '/' + to.toISOString();
    };

    $scope.table.delete = function (row) {
      EventsResource.delete({id: row.id}, function (response) {
        Table.fetch();
        if (response.status == 200) {
          Notifications.add('success', 'EVENT_DELETED');
        } else {
          Notifications.add('success', 'EVENT_WILL_BE_DELETED');
        }
      }, function (error) {
        if (error.status === 401) {
          Notifications.add('error', 'EVENTS_NOT_DELETED_NOT_AUTHORIZED');
        } else {
          Notifications.add('error', 'EVENTS_NOT_DELETED');
        }
      });
    };
    $scope.embedCode = '';

    $scope.getEmbedCode = function (size, id) {
      const identity = IdentityResource.get();
      identity.$promise.then(function (info) {

        // check if the engage URL is configured
        let engageUrl = window.location.origin;
        if (info && info.org && info.org.properties && info.org.properties) {
          engageUrl = info.org.properties['org.opencastproject.engage.ui.url'] || engageUrl;
        }

        const url = engageUrl + '/play/' + id;
        const sizeArray = size.split('x'),
              width = sizeArray[0],
              height = sizeArray[1];
        $scope.selectedBox = size;
        $scope.embedCode = '<iframe allowfullscreen src="' + url +
          '" style="border: none;" name="Player" scrolling="no"' +
          ' frameborder="0" marginheight="0px" marginwidth="0px" width="' +
          width + '" height="' + height + '"></iframe>';
      });
    };

    $scope.copyToClipboard = function(toCopy) {
      // create temp element
      var copyElement = document.createElement('span');
      copyElement.appendChild(document.createTextNode(toCopy));
      copyElement.id = 'tempCopyToClipboard';
      angular.element(document.body.append(copyElement));

      // select the text
      var range = document.createRange();
      range.selectNode(copyElement);
      window.getSelection().removeAllRanges();
      window.getSelection().addRange(range);

      // copy & cleanup
      document.execCommand('copy');
      window.getSelection().removeAllRanges();
      copyElement.remove();

      //update & show confirmation message
      $scope.copiedBox = $scope.selectedBox;
      $scope.confirmMsgVisibility = {'visibility': 'visible'};
    };

    // Type of comments in the notes column
    $scope.table.notesCommentReason = 'EVENTS.EVENTS.DETAILS.COMMENTS.REASONS.ADMINUI_NOTES';
    // Tmp storage for created comments
    // Used to avoid creating duplicate comments
    $scope.table.createdComments = [];

    $scope.table.createComment = function(commentText, eventId) {
      if (!commentText || !eventId) {
        return;
      }

      // If a comment for this event was already created,
      // update the existing comment instead
      const index = $scope.table.createdComments.findIndex(object => {
        return object.eventId === eventId;
      });
      if (index >= 0) {
        let comment = $scope.table.createdComments[index].comment;
        comment.text = commentText;
        $scope.table.updateComment(comment, eventId);
        return;
      }

      const comment = CommentResource.save(
        { resource: 'event', resourceId: eventId, type: 'comment' },
        { text: commentText, reason: $scope.table.notesCommentReason }
      );

      // Remember created comment
      $scope.table.createdComments.push({eventId: eventId, comment: comment});
    };

    $scope.table.updateComment = function(comment, eventId) {
      if (!comment || !eventId) {
        return;
      }
      CommentResource.update(
        { resource: 'event', resourceId: eventId, id: comment.id, type: 'comment' },
        { text: comment.text, reason: comment.reason, resolved: comment.resolvedStatus }
      );
    };

    $scope.table.deleteComment = function(comment, eventId) {
      if (!comment || !eventId) {
        return;
      }
      CommentResource.delete(
        { resource: 'event', resourceId: eventId, id: comment.id, type: 'comment' }
      );
    };

    // Stop automatic table refresh while performing an action in the table
    $scope.table.stopRefresh = function() {
      $scope.table.refreshScheduler.on = false;
      $scope.table.refreshScheduler.cancel();
    };

    $scope.table.startRefresh = function() {
      $scope.table.refreshScheduler.on = true;
      $scope.table.refreshScheduler.newSchedule();
    };

  }
]);
