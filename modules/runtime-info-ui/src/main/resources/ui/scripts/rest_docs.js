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

function search() {
  const value = document.getElementById('search').value.toLowerCase();
  for (const li of document.getElementsByTagName('li')) {
    li.style.display = li.innerText.toLowerCase().indexOf(value) >= 0 ? 'block' : 'none';
  }
}

async function init() {
  const input = document.getElementById('search');
  input.addEventListener('keyup', search);
  input.addEventListener('change', search);

  const docs = document.getElementById('docs');

  const response = await fetch('/info/components.json');
  const rest = (await response.json()).rest;
  rest.sort((a,b) => a.path > b.path ? 1 : -1);

  for (const endpoint of rest) {
    const li = document.createElement('li');
    const a = document.createElement('a');
    a.href = '/docs.html?path=' + endpoint.path;
    li.appendChild(a);
    const path = document.createElement('span');
    path.classList = ['path'];
    path.innerText = endpoint.path;
    a.appendChild(path);
    const desc = document.createElement('span');
    desc.classList = ['desc'];
    desc.innerText = endpoint.description;
    a.appendChild(desc);
    docs.appendChild(li);
  }
  search();
}

addEventListener('DOMContentLoaded', () => init());
