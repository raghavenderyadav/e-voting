'use strict';

angular
  .module('e-voting.voting.voting-question-directive', [])
  .directive('votingQuestion', votingQuestion);

function votingQuestion($injector) {
  return {
    link: link,
    templateUrl: 'components/voting/voting-question-directive.html',
    restrict: 'EA',
    scope: {
      votingQuestion: "=",
      votingChoice: "="
    }
  };

  function link(scope, element, attrs) {
    scope.totalVotes = $injector.get("$sessionStorage").totalVotes;
    scope.question = scope.votingQuestion;
  }
}