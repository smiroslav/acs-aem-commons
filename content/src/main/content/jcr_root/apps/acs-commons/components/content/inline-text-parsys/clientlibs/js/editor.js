/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

/*global CKEDITOR: false, confirm: false, ACS: false, CQ: false, setTimeout: false */

ACS.CQ.wcm.LongFormText = {

    getID: function (path) {
        return 'acs-commons-long-form-editor-id_' + path;
    },

    getEl: function (path) {
        return document.getElementById(this.getID(path));
    },

    handleTextEdit: function (component) {
        this.switchContext(component, component.path + '.views.edit-text.html');
    },

    handleComponentsEdit: function (component) {
        this.switchContext(component, component.path + '.views.parsys.html');
    },

    handlePreview: function (component) {
        this.switchContext(component, component.path + '.html');
    },

    handleCancel: function (component) {
        this.clearEditor(component.path);
        setTimeout(function () {
            component.refresh(CQ.HTTP.noCaching(component.path + '.html'));
        }, 100);
    },

    switchContext: function (component, uri) {
        var editor = this.getEditor(component.path),
            el = ACS.CQ.wcm.LongFormText.getEl(component.path);

        if (editor && el && editor.checkDirty()) {
            this.save(component.path, editor.getData(), function () {
                component.refresh(CQ.HTTP.noCaching(uri));
            });
        } else {
            component.refresh(CQ.HTTP.noCaching(uri));
        }
    },

    getEditor: function (path) {
        return CKEDITOR.instances[this.getID(path)];
    },

    clearEditor: function (path) {
        var editor = this.getEditor(path),
            el = this.getEl(path);

        if (editor && el) {
            editor.setData(el.getAttribute('data-reset'));
        }
    },

    save: function (path, textData, callback) {
        var data = {
            text: textData
        };

        CQ.HTTP.post(path, function () {
            var el = ACS.CQ.wcm.LongFormText.getEl(path);

            if (el) {
                el.setAttribute('data-reset', textData);
            }

            if (callback) {
                callback();
            }
        }, data);
    },

    init: function (el) {

        var editorID = el.getAttribute('id'),
            action = el.getAttribute('data-action'),
            page = el.getAttribute('data-current-page'),
            configURI = el.getAttribute('data-config'),
            ckEditor;

        ckEditor = CKEDITOR.replace(editorID, {
            customConfig: configURI
        });

        el.setAttribute('data-reset', ckEditor.getData());

        ckEditor.on('instanceReady', function () {
            ckEditor.addCommand('save', {
                modes: { wysiwyg: 1, source: 1 },
                exec: function () {
                    ACS.CQ.wcm.LongFormText.save(action, ckEditor.getData());
                }
            });

        });

        window.onbeforeunload = function (e) {
            if (ckEditor && ckEditor.checkDirty()) {
                return 'There are unsaved changes to your long form text. Navigating away from this page will loose those changes.';
            }
        };
    }
};
