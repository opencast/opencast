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

function StatisticsGraphController($scope, $element, $attrs, Language) {
  var ctrl = this;

  ctrl.dataResolutions = [
    { label: 'Yearly', value: 'yearly' },
    { label: 'Monthly', value: 'monthly' },
    { label: 'Daily', value: 'daily' },
    { label: 'Hourly', value: 'hourly' }
  ];

  ctrl.timeChooseModes = [
    {
      'value': 'year',
      'translation': 'Year'
    },
    {
      'value': 'month',
      'translation': 'Month'
    },
    {
      'value': 'custom',
      'translation': 'Custom'
    }
  ];

  var formatStrings = {
    month: 'MMMM YYYY',
    year: 'YYYY'
  };

  var dataResolutions = {
    month: 'daily',
    year: 'monthly'
  };

  ctrl.change = function() {
    if (ctrl.timeChooseMode === 'year' || ctrl.timeChooseMode === 'month') {
      ctrl.from = moment(ctrl.from).clone().startOf(ctrl.timeChooseMode).format('YYYY-MM-DD');
      ctrl.to = moment(ctrl.from).clone().endOf(ctrl.timeChooseMode).format('YYYY-MM-DD');
      ctrl.dataResolution = dataResolutions[ctrl.timeChooseMode];
    }
    ctrl.onChange({
      from: ctrl.from,
      to: ctrl.to,
      dataResolution: ctrl.dataResolution,
      timeChooseMode: ctrl.timeChooseMode,
    });
  };

  var localizedMoment = function(m) {
    return moment(m).locale(Language.getLanguageCode());
  };

  ctrl.selectedName = function() {
    return localizedMoment(ctrl.from).format(formatStrings[ctrl.timeChooseMode]);
  };

  ctrl.previousName = function() {
    return localizedMoment(ctrl.from).subtract(1, ctrl.timeChooseMode + 's').format(formatStrings[ctrl.timeChooseMode]);
  };

  ctrl.nextName = function() {
    return localizedMoment(ctrl.from).add(1, ctrl.timeChooseMode + 's').format(formatStrings[ctrl.timeChooseMode]);
  };

  ctrl.selectPrevious = function() {
    ctrl.from = moment(ctrl.from).subtract(1, ctrl.timeChooseMode + 's').format('YYYY-MM-DD');
    ctrl.change();
  };

  ctrl.selectNext = function() {
    ctrl.from = moment(ctrl.from).add(1, ctrl.timeChooseMode + 's').format('YYYY-MM-DD');
    ctrl.change();
  };

  ctrl.changeTimeChooseMode = function() {
    if (ctrl.timeChooseMode === ctrl.previousTimeChooseMode) {
      return;
    }

    if (ctrl.previousTimeChooseMode === 'custom') {
      ctrl.from = moment(ctrl.from).startOf(ctrl.timeChooseMode).format('YYYY-MM-DD');
      ctrl.to = moment(ctrl.from).endOf(ctrl.timeChooseMode).format('YYYY-MM-DD');
      ctrl.dataResolution = dataResolutions[ctrl.timeChooseMode];
    }

    ctrl.previousTimeChooseMode = ctrl.timeChooseMode;

    ctrl.change();
  };

  this.$onInit = function() {
    ctrl.previousTimeChooseMode = ctrl.timeChooseMode;
    ctrl.from = moment(ctrl.from).startOf(ctrl.timeChooseMode).format('YYYY-MM-DD');
    ctrl.to = moment(ctrl.from).endOf(ctrl.timeChooseMode).format('YYYY-MM-DD');
    ctrl.dataResolution = dataResolutions[ctrl.timeChooseMode];
  };
}


/**
 * @ngdoc directive
 * @name adminNg.directives.statisticsGraph
 * @description
 * Draws a graph for statistics and provides a date/time picker
 */
angular.module('adminNg.components')
  .component('statisticsGraph', {
    templateUrl: 'shared/partials/statisticsGraph.html',
    controller: StatisticsGraphController,
    bindings: {
      sourceData: '<',
      from: '<',
      to: '<',
      dataResolution: '<',
      timeChooseMode: '<',
      chartOptions: '<',
      chartLabels: '<',
      totalValue: '<',
      onChange: '&',
      provider: '<',
      description: '<'
    }
  });
