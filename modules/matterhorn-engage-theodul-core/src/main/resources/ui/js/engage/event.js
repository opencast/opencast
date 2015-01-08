/**
 * Copyright 2009-2011 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/*jslint browser: true, nomen: true*/
/*global define, CustomEvent*/
define("EngageEvent", function() {
    "use strict";

    function EngageEvent(_name, _description, _type) {
        this.name = (!_name || _name.length <= 0) ? "" : _name;
        this.description = (!_description || _description.length <= 0) ? "" : _description;
        this.type = (!_type || _type.length <= 0) ? "" : _type;
    }

    EngageEvent.prototype.getName = function() {
        return name;
    };

    EngageEvent.prototype.getDescription = function() {
        return description;
    };

    EngageEvent.prototype.getType = function() {
        return type;
    };

    EngageEvent.prototype.toString = function() {
        return name;
    };

    return EngageEvent;
});
