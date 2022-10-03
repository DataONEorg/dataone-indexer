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
package org.dataone.cn.indexer.parser.utility;

import java.util.ArrayList;
import java.util.Collection;

import org.dataone.cn.indexer.convert.MemberNodeServiceRegistrationType;
import org.dataone.cn.indexer.convert.MemberNodeServiceRegistrationTypeDocumentService;
import org.dataone.configuration.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

public class MemberNodeServiceRegistrationTypeCache {

    private static final int REFRESH_INTERVAL_MINUTES = Settings.getConfiguration().getInt(
            "dataone.mn.registration.serviceType.cacheRefreshMinutes", 120);
    private static long refreshIntervalMillis = REFRESH_INTERVAL_MINUTES * 60 * 1000;

    private static long lastRefreshTime = 0;
    private static Collection<MemberNodeServiceRegistrationType> serviceTypes = new ArrayList<MemberNodeServiceRegistrationType>();

    private MemberNodeServiceRegistrationTypeDocumentService mnServiceRegistrationTypeDocumentService;

    public Collection<MemberNodeServiceRegistrationType> getServiceTypes() {

        long expectedRefreshTime = lastRefreshTime + refreshIntervalMillis;
        long currentTime = System.currentTimeMillis();

        if (currentTime > expectedRefreshTime) {
            Document doc = mnServiceRegistrationTypeDocumentService
                    .getMemberNodeServiceRegistrationTypeDocument();
            serviceTypes = MemberNodeServiceRegistrationTypesParser.parseServiceTypes(doc);
            lastRefreshTime = currentTime;
        }

        return serviceTypes;
    }

    public MemberNodeServiceRegistrationTypeDocumentService getMnServiceRegistrationTypeDocumentService() {
        return mnServiceRegistrationTypeDocumentService;
    }

    public void setMnServiceRegistrationTypeDocumentService(
            MemberNodeServiceRegistrationTypeDocumentService mnServiceRegistrationTypeDocumentService) {
        this.mnServiceRegistrationTypeDocumentService = mnServiceRegistrationTypeDocumentService;
    }

}
