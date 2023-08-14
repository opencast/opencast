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
/* global define */
define(function() {
  'use strict';

  function generateRandomID(length) {
    var id = '';
    var pool = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';

    for (var i = 0; i < length; ++i) {
      id += pool.charAt(Math.floor(Math.random() * pool.length));
    }

    return id;
  }

  function EngageEvent(_name, _description, _type) {
    this.name = (!_name || _name.length <= 0) ? ('RandomEvent:' + generateRandomID(8)) : _name;
    this.description = (!_description || _description.length <= 0) ? '' : _description;
    this.type = (!_type || _type.length <= 0) ? 'unknown' : _type;
  }

  EngageEvent.prototype.getName = function() {
    return this.name;
  };

  EngageEvent.prototype.getDescription = function() {
    return this.description;
  };

  EngageEvent.prototype.getType = function() {
    return this.type;
  };

  EngageEvent.prototype.toString = function() {
    return this.name;
  };

  return EngageEvent;
});
