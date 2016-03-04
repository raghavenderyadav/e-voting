angular
  .module('e-voting.locale', [
    'ngI18n',
    'tmh.dynamicLocale',
    'e-voting.locale.loader'
  ])
  .config(['tmhDynamicLocaleProvider', function (tmhDynamicLocaleProvider) {
    tmhDynamicLocaleProvider.localeLocationPattern('components/i18n/bundle/resourceBundle_{{locale}}.json');
  }]);