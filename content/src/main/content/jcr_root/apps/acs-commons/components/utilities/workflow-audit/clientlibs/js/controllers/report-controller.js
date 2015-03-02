/*global JSON: false, angular: false, workflowAuditApp: false */

workflowAuditApp.controller('ReportCtrl', ['$scope', '$http', function($scope, $http) {
    $scope.app = {
        uri: ''
    };

    $scope.rowCollection = {};

    $scope.displayedCollection = [].concat($scope.rowCollection);

    $scope.load = function() {
        $http({
            method: 'GET',
            url: $scope.app.uri,
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).
            success(function(data, status, headers, config) {
                $scope.rowCollection = data || [];
            }).
            error(function(data, status, headers, config) {
            });
    };
}]);
