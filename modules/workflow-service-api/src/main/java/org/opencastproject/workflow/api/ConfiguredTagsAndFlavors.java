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
package org.opencastproject.workflow.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Test
 */
public class ConfiguredTagsAndFlavors {

    private List<String> srcTags;
    private List<String> targetTags;
    private List<String> srcFlavors;
    private List<String> targetFlavors;

    protected ConfiguredTagsAndFlavors() {
        this.srcTags = new ArrayList<>();
        this.targetTags = new ArrayList<>();
        this.srcFlavors = new ArrayList<>();
        this.targetFlavors = new ArrayList<>();
    }

    public List<String> getSrcTags() {
        return this.srcTags;
    }

    public List<String> getTargetTags() {
        return this.targetTags;
    }

    public List<String> getSrcFlavors() {
        return this.srcFlavors;
    }

    public List<String> getTargetFlavors() {
        return this.targetFlavors;
    }

    protected void setSrcTags(List<String> srcTags) {
        this.srcTags = srcTags;
    }

    protected void setTargetTags(List<String> targetTags) {
        this.targetTags = targetTags;
    }

    protected void setSrcFlavors(List<String> srcFlavors) {
        this.srcFlavors = srcFlavors;
    }

    protected void setTargetFlavors(List<String> targetFlavors) {
        this.targetFlavors = targetFlavors;
    }
}
