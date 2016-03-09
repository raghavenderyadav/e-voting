'use strict';

// Declare app level module which depends on views, and components
angular
  .module('e-voting', [
    'ui.router',
    'e-voting.routing',
    'e-voting.auth',
    'e-voting.voting',
    'e-voting.header',
    'e-voting.server-properties',
    'e-voting.api-requests'
  ]);
