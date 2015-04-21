/**
* Copyright 2009-2013 The Regents of the University of California
* Licensed under the Educational Community License, Version 2.0
* (the "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.osedu.org/licenses/ECL-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an "AS IS"
* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*
*/
'use strict';

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('EventCtrl', [
    '$scope', 'EventMetadataResource', 'EventAttachmentsResource',
    'EventMediaResource', 'CommentResource', 'EventWorkflowsResource',
    'ResourcesListResource', 'EventAccessResource', 'EventGeneralResource',
    'OptoutsResource',
    function ($scope, EventMetadataResource,
        EventAttachmentsResource, EventMediaResource, CommentResource,
        EventWorkflowsResource, ResourcesListResource, EventAccessResource, EventGeneralResource,
        OptoutsResource) {

        var saveFns = {},
            mainCatalog = 'dublincore/episode',
            createPolicy = function (role) {
                return {
                    role  : role,
                    read  : false,
                    write : false
                };
            },
            changePolicies = function (access, loading) {
                var newPolicies = {};
                angular.forEach(access, function (acl) {
                    var policy = newPolicies[acl.role];

                    if (angular.isUndefined(policy)) {
                        newPolicies[acl.role] = createPolicy(acl.role);
                    }
                    newPolicies[acl.role][acl.action] = acl.allow;
                });

                $scope.policies = [];
                angular.forEach(newPolicies, function (policy) {
                    $scope.policies.push(policy);
                });

                if (!loading) {
                    $scope.accessSave();
                }
            },
            fetchChildResources = function (id) {
                $scope.general        = EventGeneralResource.get({ id: id });

                $scope.metadata       =  EventMetadataResource.get({ id: id }, function (metadata) {
                    var episodeCatalogIndex;
                    angular.forEach(metadata.entries, function (catalog, index) {
                        if (catalog.flavor === mainCatalog) {
                            $scope.episodeCatalog = catalog;
                            episodeCatalogIndex = index;
                            var keepGoing = true;
                            angular.forEach(catalog.fields, function (entry) {
                                if (entry.id === 'title') {
                                    $scope.title = entry.value;
                                }
                                if (keepGoing && entry.locked) {
                                    metadata.locked = entry.locked;
                                    keepGoing = false;
                                }
                            });
                        }
                    });

                    if (angular.isDefined(episodeCatalogIndex)) {
                        metadata.entries.splice(episodeCatalogIndex, 1);
                    }
                });

                $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
                $scope.roles = ResourcesListResource.get({ resource: 'ROLES' });

                $scope.media       = EventMediaResource.get({ id: id });
                $scope.attachments = EventAttachmentsResource.get({ id: id });
                $scope.workflows   = EventWorkflowsResource.get({ id: id });
                $scope.access      = EventAccessResource.get({ id: id }, function (data) {
                    if (angular.isDefined(data.episode_access)) {
                        var json = angular.fromJson(data.episode_access.acl);
                        changePolicies(json.acl.ace, true);
                    }
                });
                $scope.comments    = CommentResource.query({ resource: 'event', resourceId: id, type: 'comments' });
            };

        $scope.policies = [];
        $scope.baseAcl = {};

        $scope.changeBaseAcl = function () {
            $scope.baseAcl = EventAccessResource.getManagedAcl({id: this.baseAclId}, function () {
                changePolicies($scope.baseAcl.acl.ace);
            });
            this.baseAclId = '';
        };

        $scope.addPolicy = function () {
            $scope.policies.push(createPolicy());
        };

        $scope.deletePolicy = function (policyToDelete) {
            var index;

            angular.forEach($scope.policies, function (policy, idx) {
                if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write &&
                    policy.read === policyToDelete.read) {
                    index = idx;
                }
            });

            if (angular.isDefined(index)) {
                $scope.policies.splice(index, 1);
            }

            $scope.accessSave();
        };

        $scope.updateOptout = function (newBoolean) {
            OptoutsResource.save({
                resource: 'event',
                eventIds: [$scope.resourceId],
                optout: newBoolean
            }, function () { console.log('success'); }, function () {console.log('failure'); });
        };

        $scope.replyToId = null; // the id of the comment to which the user wants to reply
        $scope.title = $scope.resourceId; // if nothing else use the resourceId
        
        fetchChildResources($scope.resourceId);

        $scope.$on('change', function (event, id) {
            fetchChildResources(id);
        });

        // Generate proxy function for the save metadata function based on the given flavor
        // Do not generate it
        $scope.getSaveFunction = function (flavor) {
            var fn = saveFns[flavor],
                catalog;

            if (angular.isUndefined(fn)) {
                if ($scope.episodeCatalog.flavor === flavor) {
                    catalog = $scope.episodeCatalog;
                } else {
                    angular.forEach($scope.metadata.entries, function (c) {
                        if (flavor === c.flavor) {
                            catalog = c;
                        }
                    });
                }

                fn = function (id, callback) {
                    $scope.metadataSave(id, callback, catalog);
                };

                saveFns[flavor] = fn;
            }
            return fn;
        };

        $scope.metadataSave = function (id, callback, catalog) {
            catalog.attributeToSend = id;

            EventMetadataResource.save({ id: $scope.resourceId }, catalog,  function () {
                if (angular.isDefined(callback)) {
                    callback();
                }

                // Mark the saved attribute as saved
                angular.forEach(catalog.fields, function (entry) {
                    if (entry.id === id) {
                        entry.saved = true;
                    }
                });
            });
        };

        $scope.components = ResourcesListResource.get({ resource: 'components' });

        $scope.myComment = {};

        $scope.replyTo = function (comment) {
            $scope.replyToId = comment.id;
            $scope.originalComment = comment;
            $scope.myComment.resolved = false;
        };

        $scope.exitReplyMode = function () {
            $scope.replyToId = null;
            $scope.myComment.text = '';
        };

        $scope.comment = function () {
        	$scope.myComment.saving = true;
            CommentResource.save({ resource: 'event', resourceId: $scope.resourceId, type: 'comment' },
                { text: $scope.myComment.text, reason: $scope.myComment.reason },
                function () {
                	$scope.myComment.saving = false;
                	$scope.myComment.text = '';
                	
                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }, function () {
                	$scope.myComment.saving = false;
                }
            );
        };

        $scope.reply = function () {
        	$scope.myComment.saving = true;
            CommentResource.save({ resource: 'event', resourceId: $scope.resourceId, id: $scope.replyToId, type: 'comment', reply: 'reply' },
                { text: $scope.myComment.text, resolved: $scope.myComment.resolved },
                function () {
                	$scope.myComment.saving = false;
                	$scope.myComment.text = '';
                	
                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }, function () {
                	$scope.myComment.saving = false;
                }

            );
            $scope.exitReplyMode();
        };

        $scope.accessSave = function (field) {
            var ace = [];

            if (angular.isDefined(field) && angular.isUndefined(field.role)) {
                return;
            }

            angular.forEach($scope.policies, function (policy) {
                if (angular.isDefined(policy.role)) {
                    if (policy.read) {
                        ace.push({
                            'action' : 'read',
                            'allow'  : policy.read,
                            'role'   : policy.role
                        });
                    }

                    if (policy.write) {
                        ace.push({
                            'action' : 'write',
                            'allow'  : policy.write,
                            'role'   : policy.role
                        });
                    }
                }

            });

            EventAccessResource.save({id: $scope.resourceId}, {
                acl: {
                    ace: ace
                },
                override: true
            });
        };

        $scope.severityColor = function (severity) {
            switch (severity.toUpperCase()) {
                case 'FAILURE':
                    return 'red';
                case 'INFO':
                    return 'green';
                case 'WARNING':
                    return 'yellow';
            }
        };

        $scope.deleteComment = function (id) {
            CommentResource.delete(
                { resource: 'event', resourceId: $scope.resourceId, id: id, type: 'comment' },
                function () {
                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }
            );
        };

        $scope.deleteCommentReply = function (commentId, reply) {
            CommentResource.delete(
                { resource: 'event', resourceId: $scope.resourceId, type: 'comment', id: commentId, reply: reply },
                function () {
                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }
            );
        };
    }
]);
