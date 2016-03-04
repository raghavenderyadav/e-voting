'use strict';

angular
  .module('e-voting.auth.sign-in-view', [])
  .controller('SignInController', ['signIn', function (signIn) {
    var sic = this;
    sic.signIn = signIn.signIn;
  }]);