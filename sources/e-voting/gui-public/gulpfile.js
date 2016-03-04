var gulp = require('gulp');
var path = require('path');
var config = require('./config.json');
var argv = require('minimist')(process.argv.slice(2));

var plugins = require('gulp-load-plugins')();
var browserSync = require('browser-sync');

// postcss
var cssnano = require('cssnano');
var autoprefixer = require('autoprefixer');
var postcsAsssets = require('postcss-assets');
var flexboxfixer = require('postcss-flexboxfixer');

// check if production
var isOnProduction = (!!argv.production);

var destPath = isOnProduction ? config.build : config.tmp;

gulp.task('styles', function() {
  var processors = [
    flexboxfixer,
    autoprefixer({browsers: ['last 2 version', '> 5%', 'safari 5', 'ios 6', 'android 4']}),
    postcsAsssets({loadPaths: [config.src]}),
    cssnano({
      safe: true
      // sourcemap: true
    })
  ];
  // if (argv.production) {
  //   processors.push(cssnano())
  // };

  return gulp.src(path.join(config.src, '_styl/styles.styl'))
    .pipe(plugins.plumber({
      errorHandler: plugins.notify.onError('Error: <%= error.message %>')
    }))
    .pipe(plugins.if(!isOnProduction, plugins.sourcemaps.init()))
    .pipe(plugins.stylus({
      compress: false,
      url: 'url64',
      'include css': true
    }))
    .pipe(plugins.postcss(processors))
    .pipe(plugins.if(!isOnProduction, plugins.sourcemaps.write('./')))
    .pipe(gulp.dest(path.join(destPath, 'css/')))
    .pipe(plugins.notify({
      message:'css up!',
      sound: 'Pop'
    }))
    .pipe(browserSync.stream());
});