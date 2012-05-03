/* 
 * Copyright (C) 2011 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// Get parent function for iframes / popups
function getParent() {
    if (window.opener){
        return window.opener;
    }else if (window.parent){
        return window.parent;
    }else{
        return window;
    }
}

function appendPanel(header, content, container) {
    var headerobj = document.getElementById(header);
    var contentobj = document.getElementById(content);
    if(headerobj && contentobj) {
        var headercontent = headerobj.innerText || headerobj.textContent;
        headerobj.style.display = 'none';
        contentobj.className += ' insidePanel';
        
        var panelContainer = container || Ext.getBody();
        var panel = Ext.create('Ext.panel.Panel', {
            title: headercontent,
            contentEl: contentobj,
            width: '100%',
            renderTo: panelContainer
        });
        Ext.EventManager.onWindowResize(function () {
            panel.doLayout();
        });
    }
}

var helpController = null;
Ext.onReady(function() {
    helpController = Ext.create('Ext.b3p.HelpController', {
        helppath: helppath
    });
    var helpLinks = Ext.select('.helplink');
    if(helpLinks.getCount() > 0) {
        helpLinks.on('click', function(evt, htmlel, eOpts) {
            helpController.showHelp(htmlel);
        }, '', {
            stopEvent: true
        });
    }
});

Ext.define('Ext.b3p.HelpController', {
    iframe: null,
    helppath: helppath,
    constructor: function(conf) {
        var me = this;
        me.initConfig(conf);
        me.iframeid = Ext.id();
        me.popupWindow = Ext.create('Ext.window.Window', {
            title: 'Help',
            closeAction: 'hide',
            hideMode: 'offsets',
            width: 600,
            height: 400,
            layout: 'fit',
            renderTo: Ext.getBody(),
            bodyStyle: {
                background: '#FFFFFF'
            },
            items : [{
                id: me.iframeid,
                xtype : "component",
                autoEl : {
                    tag : "iframe",
                    style: "border: 0px none;",
                    frameborder: 0
                }
            }]
        });
    },
    getIframe: function() {
        var me = this;
        if(me.iframe === null) me.iframe = Ext.get(me.iframeid);
        return me.iframe;
    },
    showHelp: function(htmlel) {
        var me = this;
        var extel = Ext.fly(htmlel);
        var iframe = me.getIframe();
        if(iframe) {
            var hash = extel.getAttribute('href');
            // IE fix, href in IE8 and lower is the complete URL + hash, not just the hash
            hash = hash.substring(hash.lastIndexOf('#'));
            var iframeurl = me.helppath + hash;                
            iframe.set({ src: iframeurl });
            me.popupWindow.show();
        }
    }
});

// Default grid config
var defaultGridConfig = {
    autoWidth: true,
    height: '100%',
    disableSelection: false,
    loadMask: true,
    viewConfig: {
        trackOver: true,
        stripeRows: true
    }
}
