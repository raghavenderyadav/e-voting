var gulp = require('gulp');
var path = require('path');
var argv = require('minimist')(process.argv.slice(2));


var plugins = require('gulp-load-plugins')();
var browserSync = require('browser-sync');

// postcss
var cssnano = require('cssnano');
var autoprefixer = require('autoprefixer');

// check if production
var isOnProduction = (!!argv.production);
var browserSync = require('browser-sync').create();


var config = {
    src: 'app/_assets',
    dest: 'app'
}
var destPath = config.dest;

gulp.task('styles', function() {
  var processors = [
    require('precss')(),
    autoprefixer({browsers: ['last 2 version', '> 5%', 'safari 5', 'ios 6', 'android 4']}),
    cssnano({
      safe: true
    })
  ];

  return gulp.src(path.join(__dirname, config.src, '_styles/style.css'))
    .pipe(plugins.plumber({
      errorHandler: plugins.notify.onError('Error: <%= error.message %>')
    }))
    .pipe(plugins.postcss(processors))
    .pipe(gulp.dest(path.join(destPath, 'css/')))
    .pipe(browserSync.stream())
    .pipe(plugins.notify({
      message:'css up!',
      sound: 'Pop'
    }))
});

gulp.task('serve', function () {
    return browserSync.init( {
        open: true,
        reloadDelay: 500,
        port: 8000,
        server: {
            baseDir: path.join(__dirname, destPath)
        }
    } )
});

gulp.task('default', ['serve'], function(){
    gulp.watch('**/*.css', {cwd: path.join(__dirname, config.src, '_styles')}, ['styles', ])
})
