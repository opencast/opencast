angular.module('adminNg.directives')
.constant('EVENT_TAB_CHANGE', 'tab_change')
.directive('adminNgWizard', ['EVENT_TAB_CHANGE', function (EVENT_TAB_CHANGE) {
    function createWizard($scope) {
        var currentState = $scope.states[0], step, lookupIndex, lookupState, toTab,
            getCurrentState, getCurrentStateController, getPreviousState, getNextState,
            getCurrentStateName, getCurrentStateIndex, getStateControllerByName, save, isReachable,
            hasPrevious, isCompleted, isLast, getFinalButtonTranslation, result;

        angular.forEach($scope.states, function (state) {
            if (!angular.isDefined(state.stateController.visible)) {
                state.stateController.visible = true;
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
        getPreviousState = function () {
            return $scope.states[getCurrentStateIndex() - 1];
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

                currentState = targetState;
                $scope.wizard.step = targetState.stateController;

                //FIXME: This should rather be a service I guess, so it won't be tied to modals.
                //Its hard to unit test like this also
                $scope.$parent.openTab(targetState.name);
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

        result = {
            states: $scope.states,
            step: step,
            getCurrentState: getCurrentState,
            getCurrentStateController: getCurrentStateController,
            getCurrentStateName: getCurrentStateName,
            getStateControllerByName: getStateControllerByName,
            save: save,
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
            scope.wizard = createWizard(scope);
            scope.deleted = true;
        }
    };
}]);
