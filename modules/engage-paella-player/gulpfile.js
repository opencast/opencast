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

/* global require */

'use strict';
var gulp = require('gulp');
var spawn = require('child_process').spawn;
var mergeStream = require('merge-stream');


var buildPath = 'target/gulp',
    paellaSrc = 'src/main/paella-opencast';


gulp.task('paella-opencast:prepare:source', function(){
  var s1 = gulp.src('node_modules/paellaplayer/**').pipe(gulp.dest(buildPath + '/paella'));
  var s2 = gulp.src(paellaSrc + '/plugins/**').pipe(gulp.dest(buildPath + '/paella/plugins'));

  return mergeStream(s1,s2);
});



gulp.task('paella-opencast:prepare', gulp.series('paella-opencast:prepare:source', function(cb){
  var cmd_npm = spawn('npm', ['install'], {cwd: buildPath + '/paella', stdio: 'inherit'});
  cmd_npm.on('close', function (code) {
    cb(code);
  });
}));


gulp.task('paella-opencast:compile.debug', gulp.series('paella-opencast:prepare', function(cb){
  var cmd_npm = spawn('node', ['node_modules/gulp/bin/gulp.js', 'build.debug'], {cwd: buildPath + '/paella'});
  cmd_npm.on('close', function (code) {
    cb(code);
  });
}));

gulp.task('paella-opencast:compile.release', gulp.series('paella-opencast:prepare', function(cb){
  var cmd_npm = spawn('node', ['node_modules/gulp/bin/gulp.js', 'build.release'], {cwd: buildPath + '/paella'});
  cmd_npm.on('close', function (code) {
    cb(code);
  });
}));


gulp.task('paella-opencast:build', gulp.series('paella-opencast:compile.debug', function(){
  return gulp.src([
    buildPath + '/paella/build/player/**',
    paellaSrc + '/ui/**'

  ]).pipe(gulp.dest(buildPath + '/paella-opencast'));
}));



gulp.task('default', gulp.series('paella-opencast:build'));
