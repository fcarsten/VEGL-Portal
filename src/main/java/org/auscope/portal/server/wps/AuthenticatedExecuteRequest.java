/**
 * Copyright 2014 CSIRO
 */
package org.auscope.portal.server.wps;

import org.n52.wps.server.*;
import org.n52.wps.server.request.*;
import org.n52.wps.server.response.*;
import org.springframework.security.core.userdetails.*;
import org.w3c.dom.*;

/**
 * @author fri096
 *
 */
public class AuthenticatedExecuteRequest extends ExecuteRequest {
    public static final ThreadLocal<User> currentUser = new ThreadLocal<User>();

    private User user;

    /* (non-Javadoc)
     * @see org.n52.wps.server.request.ExecuteRequest#call()
     */
    @Override
    public Response call() throws ExceptionReport {
        currentUser.set(user);
        return super.call();
    }

    /**
     * @param doc
     * @param activeUser
     * @throws ExceptionReport
     */
    public AuthenticatedExecuteRequest(Document doc, User activeUser) throws ExceptionReport {
        super(doc);
        this.user=activeUser;
    }

}
