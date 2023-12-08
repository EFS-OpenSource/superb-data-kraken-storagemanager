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
package com.efs.sdk.storagemanager.clients;

import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.storagemanager.commons.StorageManagerException;

/**
 * Provides a unified interface for abstracting different cloud storage providers.
 * This interface allows the SDK to interact with the underlying cloud storage, regardless
 * of the specific provider being used.
 * <p>
 * Each supported cloud storage provider has a corresponding implementation class with
 * the naming convention `StorageClient*` (e.g., `StorageClientAzure`, `StorageClientS3`). These implementation classes are Spring Boot components that are
 * dynamically injected based on the active profile configuration.
 * </p>
 */
public interface StorageClient {

    String LOADINGZONE = "loadingzone";

    /**
     * Create a storage for the provided organization.
     *
     * @param organization the organizatiobn
     * @throws StorageManagerException thrown on errors
     */
    void createOrganizationStorage(OrganizationContextDTO organization) throws StorageManagerException;

    void createLoadingzone(OrganizationContextDTO organization) throws StorageManagerException;

    /**
     * Create a new storage the provided space.
     *
     * @param space the space
     * @throws StorageManagerException thrown on errors
     */
    void createSpaceStorage(SpaceContextDTO space) throws StorageManagerException;

    /**
     * Delete the storage of the provided organization.
     *
     * @param organization the organization
     * @throws StorageManagerException thrown on errors
     */
    void deleteOrganizationStorage(OrganizationContextDTO organization) throws StorageManagerException;

    /**
     * Delete the storage for the provided space.
     *
     * @param space the space
     * @throws StorageManagerException thrown on errors
     */
    void deleteSpaceStorage(SpaceContextDTO space) throws StorageManagerException;

}
