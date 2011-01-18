/**
 * Builds a form panel for WMS Layers (Containing WMS specific options such as transparency).
 *
 */

WMSLayerFilterForm = function(activeLayerRecord, map) {

	var pos = new GControlPosition(G_ANCHOR_BOTTOM_LEFT, new GSize(0, -85));
	map.addControl(new MStatusControl({position:pos}));
	
	// create a drag control for each bounding box
	var bufferBbox = new MPolyDragControl({map:map,type:'rectangle',label:'Buffer'});
	var dataBbox = new MPolyDragControl({map:map,type:'rectangle',label:'Data'});
	
    var sliderHandler = function(caller, newValue) {
    	var overlayManager = activeLayerRecord.getOverlayManager();
    	var newOpacity = (newValue / 100);
    	
    	activeLayerRecord.setOpacity(newOpacity);
    	overlayManager.updateOpacity(newOpacity);
    };
    
    var drawBoundsButton = new Ext.Button({  
        text 	: 'Draw Bounding Box',
        width	: 110,
        handler : function() {
			bufferBbox.enableTransMarker();
			dataBbox.enableTransMarker();
			
			drawBoundsButton.hide();
    		clearBoundsButton.show();
    		sendToGridButton.setDisabled(false);
    	}
    });
    
    var clearBoundsButton = new Ext.Button({  
        text 	: 'Clear Bounding Box',
        width	: 110,
        hidden	: true,
        handler : function() {
    		bufferBbox.reset();
    		dataBbox.reset();
			
			drawBoundsButton.show();
			clearBoundsButton.hide();
			sendToGridButton.setDisabled(true);
    	}
    });
    
    var sendToGridButton = new Ext.Button({  
        text 	: 'Send to Grid',
        disabled: true,
        handler: function() {
        	
        	if (dataBbox.getParams() == null || bufferBbox.getParams() == null) {
        		Ext.Msg.alert("Error", 'You must draw the data and buffer bounds ' + 
        				'before submitting to the grid.');
        	}
        	else {
	        	Ext.Ajax.request({
	        		url: 'sendSubsetsToGrid.do' ,
	        		success: WMSLayerFilterForm.onSendToGridResponse,
	        		failure: WMSLayerFilterForm.onRequestFailure,
	        		params		: {
	        			layerName  		: activeLayerRecord.getLayerName(),
	        			dataCoords 		: dataBbox.getParams(),
	        			bufferCoords	: bufferBbox.getParams(),
	        			format			: fileTypeCombo.getValue()
	            	}
	        	});
        	}
        }
    });
    
    // subset file type selection values
	var fileTypes =  [
   		 ['NetCDF','nc'],
   		 ['GeoTIFF','geotif']
   	];
	
	var fileTypeStore = new Ext.data.SimpleStore({
		fields : ['type','value'],
        data   : fileTypes
    });
	
	var fileTypeCombo = new Ext.form.ComboBox({  
		tpl: '<tpl for="."><div ext:qtip="{type}" class="x-combo-list-item">{type}</div></tpl>',
        width          : 100,
        editable       : false,
        forceSelection : true,
        fieldLabel     : 'Subset File Type',
        mode           : 'local',
        store          : fileTypeStore,
        triggerAction  : 'all',
        typeAhead      : false,
        displayField   : 'type',
        valueField     : 'value',
        value          : 'nc',
        id			   : 'fileType',
        submitValue	   : false
    });
    
    //-----------Panel
    WMSLayerFilterForm.superclass.constructor.call(this, {
        id          : String.format('{0}',activeLayerRecord.getId()),
        border      : false,
        autoScroll  : true,
        hideMode    : 'offsets',
        //width       : '100%',
        labelAlign  : 'right',
        bodyStyle   : 'padding:5px',
        autoHeight:    true,
        layout: 'anchor',
        items:[ {
            xtype      :'fieldset',
            title      : 'WMS Properties',
            autoHeight : true,
            items      : [{
                    xtype       : 'slider',
                    fieldLabel  : 'Opacity',
                    minValue    : 0,
                    maxValue    : 100,
                    value       : (activeLayerRecord.getOpacity() * 100),
                    listeners   : {changecomplete: sliderHandler}
            },
            	fileTypeCombo,
            	drawBoundsButton,
            	clearBoundsButton,
            	sendToGridButton,
            	{
    				// column layout with 2 columns
                    layout:'column',
                    border: false,
                    bodyStyle:'margin:5px 0 0 0',
                    items:[{
                        // right column
                        border: false,
                        items:[drawBoundsButton,
                           	clearBoundsButton]
                    }
                    ,{
                        // right column
                        border: false,
                    	bodyStyle:'margin-left:5px',
                        items:[sendToGridButton]
                    }
                    ]
                }
            ]
        }]
    });
};

WMSLayerFilterForm.onSendToGridResponse = function(response, request) {
    var resp = Ext.decode(response.responseText);
    if (resp.error != null) {
        JobList.showError(resp.error);
    } else {
        Ext.Msg.alert("Success", "The selected coverage subsets have been added as inputs for the grid job.");
    }
};

//called when an Ajax request fails
WMSLayerFilterForm.onRequestFailure = function(response, request) {
	Ext.Msg.alert("Error", 'Could not execute last request. Status: '+
        response.status+' ('+response.statusText+')');
};

Ext.extend(WMSLayerFilterForm, Ext.FormPanel, {
    
});