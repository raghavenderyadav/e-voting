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

angular
  .module('e-voting.locale.loader', [])
  .service('resourceBundle', ['$rootScope', 'ngI18nResourceBundle', '$sessionStorage', function ($rootScope, ngI18nResourceBundle, $sessionStorage) {

    var _this = this;
    _this.resourceBundle = {};
    var loadCss = function(path) {
      var head  = document.getElementsByTagName('head')[0];
      var link  = document.createElement('link');
      link.rel  = 'stylesheet';
      link.type = 'text/css';
      link.href = path;
      link.media = 'all';
      head.appendChild(link);
    };
    var unloadCss = function(path){
      var targetelement = "link";
      var targetattr = "href";
      var allsuspects=document.getElementsByTagName(targetelement);
      for (var i=allsuspects.length; i>=0; i--){ //search backwards within nodelist for matching elements to remove
        if (allsuspects[i] && allsuspects[i].getAttribute(targetattr)!=null && allsuspects[i].getAttribute(targetattr).indexOf(path)!=-1)
          allsuspects[i].parentNode.removeChild(allsuspects[i]); //remove element by calling parentNode.removeChild()
      }
    };

    $rootScope.$watch('i18n.language', function (language) {
      if(language !== undefined) {
        ngI18nResourceBundle.get({locale: language}).success(function (resourceBundle) {
          _this.resourceBundle = resourceBundle;
          unloadCss("/js/i18n/css/style_" + $sessionStorage.locale + ".css");
          loadCss("/js/i18n/css/style_" + language + ".css");
          $sessionStorage.locale = language;
          tmhDynamicLocale.set($sessionStorage.locale);
        });
      }
    });


  }]);
