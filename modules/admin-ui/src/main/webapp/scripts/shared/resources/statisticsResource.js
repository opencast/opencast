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

angular.module('adminNg.resources')
.factory('StatisticsResource', ['$resource', function ($resource) {
  return $resource('/admin-ng/statistics/:op.json', {}, {
    query: {
      method: 'GET',
      isArray: true,
      params: {
        op: 'providers',
        resourceType: '@resourceType'
      }
    },
    get: {
      method: 'POST',
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      transformRequest: function (data) {
        return $.param({
          data: JSON.stringify(data)
        });
      },
      params: {
        op: 'data'
      },
      isArray: true
    }
  });
}]);

angular.module('adminNg.resources')
.factory('StatisticsReusable', ['Language', 'StatisticsResource', function (Language, StatisticsResource) {
  function createTickCallback(that, idx) {
    return function(value, index, values) {
      var chooseMode = that.statProviderData[idx].timeChooseMode;
      var dataResolution = that.statProviderData[idx].dataResolution;
      var formatString = 'L';
      if (chooseMode == 'year') {
        formatString = 'MMMM';
      } else if (chooseMode == 'month') {
        formatString = 'dddd, Do';
      } else {
        if (dataResolution === 'hourly') {
          formatString = 'LLL';
        }
      }
      return moment(value).locale(Language.getLanguageCode()).format(formatString);
    };
  }

  function createTooltipCallback(that, idx) {
    return function(tooltipItem, data) {
      var timeItemIdx = tooltipItem.index;
      var itemDate = data.labels[timeItemIdx];
      var chooseMode = that.statProviderData[idx].timeChooseMode;
      var dataResolution = that.statProviderData[idx].dataResolution;
      var formatString;
      if (chooseMode === 'year') {
        formatString = 'MMMM YYYY';
      } else if (chooseMode === 'month') {
        formatString = 'dddd, MMMM Do, YYYY';
      } else {
        if (dataResolution === 'monthly') {
          formatString = 'MMMM YYYY';
        } else if (dataResolution === 'yearly') {
          formatString = 'YYYY';
        } else if (dataResolution === 'daily') {
          formatString = 'dddd, MMMM Do, YYYY';
        } else {
          formatString = 'dddd, MMMM Do, YYYY HH:mm';
        }
      }
      var finalDate = moment(itemDate).locale(Language.getLanguageCode()).format(formatString);
      var value = tooltipItem.value;
      return finalDate + ': ' + value;
    };
  }

  function StatisticsReusableInstance(resourceType, resourceId, previousProviderData) {
    this.statProviderData = [];
    this.error = false;
    var that = this;
    StatisticsResource.query({
      resourceType: resourceType,
    }).$promise.then(function(providers) {
      var statsRequest = [];
      var originalDataResolution = 'monthly';
      var originalTimeChooseMode = 'year';
      var originalFrom = moment().startOf(originalTimeChooseMode);
      var originalTo = moment().endOf(originalTimeChooseMode);
      for (var i = 0; i < providers.length; i++) {
        var previousData;
        if (previousProviderData !== undefined) {
          previousData = previousProviderData[i];
        } else {
          previousData = null;
        }
        var providerId = providers[i].providerId;
        if (providers[i].providerType !== 'timeSeries') {
          that.statProviderData.push({
            title: providers[i].title,
            description: providers[i].description,
            providerType: providers[i].providerType
          });
          continue;
        }
        // eslint-disable-next-line
        var xAxisTickCallback = createTickCallback(that, i);
        var tooltipLabelCallback = createTooltipCallback(that, i);
        var timeChooseMode, from, to, dataResolution;
        if (previousData !== null) {
          from = previousData.from;
          to = previousData.to;
          timeChooseMode = previousData.timeChooseMode;
          dataResolution = previousData.dataResolution;
        } else {
          from = originalFrom.format('YYYY-MM-DD');
          to = originalTo.format('YYYY-MM-DD');
          timeChooseMode = originalTimeChooseMode;
          dataResolution = originalDataResolution;
        }
        var providerData = {
          from: from,
          dataResolution: dataResolution,
          timeChooseMode: timeChooseMode,
          dataResolutions: providers[i].dataResolutions,
          options: {
            animation: {
              duration: 0
            },
            layout: {
              padding: {
                top: 20,
                left: 20,
                right: 20
              }
            },
            scales: {
              xAxes: [ {
                ticks: {
                  callback: xAxisTickCallback
                }
              } ],
              yAxes: [ {
                ticks: {
                  beginAtZero: true
                }
              } ]
            },
            tooltips: {
              callbacks: {
                label: tooltipLabelCallback
              }
            }
          },
          labels: [],
          providerId: providerId,
          providerType: 'timeSeries',
          title: providers[i].title,
          description: providers[i].description,
          to: to,
          values: []
        };
        that.statProviderData.push(providerData);
        statsRequest.push({
          dataResolution: dataResolution,
          from: moment(from),
          providerId: providerId,
          resourceId: resourceId,
          to: moment(to)
        });
      }

      StatisticsResource.get(statsRequest).$promise.then(function(providerData) {
        for (var i = 0; i < providerData.length; i++) {
          var providerId = providerData[i].providerId;
          for (var j = 0; j < that.statProviderData.length; j++) {
            var currentData = that.statProviderData[j];
            if (currentData.providerId === providerId) {
              currentData.values = [ providerData[i].values ];
              currentData.labels = providerData[i].labels;
              currentData.totalValue = providerData[i].total;
              break;
            }
          }
        }
      },
      function(error) {
        that.error = true;
      });
    });

    this.hasStatistics = function () {
      return that.statProviderData.length !== 0;
    };

    this.recalculate = function(providerId, from, to, dataResolution, timeChooseMode) {
      var statsRequest = [{
        dataResolution: dataResolution,
        from: moment(from).toJSON(),
        providerId: providerId,
        resourceId: resourceId,
        to: moment(to).endOf('day').toJSON()
      }];
      StatisticsResource.get(statsRequest).$promise.then(function(providerData) {
        if (providerData.length == 1) {
          var newData = providerData[0];
          for (var j = 0; j < that.statProviderData.length; j++) {
            var currentData = that.statProviderData[j];
            if (currentData.providerId === providerId) {
              currentData.from = from;
              currentData.to = to;
              currentData.dataResolution = dataResolution;
              currentData.timeChooseMode = timeChooseMode;
              currentData.totalValue = newData.total;
              currentData.values = [ newData.values ];
              currentData.labels = newData.labels;
              break;
            }
          }
        }
      });
    };
  }

  return {
    createReusableStatistics: function (resourceType, resourceId, previousData) {
      return new StatisticsReusableInstance(resourceType, resourceId, previousData);
    }
  };
}]);
