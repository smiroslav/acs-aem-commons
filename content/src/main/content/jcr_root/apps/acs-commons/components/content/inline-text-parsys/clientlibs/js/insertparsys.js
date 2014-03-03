/*global CKEDITOR: false, console: false */

( function() {
    var pluginName = 'insertparsys',

        attributesSet = function(label) {
            return {
                title: label,
                'aria-label': label,
                'class': 'cke_insertparsys',
                contenteditable: 'false',
                'data-cke-display-name': 'insertparsys',
                'data-cke-insertparsys': 1
            };
        },

        insertparsysCmd = {
            canUndo: false, // The undo snapshot will be handled by 'insertElement'.

            exec: function( editor ) {
                var div = editor.document.createElement( 'div', {
                    attributes: attributesSet(pluginName)
                });
                editor.insertElement( div );
            },

            allowedContent: 'div',
            requiredContent: 'div'
        };



    // Register a plugin named "insertparsys".
    CKEDITOR.plugins.add( pluginName, {
        lang: 'af,ar,bg,bn,bs,ca,cs,cy,da,de,el,en,en-au,en-ca,en-gb,eo,es,et,eu,fa,fi,fo,fr,fr-ca,gl,gu,he,hi,hr,hu,id,is,it,ja,ka,km,ko,ku,lt,lv,mk,mn,ms,nb,nl,no,pl,pt,pt-br,ro,ru,si,sk,sl,sq,sr,sr-latn,sv,th,tr,ug,uk,vi,zh,zh-cn', // %REMOVE_LINE_CORE%
        icons: 'pagebreak',
        hidpi: true,
        onLoad: function() {
            var cssStyles = (
                'background:url(' + CKEDITOR.getUrl( this.path + 'images/insertparsys.gif' ) + ') no-repeat center center;' +
                    'clear:both;' +
                    'width:100%;' +
                    'border-top:red 1px dotted;' +
                    'border-bottom:red 1px dotted;' +
                    'padding:0;' +
                    'height:5px;' +
                    'cursor:default;'
                ).replace( /;/g, ' !important;' );

            // Add the style that renders our placeholder.
            CKEDITOR.addCss( 'div.cke_insertparsys{' + cssStyles + '}' );
        },

        init: function(editor) {

            if (editor.blockless) {
                console.log("leaving early");
                return;
            }

            editor.addCommand( pluginName, insertparsysCmd );

            if(editor.ui.addButton) {

                editor.ui.addButton( 'InsertParsys', {
                    label: 'Insert Parsys',
                    command: pluginName,
                    toolbar: 'insert,82'
               });
            }
        },

        afterInit: function( editor ) {
            // Register a filter to displaying placeholders after mode change.
            var dataProcessor = editor.dataProcessor,
                dataFilter = dataProcessor && dataProcessor.dataFilter,
                htmlFilter = dataProcessor && dataProcessor.htmlFilter;

            console.log('afterinit');

            function upcastInsertParsys( element ) {
                CKEDITOR.tools.extend(element.attributes,
                    attributesSet('insertparsys'), true);
                element.children.length = 0;
            }

            if (htmlFilter) {
                console.log('htmlfilter');

                htmlFilter.addRules( {
                    attributes: {
                        'class': function(value, element) {
                            var attrs, span,
                                className = value.replace('cke_insertparsys', '');
                            if (className !== value) {
                                span = CKEDITOR.htmlParser.fragment.fromHtml( '<span>HELLO</span>' ).children[ 0 ];
                                element.children.length = 0;
                                element.add( span );

                                attrs = element.attributes;
                                delete attrs[ 'aria-label' ];
                                delete attrs.contenteditable;
                                delete attrs.title;
                            }
                            return className;
                        }
                    }
                }, { applyToAll: true, priority: 5 } );
            }

            if ( dataFilter ) {
                console.log('datafilter');
                dataFilter.addRules( {
                    elements: {
                        div: function( element ) {
                            // The "internal form" of a pagebreak is pasted from clipboard.
                            // ACF may have distorted the HTML because "internal form" is
                            // different than "data form". Make sure that element remains valid
                            // by re-upcasting it (#11133).
                            if(element.attributes['data-cke-insertparsys']) {
                                upcastInsertParsys(element);
                            }
                        }
                    }
                } );
            }
        }
    });
}());

