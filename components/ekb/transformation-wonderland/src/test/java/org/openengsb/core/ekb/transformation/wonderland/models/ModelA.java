/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.core.ekb.transformation.wonderland.models;

public class ModelA {
    private String idA;
    private String testA;
    private String blubA;
    private String blaA;
    private NestedObject nested;

    public String getIdA() {
        return idA;
    }

    public void setIdA(String idA) {
        this.idA = idA;
    }

    public String getTestA() {
        return testA;
    }

    public void setTestA(String testA) {
        this.testA = testA;
    }

    public String getBlubA() {
        return blubA;
    }

    public void setBlubA(String blubA) {
        this.blubA = blubA;
    }

    public String getBlaA() {
        return blaA;
    }

    public void setBlaA(String blaA) {
        this.blaA = blaA;
    }

    public NestedObject getNested() {
        return nested;
    }

    public void setNested(NestedObject nested) {
        this.nested = nested;
    }
}
