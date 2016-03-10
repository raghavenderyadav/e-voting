/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 * *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 * *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 * *
 * Removal or modification of this copyright notice is prohibited.            *
 * *
 ******************************************************************************/

'use strict';

angular.module('e-voting.routing', [
    'ngResource'
  ])
  .config(['$stateProvider', '$urlRouterProvider', '$locationProvider', '$resourceProvider',
    function ($stateProvider, $urlRouterProvider, $locationProvider, $resourceProvider) {

      $urlRouterProvider.rule(function ($injector, $location) {
        var path = $location.path(), search = $location.search();
        if (path[path.length - 1] !== '/') {
          if (angular.equals(search, {})) {
            return path + '/';
          } else {
            var params = [];
            angular.forEach(search, function (v, k) {
              params.push(k + '=' + v);
            });
            return path + '/?' + params.join('&');
          }
        }
      });
      //$urlRouterProvider.otherwise(function ($injector) {
      //  $injector.get("$timeout")(function () {
      //    $("<form action='/404/" + $injector.get("$sessionStorage").locale + "/' target='_self'/></form>").appendTo("body").submit();
      //  }, 0);
      //});
      $stateProvider
        .state('votingList', {
          url: '/votingList/:locale/',
          templateUrl: 'views/votingList/votingList.html',
          controller: 'VotingListController',
          controllerAs: 'vlc',
          params: {
            isLoginRequired: false,
            locale: {
              value: 'en-gb',
              squash: true
            }
          }
        })
        .state('votingResult', {
          url: '/votingResult/:locale/?id',
          templateUrl: 'views/votingResult/votingResult.html',
          controller: 'VotingResultController',
          controllerAs: 'vrc',
          params: {
            isLoginRequired: false,
            locale: {
              value: 'en-gb',
              squash: true
            },
            id: undefined
          }
        })
        .state('confirmedVotes', {
          url: '/confirmedVotes/:locale/?id',
          templateUrl: 'views/confirmedVotes/confirmedVotesList.html',
          controller: 'ConfirmedVotesListController',
          controllerAs: 'cvlc',
          params: {
            isLoginRequired: false,
            locale: {
              value: 'en-gb',
              squash: true
            },
            id: undefined
          }
        })
        .state('voting', {
          url: '/voting/:locale/?id',
          templateUrl: 'views/voting/voting.html',
          controller: 'VotingController',
          controllerAs: 'vc',
          params: {
            isLoginRequired: false,
            locale: {
              value: 'en-gb',
              squash: true
            },
            id: undefined
          }
        })
        .state('signIn', {
          url: '/:locale/',
          templateUrl: 'views/signIn/signIn.html',
          controller: 'SignInController',
          controllerAs: 'sic',
          params: {
            isLoginRequired: false,
            locale: {
              value: 'en-gb',
              squash: true
            }
          }
        });
      $locationProvider.html5Mode(true).hashPrefix('!');
      $resourceProvider.defaults.stripTrailingSlashes = false;
    }
  ]);
