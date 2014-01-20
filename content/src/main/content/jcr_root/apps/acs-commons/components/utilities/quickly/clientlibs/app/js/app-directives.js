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
/*global quickly: false, angular: false */


quickly.ui = {};
quickly.ui.scrollResults = function() {

    var container = $('#acs-commons-quickly-app .results'),
        selected = $('#acs-commons-quickly-app .result.selected'),

        containerHeight = container.height(),
        containerTop = container.scrollTop(),
        containerBottom = containerTop + containerHeight,

        selectedHeight = selected.outerHeight(true),
        selectedTop = selected.offset().top - container.offset().top + containerTop,
        selectedBottom = selectedTop + selectedHeight;

    if(selectedBottom > containerBottom) {
        // Scroll down
        container.scrollTop(selectedTop + selectedHeight - containerHeight);
    } else if(selectedTop < containerTop) {
        // Scroll Up
        container.scrollTop(containerTop - selectedHeight);
    }
};

quickly.directive('ngEnter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if(event.which === 13) {
                scope.$apply(function (){
                    scope.$eval(attrs.ngEnter);
                });

                event.preventDefault();
            }
        });
    };
});

/*
 left = 37
 up = 38
 right = 39
 down = 40
*/

quickly.directive('ngUp', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if(event.which === 38) {
                scope.$apply(function (){
                    scope.$eval(attrs.ngUp);
                });

                quickly.ui.scrollResults();

                event.preventDefault();
            }
        });
    };
});

quickly.directive('ngDown', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if(event.which === 40) {
                scope.$apply(function (){
                    scope.$eval(attrs.ngDown);
                });

                quickly.ui.scrollResults();

                event.preventDefault();
            }
        });
    };
});