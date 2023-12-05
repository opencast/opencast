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
import { ButtonPlugin } from 'paella-core';
import defaultCookieIcon from '../icons/cookie.svg';


export default class CookieConsentButtonPlugin extends ButtonPlugin {

  async isEnabled() {
    if (!(await super.isEnabled())) {
      return false;
    }

    if (!this?.player?.config?.opencast?.cookieConsent?.enable) {
      this.player.log.warn('"Cookie Consent by TermsFeed" library not loaded. \
      You need to load in your web to use this plugin.');
      return false;
    }

    const consentChanged = () => {
      this.player.log.debug('Cookie consent changed');
      this.player.cookieConsent.updateConsentData();
    };
    window.addEventListener('cc_noticeBannerOkOrAgreePressed', consentChanged);
    window.addEventListener('cc_noticeBannerRejectPressed', consentChanged);
    window.addEventListener('cc_preferencesCenterSavePressed', consentChanged);

    return true;
  }

  getAriaLabel() {
    return 'Cookie consent';
  }

  getDescription() {
    return this.getAriaLabel();
  }

  async load() {
    this.icon = this.player.getCustomPluginIcon(this.name, 'buttonIcon') || defaultCookieIcon;
  }

  async action() {
    window.cookieconsent.openPreferencesCenter();
  }
}
