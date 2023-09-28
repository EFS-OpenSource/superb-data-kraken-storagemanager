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
import com.efs.sdk.storagemanager.commons.StorageManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StorageManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(StorageManagerService.class);
    private final StorageService storageService;


    public StorageManagerService(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Creates organization storage context consisting of:
     * - storage dedicated to given organization
     *
     * @param org The Organization
     * @throws StorageManagerException thrown on StorageManagerException Errors
     */
    public void createOrganizationContext(OrganizationContextDTO org) throws StorageManagerException {
        LOG.debug("creating organization storage context for '{}'", org.getName());
        storageService.createOrganizationStorage(org);
        LOG.debug("creating organization storage context for '{}' ... successful", org.getName());
    }

    /**
     * Deletes organization storage context of the given organization
     *
     * @param orgaName The name of the organization
     * @throws StorageManagerException thrown on StorageManagerException Errors
     */
    public void deleteOrganizationContext(String orgaName) throws StorageManagerException {
        LOG.debug("deleting organization storage context for '{}'", orgaName);
        // only name needed for delete operation
        OrganizationContextDTO org = OrganizationContextDTO.builder().name(orgaName).build();
        storageService.deleteOrganizationStorage(org);
        LOG.debug("deleting organization storage context for '{}' ... successful", orgaName);
    }


    /**
     * Creates space storage context consisting of:
     * - storage dedicated to given space
     *
     * @param space The Space
     * @throws StorageManagerException thrown on StorageManagerException Errors
     */
    public void createSpaceContext(SpaceContextDTO space) throws StorageManagerException {
        LOG.debug("creating space storage context for '{}'", space.getName());
        storageService.createSpaceStorage(space);
        LOG.debug("creating space storage context for '{}' ... successful", space.getName());
    }


    /**
     * Deletes space storage context of the given space
     *
     * @param orgaName  The name of the organization
     * @param spaceName The name of the organization
     * @throws StorageManagerException thrown on StorageManagerException Errors
     */
    public void deleteSpaceContext(String orgaName, String spaceName) throws StorageManagerException {
        LOG.debug("deleting space storage context for '{}'", spaceName);
        OrganizationContextDTO org = OrganizationContextDTO.builder().name(orgaName).build();
        SpaceContextDTO spaceContext = SpaceContextDTO.builder().name(spaceName).organization(org).build();
        this.storageService.deleteSpaceStorage(spaceContext);
        LOG.debug("deleting space storage context for '{}' ... successful", spaceName);
    }
}
