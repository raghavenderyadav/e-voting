'use strict';

angular
  .module('e-voting.auth', [
    'e-voting.auth.sign-in',
    'e-voting.auth.sign-out',
    'e-voting.auth.user-info',
    'e-voting.auth.sign-in-view'
  ]);
