'use strict';

angular
  .module('e-voting.voting.voting-countdown-directive', [])
  .directive('votingCountdown', votingCountdown);

function votingCountdown($injector) {
  var countdownRefreshRate = 1000,
      countdownServerUpdateRate = 30000,
      countdownRefreshInterval = {},
      countdownServerUpdateInterval = {},
      interval = $injector.get("$interval"),
      votingInfo = $injector.get("votingInfo"),
      stateParams = $injector.get("$stateParams"),
      timeout = $injector.get("$timeout");

  function timestampToTime(timestamp) {
    var result = {
      days: 0,
      hours: 0,
      minutes: 0,
      seconds: 0
    }
    result.days = Math.floor(timestamp/24/60/60/1000);
    result.hours = Math.floor(timestamp/60/60/1000 - result.days*24);
    result.minutes = Math.floor(timestamp/60/1000 - result.days*24*60 - result.hours*60);
    result.seconds = Math.floor(timestamp/1000 - result.days*24*60*60 - result.hours*60*60 - result.minutes*60);
    if(result.hours.toString().length < 2) {
      result.hours = "0" + result.hours;
    }
    if(result.minutes.toString().length < 2) {
      result.minutes = "0" + result.minutes;
    }
    if(result.seconds.toString().length < 2) {
      result.seconds = "0" + result.seconds;
    }
    return result;
  }

  return {
    link: link,
    templateUrl: 'components/voting/voting-countdown-directive.html',
    restrict: 'EA',
    scope: {
      votingCountdown: "="
    }
  };

  function link(scope, element, attrs) {
    scope.timer = timestampToTime(scope.votingCountdown);
    scope.votingCountdownInitial = angular.copy(scope.votingCountdown);
    scope.isServerUpdateInProcess = false;

    countdownRefreshInterval = interval(function() {
      scope.votingCountdown -= countdownRefreshRate;
      if(scope.votingCountdown < 0) {
        interval.cancel(countdownRefreshInterval);
      } else {
        scope.timer = timestampToTime(scope.votingCountdown);
      }
    }, countdownRefreshRate);

    countdownServerUpdateInterval = interval(function() {
      scope.isServerUpdateInProcess = true;
      votingInfo.getTimer(stateParams.id, getTimerComplete)
    }, countdownServerUpdateRate);

    function getTimerComplete(data) {
      if(scope.votingCountdown < 0) {
        interval.cancel(countdownServerUpdateInterval);
      } else {
        scope.votingCountdown = data;
      }
      timeout(function() {
        scope.isServerUpdateInProcess = false;
      }, 500)
    }
  }
}
