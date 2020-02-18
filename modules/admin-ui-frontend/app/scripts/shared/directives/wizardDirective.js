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

angular.module('adminNg.directives')
.constant('EVENT_TAB_CHANGE', 'tab_change')
.directive('adminNgWizard', ['EVENT_TAB_CHANGE', function (EVENT_TAB_CHANGE) {
  function createWizard($scope) {
    var currentState = $scope.states[0], step, lookupIndex, lookupState, toTab,
        getCurrentState, getCurrentStateController, getPreviousState, getNextState,
        getCurrentStateName, getCurrentStateIndex, getStateControllerByName, save, isReachable,
        hasPrevious, isCompleted, isLast, getFinalButtonTranslation, sharedData, result;

    // The "sharedData" attribute allows steps to preserve localized ng-repeat data
    // at the wizard scope for cases when then the "save" callback is not sufficient.
    // Specifically, to preserve UI visible "file" selections created within ng-repeat loops.
    // Unlike all other html input fields, the "file" type html input is forbidden to be
    // manually/programatically reset via javascript.
    // This attribute is used by the asset file upload directive.
    $scope.sharedData = {};

    angular.forEach($scope.states, function (state) {
      if (!angular.isDefined(state.stateController.visible)) {
        state.stateController.visible = true;
      }
    });

    // retrieve shared data from the state controllers
    angular.forEach($scope.states, function (state) {
      if (angular.isDefined(state.stateController.addSharedDataPromise)) {
        state.stateController.addSharedDataPromise();
      }
    });

    /* Saves the state of the current view */
    save = function (id, callback) {
      getCurrentState().stateController.save(this, callback);
    };

    step = $scope.states[0].stateController;

    /* Returns the current state object, which allows for specific
         * queries, e.g. the metadata array. */
    getCurrentState = function () {
      return currentState;
    };
    getCurrentStateController = function () {
      return currentState.stateController;
    };
    getPreviousState = function (offset) {
      if (!offset) {
        offset = 1;
      }
      var prevState = $scope.states[getCurrentStateIndex() - offset];
      if (prevState.stateController.visible) {
        return prevState;
      } else {
        return getPreviousState(offset + 1);
      }
    };
    getNextState = function (offset) {
      if (!offset) {
        offset = 1;
      }
      var nextState = $scope.states[getCurrentStateIndex() + offset];
      if (nextState.stateController.visible) {
        return nextState;
      } else {
        return getNextState(offset + 1);
      }
    };
    getCurrentStateName = function () {
      return currentState.name;
    };
    getCurrentStateIndex = function () {
      return lookupIndex(currentState.name);
    };
    getStateControllerByName = function (name) {
      return $scope.states[lookupIndex(name)].stateController;
    };
    hasPrevious = function () {
      return lookupIndex(currentState.name) > 0;
    };
    lookupState = function (name) {
      var index = lookupIndex(name);
      return $scope.states[index];
    };
    lookupIndex = function (name) {
      try {
        for (var i = 0; i < $scope.states.length; i++) {
          if ($scope.states[i].name === name) {
            return i;
          }
        }
      } catch (e) { }
    };

    /* Will switch to the tab denoted in the data-modal-tab attribute of
     * the anchor that was clicked.
     * Prerequisite: All previous steps of the wizard have been passed
     * successfully.
     */
    toTab = function ($event) {
      var targetStepName, targetState;
      targetStepName = $event.target.getAttribute('data-modal-tab');
      if (targetStepName === 'previous') {
        targetState = getPreviousState();
      } else if (targetStepName === 'next') {
        targetState = getNextState();
      } else {
        targetState = lookupState(targetStepName);
      }
      if (angular.isUndefined(targetState)) {
        return;
      }
      // Permission to navigate to a tab is only granted, if the previous tabs are all valid
      if (isReachable(targetState.name)) {
        $scope.$emit(EVENT_TAB_CHANGE, {
          old: currentState,
          current: targetState
        });

        // one-time directed call to the current state
        // to allow the state to perform exit cleanup
        if ($scope.wizard.step.onExitStep) {
          $scope.wizard.step.onExitStep();
        }

        currentState = targetState;
        $scope.wizard.step = targetState.stateController;

        //FIXME: This should rather be a service I guess, so it won't be tied to modals.
        //Its hard to unit test like this also
        $scope.$parent.openTab(targetState.name);
        focus();
      }
    };

    isReachable = function (stateName) {
      var index, currentState;
      index = lookupIndex(stateName) - 1;
      while (index >= 0) {
        currentState = $scope.states[index];
        if (!currentState.stateController.isValid()) {
          return false;
        }
        index--;
      }
      return true;
    };

    isCompleted = function (stateName) {
      return lookupState(stateName).stateController.isValid();
    };

    isLast = function () {
      return getCurrentStateIndex() === $scope.states.length - 1;
    };

    getFinalButtonTranslation = function () {
      if (angular.isDefined($scope.finalButtonTranslation)) {
        return $scope.finalButtonTranslation;
      } else {
        return 'WIZARD.CREATE';
      }
    };

    // TODO: Does this have to be a global var?
    // eslint-disable-next-line
    focus = function () {
      //make sure the tab index starts again with 1
      angular.forEach(angular.element.find('[focushere]'), function (element) {
        angular.element(element).trigger('chosen:activate').focus();
      });
      var tabindexOne = angular.element($('[tabindex=1]'));
      tabindexOne.focus();
    };

    result = {
      states: $scope.states,
      step: step,
      getCurrentState: getCurrentState,
      getCurrentStateController: getCurrentStateController,
      getCurrentStateName: getCurrentStateName,
      getStateControllerByName: getStateControllerByName,
      save: save,
      sharedData: sharedData,
      toTab: toTab,
      isReachable: isReachable,
      isCompleted: isCompleted,
      isLast: isLast,
      getFinalButtonTranslation: getFinalButtonTranslation,
      hasPrevious: hasPrevious,
      submit: $scope.submit
    };

    angular.forEach($scope.states, function (state) {
      if (!angular.isDefined(state.stateController.visible)) {
        state.stateController.visible = true;
      }
      state.stateController.wizard = result;
    });

    return result;
  }

  return {
    restrict: 'E',
    templateUrl: 'shared/partials/wizard.html',
    scope: {
      submit: '=',
      states: '=',
      name:   '@',
      action: '=',
      finalButtonTranslation: '@'
    },
    link: function (scope) {
      scope.isCurrentTab = function (tab) {
        return scope.wizard.getCurrentStateName() === tab;
      };
      /**
             * Check if the given value is empty or undefined
             */
      scope.isEmpty =  function (value) {
        return angular.isUndefined(value) || (angular.isString(value) && value.length === 0) ||
                        (angular.isObject(value) && JSON.stringify(value).length === 2);
      };
      scope.wizard = createWizard(scope);
      scope.deleted = true;

      scope.keyUp = function (event) {
        if (event.keyCode === 13 || event.keyCode === 32) {
          scope.wizard.toTab(event);
        }
      };

      scope.keyUpSubmit = function (event) {
        if (event.keyCode === 13 || event.keyCode === 32) {
          scope.submit();
        }
      };

    }
  };
}]);
