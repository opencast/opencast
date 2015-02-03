/**
 * Copyright 2009-2013 The Regents of the University of California Licensed
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
$.smilResponseParser = function (smilResponse) {
    var smilResponse_xmlID = '-xml:id';
    var smilResponse_ns = 'ns2:smil-response';
    var smilResponse_nsEnt = 'ns2:entity';
    var smilResponse_nsEnt_entStart = '<ns2:entity>';
    var smilResponse_nsEnt_entEnd = '</ns2:entity>';
    var smilResponse_nsEnt_subStart = '</ns2:entity>';
    var smilResponse_nsEnt_subEnd = '</ns2:smil-response>';
    var smil_xmlHeader = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>';
    var smil_smilBaseProfile_start = '<smil baseProfile="Language" version="3.0"';
    var smil_smilXml_ns = 'xmlns="http://www.w3.org/ns/SMIL"';

    if (smilResponse) {
        this.smilResponse = smilResponse;
        this.parID = -1; // we just need the par id and the new smil
        this.parXMLEntity = "";
        this.smil = "";

        try {
            var xotree = new XML.ObjTree();
            var tree = xotree.parseXML(this.smilResponse);

            if (tree &&
                tree[smilResponse_ns]) {
                if (tree[smilResponse_ns].smil) {
                    // parse added par xml entity
                    this.parXMLEntity = smilResponse.substring(smilResponse.indexOf(smilResponse_nsEnt_entStart) + smilResponse_nsEnt_entStart.length, smilResponse.indexOf(smilResponse_nsEnt_entEnd));

                    // parse smil
                    var smilSub = smilResponse.substring(smilResponse.indexOf(smilResponse_nsEnt_subStart) + smilResponse_nsEnt_subStart.length, smilResponse.indexOf(smilResponse_nsEnt_subEnd));
                    // insert xml namespace
                    this.smil =
                        smil_xmlHeader +
                        smil_smilBaseProfile_start +
                        ' ' +
                        smil_smilXml_ns +
                        smilSub.substring(smilSub.indexOf(smil_smilBaseProfile_start) + smil_smilBaseProfile_start.length, smilSub.length);
                } else {
                    ocUtils.log("Error: Smil response does not contain valid smil");
                }

                if (tree[smilResponse_ns][smilResponse_nsEnt] &&
                    tree[smilResponse_ns][smilResponse_nsEnt].par &&
                    tree[smilResponse_ns][smilResponse_nsEnt].par[smilResponse_xmlID]) {
                    this.parID = tree[smilResponse_ns][smilResponse_nsEnt].par[smilResponse_xmlID];
                } else {
                    ocUtils.log("Warning: Smil response does not contain additional data");
                }
            } else {
                ocUtils.log("Error: Smil response does not contain valid data");
            }
        } catch (e) {
            ocUtils.log("Error: Error while parsing smil response: ");
            ocUtils.log(e);
        }
    } else {
        ocUtils.log("Error: No smil response given");
    }
}
