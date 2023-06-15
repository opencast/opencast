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
const defaultDictionaries = {};

const dictionaries = require.context('./i18n/', true, /\.json$/);
dictionaries.keys().forEach(k => {
  const reResult = /([a-z-]+[A-Z_]+)\.json/.exec(k);
  const localization = reResult && reResult[1];

  if (localization) {
    const dict = dictionaries(k);
    defaultDictionaries[localization] = dict;

    const lang = localization.substr(0,2);
    if (!(lang in defaultDictionaries)) {
      defaultDictionaries[lang] = dict;
    }
  }
});

export default defaultDictionaries;
