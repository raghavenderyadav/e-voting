'use strict';

angular
  .module('e-voting.voting.voting-question-directive', [])
  .directive('votingQuestion', votingQuestion);

function votingQuestion() {
  return {
    link: link,
    templateUrl: 'components/voting/voting-question-directive.html',
    restrict: 'EA',
    scope: {
      votingQuestion: "=",
      votingChoice: "=",
      totalVotes: "="
    }
  };

  function link(scope, element, attrs) {
    scope.question = scope.votingQuestion;
  }
}
