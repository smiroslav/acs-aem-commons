/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/*global quickly: false, angular: false, console: false */

quickly.controller('QuicklyCtrl', ['$scope', '$http', '$timeout', function($scope, $http, $timeout){

    /* Data/Models */
    $scope.data = {
        cmd: "",
        form: {
            actionURI: '#',
            method: 'get',
            target: '_self',
            params: []
        },
        results: []
    };

    /* Method namespaces */
    $scope.app = {
        timeout: 0,
        keyStrokes: 0,
        resetOnToggle: true,
        visible: false
    };
    $scope.util = {};

    /* Watchers */
    $scope.$watch('data.cmd', function(newValue, oldValue) {
        if($scope.app.visible) {
            clearTimeout($scope.app.timeout);
            if($scope.app.keyStrokes >= 2) {
                $scope.app.keyStrokes = 0;
                $scope.app.getResults();
            } else {
                $scope.app.keyStrokes = $scope.app.keyStrokes + 1;
                $scope.app.timeout = setTimeout(function() { $scope.app.getResults(); }, 300);
            }
        }
    });

    $scope.$watch('data.form', function(newValue, oldValue) {
        if($scope.app.visible) {
            setTimeout(function() {
                angular.element('#quickly-result-form').submit();
            }, 100);
            $scope.app.visible = false;
        }
    });

    $scope.app.getResults = function() {
        if(!$scope.data.cmd) {
            $scope.data.results = [];
            return;
        }

        $http({
            method: 'GET',
            url: '/bin/quickly.json',
            params: {
                t: new Date().getTime(),
                cmd: $scope.data.cmd
            }
        }).success(function(data, status, headers, config) {
            $scope.data.results = data.results || [];

            if($scope.data.results && $scope.data.results[0]) {
                $scope.data.results[0].selected = true;
            }
        });
    };

    $scope.app.toggle = function() {
        if($scope.app.resetOnToggle) {
            $scope.data.cmd = '';
            $scope.data.results = [];
        }
        $scope.app.visible = !$scope.app.visible;
        setTimeout(function(){document.getElementById('acs-commons-quickly-cmd').focus();} ,0);
    };

    $scope.app.selectUp = function() {
        var i = $scope.util.findSelectedIndex();
        if(i > 0) {
            $scope.util.select($scope.data.results[i - 1]);
        }
    };

    $scope.app.selectDown = function() {
        var i = $scope.util.findSelectedIndex();
        if(i < $scope.data.results.length - 1) {
            $scope.util.select($scope.data.results[i + 1]);
        }
    };

     $scope.app.processSelected = function() {
         var i = $scope.util.findSelectedIndex();
         $scope.app.processAction($scope.data.results[i]);
     };

     $scope.app.selectResult = function(result) {
        $scope.util.select(result);
        $scope.app.processAction(result);
    };

    $scope.app.processAction = function(result) {
        // Actual form submission happens in the watcher for data.form
        $scope.data.form = result.action;
    };


    /* Util Methods */

    $scope.util.select = function(result) {
        var i = 0;
        for(; i < $scope.data.results.length; i++) {
            $scope.data.results[i].selected = false;
        }

        result.selected = true;
    };

    $scope.util.findSelectedIndex = function() {
        var i = 0;
        for(; i < $scope.data.results.length; i++) {
            if($scope.data.results[i].selected) {
                return i;
            }
        }

        return 0;
    };
}]);
