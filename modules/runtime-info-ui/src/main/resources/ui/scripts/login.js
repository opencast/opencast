/*
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

function init() {
  const params = new URLSearchParams(document.location.search);
  if (params.has('error')) {
    document.getElementById('login').classList.add('error');
  }

  // fill in the default credential on test systems
  if (window.location.hostname === 'localhost' || window.location.hostname.endsWith('opencast.org')) {
    document.getElementById('username').value = 'admin';
    document.getElementById('password').value = 'opencast';
  }
}

addEventListener('DOMContentLoaded', () => init());
