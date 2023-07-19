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

import { getUrlFromOpencastServer } from './PaellaOpencast';

export const loadBreaks = async (player, videoId) => {
  const requestUrl = `/annotation/annotations.json?episode=${videoId}&type=paella%2Fbreaks&day=&limit=1&offset=0`;
  const breaks = [];
  const response = await fetch(getUrlFromOpencastServer(requestUrl));
  if (response.ok) {
    try {
      const data = await response.json();
      const annotation = Array.isArray(data.annotations?.annotation) ?
        data.annotations?.annotation[0] : data.annotations?.annotation;
      JSON.parse(annotation.value).breaks.forEach(({id, e, s, text}) => {
        breaks.push({id, s, e, text});
      });
    }
    catch (e) {
      player.log.warn('Error loading breaks annotations');
    }
  }
  return breaks;
};
