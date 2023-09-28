/*
Copyright (C) 2023 e:fs TechHub GmbH (sdk@efs-techhub.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.efs.sdk.storagemanager.helper;

import com.efs.sdk.storagemanager.commons.StorageManagerException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.UNABLE_LOAD_INTERNAL_RESOURCE;

public final class ResourceReader {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceReader.class);

    private ResourceReader() {
    }

    /**
     * Gets a resource as String (from a file)
     *
     * @param fileName The name of the file
     * @return the content of the file as String
     * @throws StorageManagerException thrown on io-errors
     */
    public static String readFileToString(String fileName) throws StorageManagerException {
        try {
            return IOUtils.toString(Objects.requireNonNull(ResourceReader.class.getResourceAsStream(fileName)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new StorageManagerException(UNABLE_LOAD_INTERNAL_RESOURCE);
        }
    }
}