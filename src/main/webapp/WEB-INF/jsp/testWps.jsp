<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">

<title>WPS Client</title>
<link rel="shortcut icon" href="favicon.ico" />

<link type="text/css" rel="stylesheet" href="css/wps.css">
<style type="text/css">
#content {
	margin-top: 10px;
	margin-right: auto;
	margin-left: auto;
	width: 780px;
	font-family: Verdana, Arial, Helvetica, sans-serif;
}

#header {
	position: relative;
	height: 80px;
	margin-bottom: 20px;
}

#headline {
	position: absolute;
	top: 0;
	left: 0;
}

#logo {
	position: absolute;
	top: 0;
	right: 0;
}

#url-form {
	float: none;
	margin-top: 8em;
}

#requestTextarea {
	width: 780px;
	height: 320px;
	/* border-color: #63C4E4; */
}

h1,.title {
	font-size: 20pt;
	margin-right: 6pt;
}

h3 {
	font-size: 12pt;
	font-weight: bold;
}

.request-form {
	margin-bottom: 12px;
}

.request-form-buttons {
	margin: 6px;
}

.editorBorder {
	border: 1px solid #eee;
	padding: 0px;
}

.CodeMirror-line-numbers {
	width: 2.2em;
	color: #aaa;
	background-color: #eee;
	text-align: right;
	padding-right: .3em;
	font-size: 10pt;
	font-family: monospace;
	padding-top: .4em;
}
</style>

<script type="text/javascript" src="CodeMirror-2.33/lib/codemirror.js"></script>

<script language="JavaScript" type="text/javascript">
	// derive service url from current location
	var urlIndex = window.location.href.lastIndexOf("/testWps.html");
	var urlBasisString = window.location.href.substring(0, (urlIndex + 1));
	var serviceUrlString = urlBasisString + "wps/WebProcessingService";

	var datafolder = "requests/";

	var editor = null;
	var defaultString = "<!-- Insert your request here or select one of the examples from the menu above. -->";

	function load() {

		var placeholderIndex = "PLACEHOLDER";
		//load files
		var requests = new Array();
		requests[100] = datafolder + "GetCapabilities.xml";
		requests[101] = datafolder + "DescribeProcess.xml";
		requests[102] = datafolder + "ListJobs.xml";
		requests[103] = datafolder + "EchoProcess_response-both.xml";
/* 		requests[103] = datafolder + "EchoProcess_raw-complex.xml";
		requests[104] = datafolder + "EchoProcess_reference-complex.xml";
		requests[104] = datafolder + "jtsconvexhull_request.xml";

		requests[120] = datafolder + "SimpleBuffer.xml";
		requests[121] = datafolder + "SimpleBufferRemoteWFSPOST.xml";
		requests[123] = datafolder + "Unionrequest.xml";

 		var rasters = 200;
		requests[rasters] = datafolder
				+ "r.contour_request_all_bands_out_gml.xml";
		requests[rasters + 1] = datafolder
				+ "r.contour_request_all_bands_out_shp.xml";
		requests[rasters + 2] = datafolder + "r.contour_request_out_gml.xml";
		requests[rasters + 3] = datafolder + "r.contour_request_out_shp.xml";
		requests[rasters + 4] = datafolder + "r.los_request_out_img.xml";
		requests[rasters + 6] = datafolder + "r.los_request_out_tiff.xml";
		requests[rasters + 7] = datafolder + "r.neighbors_request.xml";
		requests[rasters + 8] = datafolder + "r.resample_request_out_tiff.xml";
		requests[rasters + 9] = datafolder
				+ "r.resample_request_out_netcdf.xml";
		requests[rasters + 10] = datafolder + "r.to.vect_request_out_gml.xml";
		requests[rasters + 11] = datafolder + "r.to.vect_request_out_shp.xml";
		requests[rasters + 12] = datafolder + "r.watershed_request.xml";
		requests[rasters + 13] = datafolder + "r.math_request.xml";

		requests[300] = datafolder + "v.buffer_request_in_kml.xml";
		requests[301] = datafolder + "v.buffer_request_in_dgn.xml";
		requests[302] = datafolder + "v.buffer_request_out_gml.xml";
		requests[303] = datafolder + "v.buffer_request_out_kml.xml";
		requests[304] = datafolder + "v.buffer_request_out_shp.xml";
		requests[305] = datafolder + "v.delaunay_request_out_gml.xml";
		requests[306] = datafolder + "v.delaunay_request_out_shp.xml";
		requests[307] = datafolder + "v.hull_request_out_gml.xml";
		requests[308] = datafolder + "v.hull_request_out_kml.xml";
		requests[309] = datafolder + "v.hull_request_out_shp.xml";
		requests[310] = datafolder + "v.to.rast_request.xml";

		requests[400] = datafolder + "R_echo.xml";
		requests[402] = datafolder + "R_SosPlot.xml";
		requests[403] = datafolder + "R_pegel-report.xml";
		requests[404] = datafolder + "R_pegel-report_pdf.xml";
		requests[405] = datafolder + "R_sweave-foo.xml";
		requests[406] = datafolder + "R_sweave-foo_pdf.xml";
		requests[407] = datafolder + "R_Idw.xml";
		requests[408] = datafolder + "R_image.xml";
		requests[410] = datafolder + "R_uniform.xml";
		requests[411] = datafolder + "R_meuse.xml";
		requests[412] = datafolder + "R_meuse2.xml";
		requests[413] = datafolder + "R_EO2H_AirQualitySaxony.xml";
		requests[414] = datafolder + "R_enviroCar_mapMatching.xml";

		var mcs = 500;
		requests[mcs] = datafolder + "mc_echo.xml";
		 */

		//fill the select element
		var selRequest = document.getElementById("selRequest");

		l = requests.length;
		for ( var i = 0; i < l; i++) {
			var requestString = "";
			if (requests[i] == placeholderIndex) {
				//skip this one
			} else if (requests[i]) {
				try {

					var name = requests[i].substring(requests[i]
							.lastIndexOf("/") + 1, requests[i].length);

					selRequest.add(new Option(name, requests[i]), null);
				} catch (err) {
					var txt = "";
					txt += "Error loading file: " + requests[i];
					txt += "Error: " + err + "\n\n";
					var requestTextarea = document
							.getElementById('requestTextarea').value = "";
					requestTextarea.value += txt;
				}
			} else {
				// request is null or empty string - do nothing
			}
		}

		// Put service url into service url field
		var serviceUrlField = document.getElementById("serviceUrlField");
		serviceUrlField.value = serviceUrlString;
/* 		if (editor == null) {
			initEditor();
		}
 */
	}

	function insertSelected() {
		try {
			var selObj = document.getElementById('selRequest');
			var requestTextarea = document.getElementById('requestTextarea');
			var requestString = "";

			if (selObj.selectedIndex != 0) // Handle selection of empty drop down entry.
				requestString = getFile(selObj.options[selObj.selectedIndex].value);

			if (requestString == null) {
				requestString = "Sorry! There is a problem, please refresh the page.";
			}

			requestTextarea.value= requestString;

		} catch (err) {
			var txt = "";
			txt += "Error loading file: "
					+ selObj.options[selObj.selectedIndex].value;
			txt += "Error: " + err + "\n\n";
			requestTextarea.value += txt;
		}

	}

	function getFile(fileName) {
		oxmlhttp = null;
		try {
			oxmlhttp = new XMLHttpRequest();
			oxmlhttp.overrideMimeType("text/xml");
		} catch (e) {
			try {
				oxmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
			} catch (e) {
				return null;
			}
		}
		if (!oxmlhttp)
			return null;
		try {
			oxmlhttp.open("GET", fileName, false);
			oxmlhttp.send(null);
		} catch (e) {
			return null;
		}
		return oxmlhttp.responseText;
	}

	function initEditor() {
		editor = CodeMirror.fromTextArea(document.getElementById('requestTextarea'), {
			height : "380px",
			mode : "text/xml",
			lineNumbers : true,
			content : defaultString
		});
	}
</script>

</head>

<body bgcolor="#ffffff" text="#000000" onload="load()">

	<div id="content">
		<div id="header">
			<div id="headline">
				<span class="title">WPS TestClient</span>
				<p class="infotext">
					<br />For more information about the 52&deg; North Web
					Processing Service visit <a href="http://52north.org/wps">http://52north.org/wps</a>.
				</p>
			</div>
			<div id="logo">
				<a href="http://www.52north.org/" target="_blank"><img
					src="http://52north.org/templates/52n-2012/images/52n-logo.gif"
					border="0" alt="52North Logo" /></a>
			</div>
		</div>

		<form name="urlform" method="post" action="">
			<h3>
				Service URL:&nbsp;&nbsp; <span> <input name="url"
					id="serviceUrlField" value="./WebProcessingService" size="80"
					type="text" />
				</span>
			</h3>
		</form>

		<h3>
			Request Examples:&nbsp;&nbsp; <select id="selRequest"
				onchange="insertSelected();">
				<option value=" "></option>
			</select>
		</h3>

		<div class="request-form">
			<form name="requestform" method="post" action="">
				<div class="editorBorder">
					<textarea name="request" id="requestTextarea"></textarea>
				</div>
				<div class="request-form-buttons">
					<input value="Send"
						onclick="requestform.action = urlform.url.value" type="submit" />
					<input value="Clear" name="reset" type="reset"
						onclick="document.getElementById('selRequest').selectedIndex = 0; insertSelected();" />
				</div>
			</form>

			<p class="infotext">This TestClient was successfully tested in
				Firefox 3.5.2, Safari 4.0.3, Opera 9.64 and InternetExplorer
				8.0.6001.18702 and should work properly in Firefox 1.0 or higher,
				Safari 1.2 or higher, Opera 8 or higher and InternetExplorer 5 or
				higher.</p>
		</div>
	</div>

</body>
</html>
