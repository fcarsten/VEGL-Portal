/**
 * Copyright 2014 CSIRO
 */
package org.auscope.portal.server.wps;

import org.auscope.portal.server.vegl.*;
import org.springframework.beans.*;
import org.springframework.context.*;

/**
 * @author fri096
 *
 */
public class SpringContext implements ApplicationContextAware  {
    private static ApplicationContext context;

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
      context = ctx;
    }
    public static ApplicationContext getApplicationContext() {
      return context;
    }

    static public VEGLJobManager getJobManager() {
        return (VEGLJobManager) context.getBean("veglJobManager");
    }
}
