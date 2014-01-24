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

quickly.controller('QuicklyCtrl', ['$scope', '$http', '$timeout', '$cookies', function($scope, $http, $timeout, $cookies){

    /* Data/Models */
    $scope.data = {
        cmd: '',
        results: []
    };

    /* Method namespaces */
    $scope.app = {
        resultTime: 0,
        timeout: 0,
        timeoutThrottle: 400,
        keyStrokeThrottle: 3,
        keyStrokes: 0,
        resetOnToggle: true,
        visible: false
    };

    /* Watchers */
    $scope.$watch('data.cmd', function(newValue, oldValue) {
        if($scope.app.visible) {
            clearTimeout($scope.app.timeout);
            if($scope.app.keyStrokes >= $scope.app.keyStrokeThrottle) {
                $scope.app.keyStrokes = 0;
                $scope.app.getResults();
            } else {
                $scope.app.keyStrokes = $scope.app.keyStrokes + 1;
                $scope.app.timeout = setTimeout(function() { $scope.app.getResults(); }, $scope.app.keyStrokeThrottle);
            }
        }
    });




    $scope.app.getResults = function() {
        var requestTime = new Date().getTime();

        if(!$scope.data.cmd) {
            $scope.data.results = [];
            return;
        }

        $http({
            method: 'GET',
            url: '/bin/quickly.json',
            params: {
                t: requestTime,
                cmd: $scope.data.cmd
            }
        }).success(function(data, status, headers, config) {
            if(requestTime <= $scope.app.resultTime) {
                // This check prevents previous slow running queries from
                // overwriting results of faster later queries
                return;
            }

            $scope.app.resultTime = requestTime;
            $scope.data.results = data.results || [];

            if($scope.data.results && $scope.data.results[0]) {
                $scope.data.results[0].selected = true;
                $scope.ui.resetResultScroll();
            }
        });
    };

    $scope.app.toggle = function() {
        if($scope.app.resetOnToggle) {
            $scope.data.cmd = '';
            $scope.data.results = [];
        }
        $scope.app.visible = !$scope.app.visible;
        $scope.ui.focusCommand();
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

    $scope.app.selectRight = function() {
        var i = $scope.util.findSelectedIndex(),
            result = $scope.data.results[i];

        if(result.path) {
            $scope.data.cmd = $scope.util.getCommandOp($scope.data.cmd) + ' ' + result.path;

            $scope.ui.focusCommand();
            $scope.ui.scrollCommandInputToEnd();
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
        if($scope.app.visible) {
            if(result.action.method !== 'noop') {
                $scope.ui.createForm(result.action).submit();
                $scope.app.visible = false;
            }
        }
    };


    /* Util Methods */
    $scope.util = {};

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

    $scope.util.buildFormHTML = function(action) {
        var params = action.params || [],
            html = '<form';
            html += ' action="' + (action.actionURI || '#') + '"';
            html += ' method="' + (action.method || 'get') + '"';
            html += ' target="' + (action.target || '_top') + '">';

        angular.forEach(params, function(value, key) {
            html += '<input type="hidden" name="' + key + '" value="' + value + '"/>';
        }, this);

        html += '</form>';

        return html;
    };

    $scope.util.getCommandOp = function(cmd) {
        var endIndex = cmd.indexOf(' ');
        if(endIndex < 0) {
            endIndex = cmd.length - 1;
        }

        if(cmd) {
            return cmd.substring(0, endIndex);
        } else {
            return '';
        }
    };

    /* UI Methods */
    $scope.ui = {};

    $scope.ui.focusCommand = function() {
        setTimeout(function() {
            angular.element('#acs-commons-quickly-cmd').focus();
        }, 0);
    };

    $scope.ui.scrollCommandInputToEnd = function() {
        var input = document.getElementById('acs-commons-quickly-cmd');
        if(input) {
            $timeout(function() { input.scrollLeft = input.scrollWidth; });
        }
    };

    $scope.ui.createForm = function(action) {
        var formWrapper = angular.element('#quickly-result-form'),
            form = angular.element($scope.util.buildFormHTML(action));

        formWrapper.html('').append(form);

        return form;
    };

    $scope.ui.resetResultScroll = function() {
        $('#acs-commons-quickly-app .quickly-results').scrollTop(0);
    };

    $scope.ui.scrollResults = function() {
        var container = $('#acs-commons-quickly-app .quickly-results'),
            selected = $('#acs-commons-quickly-app .quickly-result.selected'),
            containerHeight,
            containerTop,
            containerBottom,
            selectedHeight,
            selectedTop,
            selectedBottom;

        if(!selected || !selected.length) {
            return;
        }

        containerHeight = container.height();
        containerTop = container.scrollTop();
        containerBottom = containerTop + containerHeight;

        selectedHeight = selected.outerHeight(true);
        selectedTop = selected.offset().top - container.offset().top + containerTop;
        selectedBottom = selectedTop + selectedHeight;

        if(selectedBottom > containerBottom) {
            // Scroll down
            container.scrollTop(selectedTop + selectedHeight - containerHeight);
        } else if(selectedTop < containerTop) {
            // Scroll Up
            container.scrollTop(containerTop - selectedHeight);
        }
    };


    /* Command Support */
    $scope.cmd = {};


    $scope.cmd.back = function() {
        var cookie = $cookies.acs_quickly_back || '[]',
            cookieHistory = JSON.parse(cookie),
            history = [],
            entry,
            maxSize = 25,
            i = 0,
            j = 1;

        entry = {
            title: document.title || 'Unnamed Page',
            uri: (window.location.pathname  + window.location.search + window.location.hash) || ''
        };

        if(entry.title && entry.uri) {
            for(i = 0; i < cookieHistory.length && j < maxSize; i += 1) {
               if(cookieHistory[i]
                   && cookieHistory[i].title !== entry.title
                   && cookieHistory[i].uri !== entry.uri) {
                    // Not the same as current, so add to history
                    history.push(cookieHistory[i]);
                    j += 1;
                }
            }

            // Add history onto the front
            history.unshift(entry);
        } else {
            history = cookieHistory;
        }

        // Save data back to cookie
        $cookies.acs_quickly_back = JSON.stringify(history);
    };

    /* Initialization */

    var init = function() {
        // Prevent flickering onload
        $timeout(function() { angular.element('#acs-commons-quickly').css('display', 'block'); }, 1000);

        // Delay until AEM has a chance to update DOM/document properties
        $timeout(function() { $scope.cmd.back(); }, 2500);
    };

    init();
}]);
