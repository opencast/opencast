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
.factory('AdopterRegistrationStates', [
  function () {
    return {
      get: function (mode) {
        var states = {
          'information': {
            'nextState': {
              0: 'close',
              1: 'form',
              2: 'skip'
            },
            'buttons': {
              'submit': true,
              'back': false,
              'skip': true,
              'close': true,
              'submitButtonText': 'ADOPTER_REGISTRATION.MODAL.CONTINUE'
            }
          },
          'form': {
            'nextState': {
              0: 'close',
              1: 'save',
              2: 'legal_info',
              3: 'update',
              4: 'delete_submit',
              5: 'information'
            },
            'buttons': {
              'submit': true,
              'back': true,
              'skip': false,
              'close': true,
              'delete': false,
              'submitButtonText': 'SUBMIT'
            }
          },
          'save': {
            'nextState': {
              0: 'thank_you',
              1: 'error'
            },
            'buttons': {
              'submit': false,
              'back': false,
              'skip': false,
              'close': false,
              'submitButtonText': null
            }
          },
          'update': {
            'nextState': {
              0: 'close',
              1: 'error'
            },
            'buttons': {
              'submit': false,
              'back': false,
              'skip': false,
              'close': false,
              'submitButtonText': null
            }
          },
          'delete_submit': {
            'nextState': {
              0: 'close',
              1: 'delete',
              5: 'form'
            },
            'buttons': {
              'submit': true,
              'back': true,
              'skip': false,
              'close': true,
              'submitButtonText': 'CONFIRM'
            }
          },
          'delete': {
            'nextState': {
              0: 'close',
              1: 'error'
            },
            'buttons': {
              'submit': false,
              'back': false,
              'skip': false,
              'close': false,
              'submitButtonText': null
            }
          },
          'thank_you': {
            'nextState': {
              0: 'close',
              1: 'error'
            },
            'buttons': {
              'submit': false,
              'back': false,
              'skip': false,
              'close': true,
              'submitButtonText': null
            }
          },
          'error': {
            'nextState': {
              0: 'close'
            },
            'buttons': {
              'submit': false,
              'back': false,
              'skip': false,
              'close': true,
              'submitButtonText': null
            }
          },
          'skip': {
            'nextState': {
              0: 'close',
              1: 'error'
            },
            'buttons': {
              'submit': false,
              'back': false,
              'skip': false,
              'close': true,
              'submitButtonText': null
            }
          },
          'legal_info': {
            'nextState': {
              0: 'close',
              1: 'error',
              5: 'form'
            },
            'buttons': {
              'submit': false,
              'back': true,
              'skip': false,
              'close': true,
              'submitButtonText': null
            }
          }
        };

        if(mode == 'slim')
        {
          delete states.information;
          delete states.skip;

          states.form.nextState[0] = 'close';
          states.form.buttons.back = false;
          states.form.buttons.close = true;
          states.form.buttons.delete = true;
        }

        return states;
      },
      getInitialState: function(mode) {
        if(mode == 'slim')
          return 'form';
        else
          return 'information';
      }
    };
  }]);
