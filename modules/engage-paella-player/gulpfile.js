"use strict"
var gulp = require('gulp');
var spawn = require('child_process').spawn;
var mergeStream = require('merge-stream');


var buildPath = "target/gulp";
var paellaSrc = "src/main/paella-opencast"


gulp.task('paella-opencast:prepare:source', function(){
	var s1 = gulp.src('node_modules/paellaplayer/**').pipe(gulp.dest(buildPath + '/paella'));
	var s2 = gulp.src(paellaSrc + '/plugins/**').pipe(gulp.dest(buildPath + '/paella/plugins'));

	return mergeStream(s1,s2);
});



gulp.task('paella-opencast:prepare', ['paella-opencast:prepare:source'], function(cb){
	var cmd_npm = spawn('npm', ['install'], {cwd: buildPath + '/paella', stdio: 'inherit'});
	cmd_npm.on('close', function (code) {
		cb(code);
	});
});


gulp.task('paella-opencast:compile.debug', ['paella-opencast:prepare'], function(cb){
	var cmd_npm = spawn('node', ['node_modules/gulp/bin/gulp.js', 'build.debug'], {cwd: buildPath + '/paella'/*, stdio: 'inherit'*/});
	cmd_npm.on('close', function (code) {
		cb(code);
	});	
});

gulp.task('paella-opencast:compile.release', ['paella-opencast:prepare'], function(cb){
	var cmd_npm = spawn('node', ['node_modules/gulp/bin/gulp.js', 'build.release'], {cwd: buildPath + '/paella'/*, stdio: 'inherit'*/});
	cmd_npm.on('close', function (code) {
		cb(code);
	});
});


gulp.task('paella-opencast:build', ["paella-opencast:compile.debug"], function(){
	return gulp.src([
		buildPath + '/paella/build/player/**',
		paellaSrc + '/ui/**'
		
	]).pipe(gulp.dest(buildPath + '/paella-opencast'));	
});



gulp.task('default', ['paella-opencast:build']);	


