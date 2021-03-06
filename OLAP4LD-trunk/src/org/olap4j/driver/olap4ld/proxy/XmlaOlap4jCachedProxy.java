/*
// $Id: XmlaOlap4jCachedProxy.java 315 2010-05-29 00:56:11Z jhyde $
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// Copyright (C) 2007-2010 Julian Hyde
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package org.olap4j.driver.olap4ld.proxy;

import java.util.Map;

import org.olap4j.OlapException;
import org.olap4j.driver.olap4ld.cache.XmlaOlap4jCache;
import org.olap4j.driver.olap4ld.proxy.XmlaOlap4jProxy;

/**
 *
 * Extended Proxy interface which supports cached SOAP calls.
 *
 * @author Luc Boudreau
 * @version $Id: XmlaOlap4jCachedProxy.java 315 2010-05-29 00:56:11Z jhyde $
 *
 */
public interface XmlaOlap4jCachedProxy extends XmlaOlap4jProxy {

    /**
     * <p>Sets the cache class to use as a SOAP message cache.
     *
     * <p>Calling this method is not mandatory. If it isn't called,
     * no cache will be used and all SOAP requests will be sent to
     * the service end-point.
     *
     * @param configParameters This contains all the parameters used
     * to configure the Olap4j driver. It contains the full class name
     * of the cache implementation to use as well as the raw Cache
     * config parameters.
     * @param properties The properties to configure the cache,
     * so all config parameters which started
     * by Cache.* are inside this convenient thigny.
     * @see XmlaOlap4jCache
     */
    void setCache(
        Map<String, String> configParameters,
        Map<String, String> properties) throws OlapException;

}

// End XmlaOlap4jCachedProxy.java
