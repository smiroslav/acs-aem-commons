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

/*global angular: false, JSON: false */

var bulkPropertyManagerApp = angular.module('bulkPropertyManagerApp', []);

bulkPropertyManagerApp.controller('MainCtrl', function ($scope, $http, $timeout) {

    $http.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';

    $scope.app = {
        resource: '',
        running: false
    };

    $scope.notifications = [];

    $scope.form = {
        queryMode: 'constructed',
        raw: {},
        constructed: {
            collectionMode: 'traversal',
            properties: [
                { name: '', value: ''}
            ],
            propertiesOperand: 'OR'
        },
        add: {},
        copy: {},
        remove: {},
        move: {},
        findAndReplace: {}
    };

    $scope.results = [];
    $scope.dryRunResults = [];


    $scope.addNotification = function (type, title, message) {
        var timeout = 30000;

        if(type === 'success')  {
            timeout = timeout / 2;
        }

        $scope.notifications.push({
            type: type,
            title: title,
            message: message
        });

        $timeout(function() {
            $scope.notifications.shift();
        }, timeout);
    };

});

