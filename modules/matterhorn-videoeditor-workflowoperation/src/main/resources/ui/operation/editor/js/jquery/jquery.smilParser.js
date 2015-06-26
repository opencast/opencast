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
$.smilParser = function (smil) {
    var TRACK_DURATION = "track-duration";
    var trackDuration_tmp = 0;

    var ms = 'ms';
    var smil_xml_xmlID = '-xml:id';
    var smil_xml_name = '-name';
    var smil_xml_content = '-content';
    var smil_xml_value = '-value';
    var smil_xml_valuetype = '-valuetype';
    var smil_xml_clipBegin = '-clipBegin';
    var smil_xml_clipEnd = '-clipEnd';
    var smil_xml_paramGroup = '-paramGroup';
    var smil_xml_src = '-src';

    if (smil) {
        this.smil = smil;
        this.trackDuration = 0;
        this.paramGroups = "";
        this.par = "";

        try {
            var xotree = new XML.ObjTree();
            var tree = xotree.parseXML(smil);

            if (tree.smil) {
                this.parsedSmil = new Object();
                this.parsedSmil.xmlID = tree.smil[smil_xml_xmlID];

                if (tree.smil.head) {
                    // parse track duration
                    if (tree.smil.head.meta && (tree.smil.head.meta.length > 1)) {
                        $.each(tree.smil.head.meta, function (index, value) {
                            if (value[smil_xml_name].indexOf(TRACK_DURATION) != -1) {
                                trackDuration_tmp = value[smil_xml_content];
                            }
                        });
                        this.parsedSmil.trackDuration = trackDuration_tmp;
                    } else if (tree.smil.head.meta &&
                        tree.smil.head.meta[smil_xml_name] &&
                        tree.smil.head.meta[smil_xml_name].indexOf(TRACK_DURATION) != -1) {
                        this.parsedSmil.trackDuration = tree.smil.head.meta[smil_xml_content];
                    }
                    if (this.parsedSmil.trackDuration && (this.parsedSmil.trackDuration.indexOf(ms) != -1)) {
                        this.parsedSmil.trackDuration = this.parsedSmil.trackDuration.substring(0, this.parsedSmil.trackDuration.length - 2);
                    } else {
                        this.parsedSmil.trackDuration = -1;
                    }

                    // parse and improve paramGroups
                    // schema: head - paramgroup[ARRAY] - param[ARRAY]
                    this.parsedSmil.paramGroup = new Array();
                    var pGroup = tree.smil.head.paramGroup;
                    if (pGroup) {
                        var tmp_pGroup = new Array();
                        if (pGroup.length && (pGroup.length > 1)) {
                            // iterate over paramGroup
                            $.each(pGroup, function (i1, value) {
                                var par = value.param;
                                tmp_pGroup[i1] = new Array();
                                tmp_pGroup[i1].param = new Array();
                                if (par) {
                                    var tmp_par_arr = new Array();
                                    if (par.length && (par.length > 1)) {
                                        // iterate over param
                                        $.each(par, function (i2, value) {
                                            tmp_par_arr[i2] = new Object();
                                            tmp_par_arr[i2].name = value[smil_xml_name];
                                            tmp_par_arr[i2].value = value[smil_xml_value];
                                            tmp_par_arr[i2].valuetype = value[smil_xml_valuetype];
                                            tmp_par_arr[i2].xmlID = value[smil_xml_xmlID];
                                        });
                                        for (var i = 0; i < tmp_par_arr.length; ++i) {
                                            tmp_pGroup[i1].param[i] = tmp_par_arr[i];
                                        }
                                    } else {
                                        tmp_par_arr[0] = new Object();
                                        tmp_par_arr[0].name = par[smil_xml_name];
                                        tmp_par_arr[0].value = par[smil_xml_value];
                                        tmp_par_arr[0].valuetype = par[smil_xml_valuetype];
                                        tmp_par_arr[0].xmlID = par[smil_xml_xmlID];
                                        tmp_pGroup[i1].param[0] = tmp_par_arr[0];
                                    }
                                }
                            });
                        } else {
                            var par = pGroup.param;
                            tmp_pGroup[0] = new Array();
                            tmp_pGroup[0].param = new Array();
                            if (par) {
                                var tmp_par_arr = new Array();
                                if (par.length && (par.length > 1)) {
                                    // iterate over param
                                    $.each(par, function (i2, value) {
                                        tmp_par_arr[i2] = new Object();
                                        tmp_par_arr[i2].name = value[smil_xml_name];
                                        tmp_par_arr[i2].value = value[smil_xml_value];
                                        tmp_par_arr[i2].valuetype = value[smil_xml_valuetype];
                                        tmp_par_arr[i2].xmlID = value[smil_xml_xmlID];
                                    });
                                    for (var i = 0; i < tmp_par_arr.length; ++i) {
                                        tmp_pGroup[0].param[i] = tmp_par_arr[i];
                                    }
                                } else {
                                    tmp_par_arr[0] = new Object();
                                    tmp_par_arr[0].name = par[smil_xml_name];
                                    tmp_par_arr[0].value = par[smil_xml_value];
                                    tmp_par_arr[0].valuetype = par[smil_xml_valuetype];
                                    tmp_par_arr[0].xmlID = par[smil_xml_xmlID];
                                    tmp_pGroup[0].param[0] = tmp_par_arr[0];
                                }
                            }
                        }
                        this.parsedSmil.paramGroup = tmp_pGroup;
                    }
                }

                if (tree.smil.body) {
                    // parse and improve par
                    // schema: body - par[ARRAY] - video[ARRAY]
                    this.parsedSmil.par = new Array();
                    var par = tree.smil.body.par;
                    if (par) {
                        var tmp_par = new Array();
                        if (par.length && (par.length > 1)) {
                            // iterate over par
                            $.each(par, function (i1, value) {
                                var video = value.video;
                                tmp_par[i1] = new Array();
                                tmp_par[i1].video = new Array();
                                if (video) {
                                    var tmp_video_arr = new Array();
                                    if (video.length && (video.length > 1)) {
                                        // iterate over video
                                        $.each(video, function (i2, value) {
                                            tmp_video_arr[i2] = new Object();
                                            tmp_video_arr[i2].clipBegin = value[smil_xml_clipBegin].substring(0, value[smil_xml_clipBegin].length - 2);
                                            tmp_video_arr[i2].clipEnd = value[smil_xml_clipEnd].substring(0, value[smil_xml_clipEnd].length - 2);
                                            tmp_video_arr[i2].paramGroup = value[smil_xml_paramGroup];
                                            tmp_video_arr[i2].src = value[smil_xml_src];
                                            tmp_video_arr[i2].xmlID = value[smil_xml_xmlID];
                                        });
                                        for (var i = 0; i < tmp_video_arr.length; ++i) {
                                            tmp_par[i1].video[i] = tmp_video_arr[i];
                                        }
                                    } else {
                                        tmp_video_arr[0] = new Object();
                                        tmp_video_arr[0].clipBegin = video[smil_xml_clipBegin].substring(0, video[smil_xml_clipBegin].length - 2);
                                        tmp_video_arr[0].clipEnd = video[smil_xml_clipEnd].substring(0, video[smil_xml_clipEnd].length - 2);
                                        tmp_video_arr[0].paramGroup = video[smil_xml_paramGroup];
                                        tmp_video_arr[0].src = video[smil_xml_src];
                                        tmp_video_arr[0].xmlID = video[smil_xml_xmlID];
                                        tmp_par[i1].video[0] = tmp_video_arr[0];
                                    }
                                }
                            });
                        } else {
                            var video = par.video;
                            tmp_par[0] = new Array();
                            tmp_par[0].video = new Array();
                            if (video) {
                                var tmp_video_arr = new Array();
                                if (video.length && (video.length > 1)) {
                                    // iterate over video
                                    $.each(video, function (i2, value) {
                                        tmp_video_arr[i2] = new Object();
                                        tmp_video_arr[i2].clipBegin = value[smil_xml_clipBegin].substring(0, value[smil_xml_clipBegin].length - 2);
                                        tmp_video_arr[i2].clipEnd = value[smil_xml_clipEnd].substring(0, value[smil_xml_clipEnd].length - 2);
                                        tmp_video_arr[i2].paramGroup = value[smil_xml_paramGroup];
                                        tmp_video_arr[i2].src = value[smil_xml_src];
                                        tmp_video_arr[i2].xmlID = value[smil_xml_xmlID];
                                    });
                                    for (var i = 0; i < tmp_video_arr.length; ++i) {
                                        tmp_par[0].video[i] = tmp_video_arr[i];
                                    }
                                } else {
                                    tmp_video_arr[0] = new Object();
                                    tmp_video_arr[0].clipBegin = video[smil_xml_clipBegin].substring(0, video[smil_xml_clipBegin].length - 2);
                                    tmp_video_arr[0].clipEnd = video[smil_xml_clipEnd].substring(0, video[smil_xml_clipEnd].length - 2);
                                    tmp_video_arr[0].paramGroup = video[smil_xml_paramGroup];
                                    tmp_video_arr[0].src = video[smil_xml_src];
                                    tmp_video_arr[0].xmlID = video[smil_xml_xmlID];
                                    tmp_par[0].video[0] = tmp_video_arr[0];
                                }
                            }
                        }
                        this.parsedSmil.par = tmp_par;
                    }
                }
            }
        } catch (e) {
            ocUtils.log("Error: Error while parsing smil: ");
            ocUtils.log(e);
        }
    } else {
        ocUtils.log("Error: No smil given");
    }
}

