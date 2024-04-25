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
package org.dataone.cn.indexer.convert;

import java.util.Collection;

import org.dataone.cn.indexer.parser.utility.MemberNodeServiceRegistrationTypeCache;
import org.springframework.beans.factory.annotation.Autowired;

public class MemberNodeServiceRegistrationTypeConverter implements IConverter {

    private MemberNodeServiceRegistrationTypeCache mnServiceRegistrationTypeCacheService;

    @Override
    public String convert(String data) {

        Collection<MemberNodeServiceRegistrationType> serviceTypes = mnServiceRegistrationTypeCacheService
                .getServiceTypes();

        for (MemberNodeServiceRegistrationType serviceType : serviceTypes)
            for (String matchPattern : serviceType.getMatchingPatterns())
                if (data.matches(matchPattern))
                    return serviceType.getName();

        return data;
    }
    
    public MemberNodeServiceRegistrationTypeCache getMnServiceRegistrationTypeCacheService() {
        return mnServiceRegistrationTypeCacheService;
    }

    public void setMnServiceRegistrationTypeCacheService(
            MemberNodeServiceRegistrationTypeCache mnServiceRegistrationTypeCacheService) {
        this.mnServiceRegistrationTypeCacheService = mnServiceRegistrationTypeCacheService;
    }

}
