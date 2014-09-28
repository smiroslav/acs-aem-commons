/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.content.properties.bulk.impl;

public class Result {

    public enum Status {
        SUCCESS,
        ACCESS_ERROR,
        RELATIVE_PATH_NOT_FOUND,
        ERROR,
        NOOP
    }

    private final Status status;

    private final String path;

    public Result(Status status, String path) {
        this.status = status;
        this.path = path;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getPath() {
        return this.path;
    }

    public String toString() {
        return this.getStatus() + "," + this.getPath();
    }
}