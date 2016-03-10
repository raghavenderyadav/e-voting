'use strict';

angular
  .module('e-voting.voting', [
    'e-voting.voting.voting-list-model',
    'e-voting.voting.voting-list-view',
    'e-voting.voting.voting-result-model',
    'e-voting.voting.voting-result-view',
    'e-voting.voting.voting-question-model',
    'e-voting.voting.voting-question-view',
    'e-voting.voting.voting-question-directive',
    'e-voting.voting.confirmed-votes-list-model',
    'e-voting.voting.confirmed-votes-list-view',
    'e-voting.voting.voting-countdown-directive'
  ]);
