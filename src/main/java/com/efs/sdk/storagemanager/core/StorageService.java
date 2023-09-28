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
package com.efs.sdk.storagemanager.core;

import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.storagemanager.clients.StorageClient;
import com.efs.sdk.storagemanager.commons.StorageManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StorageService {

    private static final Logger LOG = LoggerFactory.getLogger(StorageService.class);
    private final StorageClient storageClient;

    public StorageService(StorageClient storageClient) {
        this.storageClient = storageClient;
    }

    public void createOrganizationStorage(OrganizationContextDTO organization) throws StorageManagerException {
        LOG.info("Creating storage for organization '{}'", organization.getName());
        storageClient.createOrganizationStorage(organization);
        LOG.info("Creating storage for organization '{}' ... successful", organization.getName());
    }


    public void createSpaceStorage(SpaceContextDTO space) throws StorageManagerException {
        LOG.info("Creating storage for space '{}'", space.getName());
        storageClient.createSpaceStorage(space);
        LOG.info("Creating storage for space '{}' ... successful", space.getName());
    }

    public void deleteOrganizationStorage(OrganizationContextDTO organization) throws StorageManagerException {
        LOG.info("Deleting storage for organization '{}'", organization.getName());
        storageClient.deleteOrganizationStorage(organization);
        LOG.info("Deleting storage for organization '{}' ... successful", organization.getName());
    }

    public void deleteSpaceStorage(SpaceContextDTO space) throws StorageManagerException {
        LOG.info("Deleting storage for space '{}'", space.getName());
        this.storageClient.deleteSpaceStorage(space);
        LOG.info("Deleting storage for space '{}' ... successful", space.getName());
    }
}
