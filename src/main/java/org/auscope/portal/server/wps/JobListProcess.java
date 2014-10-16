/**
 * Copyright 2014 CSIRO
 */
package org.auscope.portal.server.wps;

import java.util.*;

import org.auscope.portal.server.vegl.*;
import org.n52.wps.algorithm.annotation.*;
import org.n52.wps.server.*;
import org.slf4j.*;

/**
 * @author fri096
 *
 */
@Algorithm(version = "1.0.0", title = "Job List Process", abstrakt = "Returns List of Jobs")
public class JobListProcess extends AbstractAnnotatedAlgorithm {


    private static final Logger log = LoggerFactory.getLogger(JobListProcess.class);

    //    private List<XmlObject> complexInput;
    private List<String> literalInput;

//    private XmlObject complexOutput;
    private String literalOutput;

    @Execute
    public void listJobs() {

        String email = literalInput.get(0);

        List<VEGLSeries> series = SpringContext.getJobManager().querySeries(email, null, null);

        for (VEGLSeries veglSeries : series) {
            literalOutput+= veglSeries.toString()+"\n";
        }

//        log.debug("Running echo process");
//
//        if (complexInput != null && complexInput.size() > 0)
//            complexOutput = complexInput.get(0);
//        else
//            log.debug("No complex inputs.");
//
//        if (literalInput != null && literalInput.size() > 0)
//            literalOutput = literalInput.get(0);
//        else
//            log.debug("No literal input");
//
        log.debug("Finished list series : {}", literalOutput);
    }

//    @ComplexDataOutput(identifier = "complexOutput", binding = GenericXMLDataBinding.class)
//    public XmlObject getComplexOutput() {
//        return complexOutput;
//    }

    @LiteralDataOutput(identifier = "literalOutput")
    public String getLiteralOutput() {
        return literalOutput;
    }

//    @ComplexDataInput(binding = GenericXMLDataBinding.class, identifier = "complexInput", minOccurs = 0, maxOccurs = 1)
//    public void setComplexInput(List<XmlObject> complexInput) {
//        this.complexInput = complexInput;
//    }

    @LiteralDataInput(identifier = "email", minOccurs = 0, maxOccurs = 1)
    public void setLiteralInput(List<String> literalInput) {
        this.literalInput = literalInput;
    }


}
