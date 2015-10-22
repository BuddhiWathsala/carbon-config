/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.internal.kernel.tenant.store.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * JAXB mapping for Hierarchy Config
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class HierarchyConfig {

    @XmlElement(name = "Parent", required = true)
    private String parentID;

    @XmlElement(name = "DepthOfHierarchy")
    private int depthOfHierarchy = -1;

    @XmlElement(name = "Child")
    private List<String> childIDs = new ArrayList<>();

    public String getParentID() {
        return parentID;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    public int getDepthOfHierarchy() {
        return depthOfHierarchy;
    }

    public void setDepthOfHierarchy(int depthOfHierarchy) {
        this.depthOfHierarchy = depthOfHierarchy;
    }

    public List<String> getChildIDs() {
        return Collections.unmodifiableList(childIDs);
    }

    public void setChildIDs(List<String> childTenantID) {
        this.childIDs = Collections.unmodifiableList(childTenantID);
    }
}
