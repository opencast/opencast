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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.solr

import org.apache.lucene.analysis.CharTokenizer
import org.apache.lucene.analysis.Tokenizer
import org.apache.solr.analysis.BaseTokenizerFactory

import java.io.Reader

/**
 * A solr tokenizer factory that doesn't actually tokenize. We use this to enable use of
 * [org.apache.solr.analysis.LowerCaseFilterFactory].
 */
class NonTokenizingFactory : BaseTokenizerFactory() {
    override fun create(input: Reader): Tokenizer {
        return object : CharTokenizer(input) {
            override fun isTokenChar(c: Char): Boolean {
                return true
            }
        }
    }
}
