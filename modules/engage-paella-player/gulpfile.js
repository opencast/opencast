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
var request = require('request');
var source = require('vinyl-source-stream');
var gunzip = require('gulp-gunzip');
var untar = require('gulp-untar');

var PAELLA_VERSION = '6.4.3';

var buildPath = 'target/gulp',
    paellaSrc = 'src/main/paella-opencast',
    paellaBuildPath = buildPath + '/paella-' + PAELLA_VERSION;


gulp.task('paella-opencast:download:paella', function(){
  return request('https://github.com/polimediaupv/paella/archive/' + PAELLA_VERSION + '.tar.gz')
  .pipe(source(PAELLA_VERSION + '.tar.gz'))
  .pipe(gunzip())
  .pipe(untar())
  .pipe(gulp.dest(buildPath));
});


gulp.task('paella-opencast:prepare:source', gulp.series('paella-opencast:download:paella', function(){
  return gulp.src(paellaSrc + '/plugins/**').pipe(gulp.dest(paellaBuildPath + '/plugins'));
}));


gulp.task('paella-opencast:prepare', gulp.series('paella-opencast:prepare:source', function(cb){
  var cmd_npm = spawn('npm', ['ci'], {cwd: paellaBuildPath });
  cmd_npm.on('close', function (code) {
    cb(code);
  });
}));


gulp.task('paella-opencast:compile.debug', gulp.series('paella-opencast:prepare', function(cb){
  var cmd_npm = spawn('node', ['node_modules/gulp/bin/gulp.js', 'build.debug'], {cwd: paellaBuildPath});
  cmd_npm.on('close', function (code) {
    cb(code);
  });
}));

gulp.task('paella-opencast:compile.release', gulp.series('paella-opencast:prepare', function(cb){
  var cmd_npm = spawn('node', ['node_modules/gulp/bin/gulp.js', 'build.release'], {cwd: paellaBuildPath});
  cmd_npm.on('close', function (code) {
    cb(code);
  });
}));


gulp.task('paella-opencast:build', gulp.series('paella-opencast:compile.debug', function(){
  return gulp.src([
    paellaBuildPath + '/build/player/**',
    paellaSrc + '/ui/**'

  ]).pipe(gulp.dest(buildPath + '/paella-opencast'));
}));



gulp.task('default', gulp.series('paella-opencast:build'));
