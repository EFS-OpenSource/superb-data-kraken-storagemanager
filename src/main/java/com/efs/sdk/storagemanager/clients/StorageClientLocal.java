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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local implementation of StorageClient
 */
@Component
@Profile("local")
public class StorageClientLocal implements StorageClient {

    private static final Logger LOG = LoggerFactory.getLogger(StorageClientLocal.class);

    /**
     * {@inheritDoc}
     * <br>
     * An organization storage corresponds to a folder (in the temp directory of the local
     * file system).
     */
    @Override
    public void createOrganizationStorage(OrganizationContextDTO organization) throws StorageManagerException {
        try {
            Path newDir = getOrganizationPath(organization.getName());
            if (Files.exists(newDir)) {
                LOG.warn("directory {} already exists - nothing to do", newDir);
            } else {
                Files.createDirectory(newDir);
            }
        } catch (IOException e) {
            LOG.error("create organization storage failed.");
            throw new StorageManagerException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * <br>
     * A space storage corresponds to a subfolder inside the organization folder.
     */
    @Override
    public void createSpaceStorage(SpaceContextDTO space) throws StorageManagerException {
        Path newDirOrg = getOrganizationPath(space.getOrganization().getName());
        if (!Files.exists(newDirOrg)) {
            try {
                Files.createDirectory(newDirOrg);
            } catch (IOException e) {
                LOG.error("failed to create directory.");
                throw new StorageManagerException(e.getMessage());
            }
        }
        try {
            Path newDir = getSpacePath(space);
            if (Files.exists(newDir)) {
                LOG.warn("directory '{}' already exists - nothing to do", newDir);
                return;
            }
            Files.createDirectory(newDir);
        } catch (IOException e) {
            LOG.error("failed to create directory.");
            throw new StorageManagerException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOrganizationStorage(OrganizationContextDTO organization) throws StorageManagerException {
        try {
            FileSystemUtils.deleteRecursively(getOrganizationPath(organization.getName()));
        } catch (IOException e) {
            LOG.error("failed to delete organization storage.");
            throw new StorageManagerException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSpaceStorage(SpaceContextDTO space) throws StorageManagerException {
        try {
            FileSystemUtils.deleteRecursively(getSpacePath(space));
        } catch (IOException e) {
            LOG.error("failed to delete space storage.");
            throw new StorageManagerException(e.getMessage());
        }
    }

    private Path getOrganizationPath(String organizationName) {
        return getPath(organizationName);
    }

    private Path getSpacePath(SpaceContextDTO space) {
        return getPath(space.getOrganization().getName()).resolve(space.getName());
    }

    private Path getPath(String dirName) {
        String dirPath = System.getProperty("java.io.tmpdir");
        Path tmpDir = Paths.get(dirPath);
        return tmpDir.resolve(dirName);
    }

}
