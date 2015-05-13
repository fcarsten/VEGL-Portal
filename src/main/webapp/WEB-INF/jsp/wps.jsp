<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<link rel="shortcut icon" href="favicon.ico" />

<title>52&deg;North WPS</title>

<link type="text/css" rel="stylesheet" href="css/wps.css">

</head>
<body>
	<div class="github-fork-ribbon-wrapper right">
		<div class="github-fork-ribbon">
			<a href="https://github.com/52North/WPS">Fork me on GitHub</a>
		</div>
	</div>

	<div id="main">

		<h1>52&deg;North WPS</h1>
		<p>This is the welcome site for the 52&deg;North Web Processing
			Service 1.0.0 implementation.</p>

		<h2>Usage</h2>

		<h3>Requests</h3>
		<ul>
			<li><a target="_blank"
				href="./wps/WebProcessingService?Request=GetCapabilities&amp;Service=WPS">GetCapabilities
					request using HTTP GET</a></li>
		</ul>

		<h3>Clients</h3>

		<ul>
			<li><strong><a href="./testWps.html">52&deg;North WPS
						form client</a></strong> can be used to submit XML-based requests this WPS
				instance manually.</li>
		</ul>

		<h2>Documentation</h2>
		<ul>
			<li>To learn more about the specification visit the <a
				href="http://opengeospatial.org/standards/wps">OGC website</a>.
			</li>
			<li>To learn more about this implementation visit the <a
				href="http://www.52north.org/wps">52&deg;North Geoprocessing
					Community website</a>.
			</li>
			<li>This is an <a href="https://github.com/52North/WPS">open
					source project on GitHub</a></li>
			<li>Find <a
				href="https://wiki.52north.org/bin/view/Geoprocessing/52nWebProcessingService">developer
					documentation</a> in the 52&deg;North Wiki
			</li>
		</ul>
<!--
		<h2>Administration</h2>
		<p>
			<a href="./webAdmin/index.jsp">52&deg;North WPS webAdmin console</a>
		</p>

 -->
 	</div>

	<div>
		<p class="infotext">${project.build.finalName} based on
		${git.branch}.${git.revision} built at ${timestamp}</p>
	</div>
</body>
</html>