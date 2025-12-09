/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.ui.internal;

// import org.eclipse.smarthome.ui.internal.chart.defaultchartprovider.DefaultChartProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the default OSGi bundle activator
 *
 * @author Kai Kreuzer - Initial contribution
 */
public final class UIActivator implements BundleActivator {
    private final Logger logger = LoggerFactory.getLogger(UIActivator.class);

    private static BundleContext context;

    /**
     * Called whenever the OSGi framework starts our bundle
     */
    @Override
    public void start(BundleContext bc) throws Exception {
        context = bc;
        System.out.println("UIActivator.java start bc={}" + context );
        logger.info("UIActivator.java start UI(mapdb) bc={}", bc.getBundle().getHeaders() );
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     */
    @Override
    public void stop(BundleContext bc) throws Exception {
        logger.info("UIActivator.java start bc={}", context );
        System.out.println("UIActivator.java stop UI(mapdb) bc={}" + context );
        context = null;
    }

    public static BundleContext getContext() {
        return context;
    }

}
