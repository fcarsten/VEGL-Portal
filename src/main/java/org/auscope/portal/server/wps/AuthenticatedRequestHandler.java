/**
 * Copyright 2014 CSIRO
 */
package org.auscope.portal.server.wps;

import java.io.*;

import javax.servlet.*;
import javax.xml.parsers.*;

import org.n52.wps.server.*;
import org.n52.wps.server.WebProcessingService;
import org.n52.wps.server.handler.*;
import org.n52.wps.server.request.*;
import org.slf4j.*;
import org.springframework.security.core.userdetails.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * @author fri096
 *
 */
public class AuthenticatedRequestHandler extends RequestHandler {
    protected static Logger LOGGER = LoggerFactory.getLogger(AuthenticatedRequestHandler.class);

    /**
     * @param byteArrayInputStream
     * @param outputStream
     * @param activeUser
     * @throws ExceptionReport
     */
    public AuthenticatedRequestHandler(
            ByteArrayInputStream is,
            ServletOutputStream os, User activeUser) throws ExceptionReport {
        String nodeName, localName, nodeURI, version = null;
        Document doc;
        this.os = os;

        boolean isCapabilitiesNode = false;

        try {
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            fac.setNamespaceAware(true);

            // parse the InputStream to create a Document
            doc = fac.newDocumentBuilder().parse(is);

            // Get the first non-comment child.
            Node child = doc.getFirstChild();
            while(child.getNodeName().compareTo("#comment")==0) {
                child = child.getNextSibling();
            }
            nodeName = child.getNodeName();
            localName = child.getLocalName();
            nodeURI = child.getNamespaceURI();
            Node versionNode = child.getAttributes().getNamedItem("version");

            /*
             * check for service parameter. this has to be present for all requests
             */
            Node serviceNode = child.getAttributes().getNamedItem("service");

            if(serviceNode == null){
                throw new ExceptionReport("Parameter <service> not specified.", ExceptionReport.MISSING_PARAMETER_VALUE, "service");
            }else{
                if(!serviceNode.getNodeValue().equalsIgnoreCase("WPS")){
                    throw new ExceptionReport("Parameter <service> not specified.", ExceptionReport.INVALID_PARAMETER_VALUE, "service");
                }
            }

            isCapabilitiesNode = nodeName.toLowerCase().contains("capabilities");
            if(versionNode == null && !isCapabilitiesNode) {
                throw new ExceptionReport("Parameter <version> not specified.", ExceptionReport.MISSING_PARAMETER_VALUE, "version");
            }
            //TODO: I think this can be removed, as capabilities requests do not have a version parameter (BenjaminPross)
            if(!isCapabilitiesNode){
//              version = child.getFirstChild().getTextContent();//.getNextSibling().getFirstChild().getNextSibling().getFirstChild().getNodeValue();
                version = child.getAttributes().getNamedItem("version").getNodeValue();
            }
            /*
             * check language, if not supported, return ExceptionReport
             * Fix for https://bugzilla.52north.org/show_bug.cgi?id=905
             */
            Node languageNode = child.getAttributes().getNamedItem("language");
            if(languageNode != null){
                String language = languageNode.getNodeValue();
                Request.checkLanguageSupported(language);
            }
        } catch (SAXException e) {
            throw new ExceptionReport(
                    "There went something wrong with parsing the POST data: "
                            + e.getMessage(),
                    ExceptionReport.NO_APPLICABLE_CODE, e);
        } catch (IOException e) {
            throw new ExceptionReport(
                    "There went something wrong with the network connection.",
                    ExceptionReport.NO_APPLICABLE_CODE, e);
        } catch (ParserConfigurationException e) {
            throw new ExceptionReport(
                    "There is a internal parser configuration error",
                    ExceptionReport.NO_APPLICABLE_CODE, e);
        }
        //Fix for Bug 904 https://bugzilla.52north.org/show_bug.cgi?id=904
        if(!isCapabilitiesNode && version == null) {
            throw new ExceptionReport("Parameter <version> not specified." , ExceptionReport.MISSING_PARAMETER_VALUE, "version");
        }
        if(!isCapabilitiesNode && !version.equals(Request.SUPPORTED_VERSION)) {
            throw new ExceptionReport("Version not supported." , ExceptionReport.INVALID_PARAMETER_VALUE, "version");
        }
        // get the request type
        if (nodeURI.equals(WebProcessingService.WPS_NAMESPACE) && localName.equals("Execute")) {
            req = new AuthenticatedExecuteRequest(doc, activeUser);
            setResponseMimeType((ExecuteRequest)req);
        }else if (nodeURI.equals(WebProcessingService.WPS_NAMESPACE) && localName.equals("GetCapabilities")){
            req = new CapabilitiesRequest(doc);
            this.responseMimeType = "text/xml";
        } else if (nodeURI.equals(WebProcessingService.WPS_NAMESPACE) && localName.equals("DescribeProcess")) {
            req = new DescribeProcessRequest(doc);
            this.responseMimeType = "text/xml";

        }  else if(!localName.equals("Execute")){
            throw new ExceptionReport("The requested Operation not supported or not applicable to the specification: "
                    + nodeName, ExceptionReport.OPERATION_NOT_SUPPORTED, localName);
        }
        else if(nodeURI.equals(WebProcessingService.WPS_NAMESPACE)) {
            throw new ExceptionReport("specified namespace is not supported: "
                    + nodeURI, ExceptionReport.INVALID_PARAMETER_VALUE);
        }
    }


}
