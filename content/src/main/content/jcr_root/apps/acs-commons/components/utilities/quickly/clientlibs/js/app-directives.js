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

                scope.ui.scrollResults();

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

                scope.ui.scrollResults();

                event.preventDefault();
            }
        });
    };
});

quickly.directive('ngRight', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            var input = document.getElementById('acs-commons-quickly-cmd');

            if(input && input.value && input.value.length > 0
                && event.which === 39) {

                if(input.selectionEnd === input.value.length) {
                    // only execute if cursor is at the end of the input
                    scope.$apply(function (){
                        scope.$eval(attrs.ngRight);
                    });

                    event.preventDefault();
                }
            }
        });
    };
});