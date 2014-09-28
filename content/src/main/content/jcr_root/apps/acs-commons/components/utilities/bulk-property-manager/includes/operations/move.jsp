<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2013 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>

<div ng-controller="MoveCtrl"
     ng-show="form.mode === 'move'">

    <div class="form-row">
        <h4>From Property</h4>

        <span>
            <input type="text"
                   ng-required="true"
                   ng-model="form.move.src"
                   placeholder="The property to copy the value from"/>
        </span>
    </div>

    <div class="form-row">
        <h4>To Property</h4>

        <span>
            <input type="text"
                   ng-required="true"
                   ng-model="form.move.dest"
                   placeholder="The property to copy the value to"/>
        </span>
    </div>

    <div class="form-row">
        <div class="form-left-cell">&nbsp;</div>

        <button ng-click="move(true)"
                class="submit-button">Dry Run</button>

        <button ng-click="move(false)"
                class="submit-button primary">Move Property</button>
    </div>
</div>