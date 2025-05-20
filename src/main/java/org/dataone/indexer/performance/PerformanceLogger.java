/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */
package org.dataone.indexer.performance;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.dataone.configuration.Settings;

public class PerformanceLogger {

    static final Log defaultLogger = LogFactory.getLog(PerformanceLogger.class);
    
    private static PerformanceLogger self = new PerformanceLogger(); // non-lazy singleton 
    private static Log perfLogger;
    private static boolean enabled;

    
    private PerformanceLogger() {
        enabled = Settings.getConfiguration().getBoolean("dataone.indexing.performance.logging.enabled", Boolean.FALSE);
        defaultLogger.warn("Setting up PerformanceLogger: set to enabled? " + enabled);
        
        perfLogger = LogFactory.getLog("performanceStats");
        
        if (perfLogger == null) {
            defaultLogger.error("Unable to create Log for performanceStats appender!");
            enabled = false;
        }
    }
    
    public static PerformanceLogger getInstance() {
        return self;
    }
    
    public void log(String id, long milliseconds) {
        if (enabled)
            log("" + id + ", " + milliseconds);
    }
    
    public void log(String message) {
        if (enabled)
            perfLogger.info(message);
    }
    
    public boolean isLogEnabled() {
        return enabled;
    }
}
