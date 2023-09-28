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
import com.efs.sdk.storagemanager.commons.StorageManagerException;
import com.efs.sdk.storagemanager.helper.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.INSUFFICIENT_PRIVILEGE;

@RequestMapping(value = OrganizationContextController.ENDPOINT)
@RestController
@Tag(name = OrganizationContextController.ENDPOINT, description = "Operations for managing storage resources of an organization.")
public class OrganizationContextController {

    static final String VERSION = "v2.0";
    static final String RESOURCE = "context/organization/";
    static final String ENDPOINT = "/" + VERSION + "/" + RESOURCE;

    private final StorageManagerService storageManagerService;
    private final AuthHelper authHelper;

    public OrganizationContextController(AuthHelper authHelper, StorageManagerService storageManagerService) {
        this.authHelper = authHelper;
        this.storageManagerService = storageManagerService;
    }

    @Operation(
            summary = "Create the Storage Context for an Organization",
            description = """
                    Create a new storage context for a given organization. 
                    This entails provisioning all associated resources required for the storage context and depends on the underlying cloud storage.
                    The special role """ + AuthHelper.ORG_CREATE_PERMISSION_ROLE + " is required to perform this operation."
    )
    @ApiResponse(responseCode = "200", description = "Successfully created the storage context for the organization.")
    @ApiResponse(responseCode = "400", description = "Bad request. Possible validation error or incorrect data.")
    @ApiResponse(responseCode = "403", description = "Forbidden. User doesn't have the required permission.")
    @ApiResponse(responseCode = "500", description = "Internal server error. An unexpected error occurred on the server.")
    @PostMapping
    @PreAuthorize("hasRole('" + AuthHelper.ORG_CREATE_PERMISSION_ROLE + "')")
    public ResponseEntity<Void> createOrganizationContext(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @Valid @RequestBody @Parameter(description = "Data Transfer Object containing information about the organization's storage context.") OrganizationContextDTO dto
    ) throws StorageManagerException {
        storageManagerService.createOrganizationContext(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "Delete the storage context of an organization",
            description = """
                    Removes the storage context for the specified organization along with all its associated resources.
                    Only superusers are authorized to perform this action.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Successfully deleted the storage context for the organization.")
    @ApiResponse(responseCode = "400", description = "Bad request. Possible validation error or incorrect data.")
    @ApiResponse(responseCode = "403", description = "Forbidden. User doesn't have the required permission to delete the storage context of the given" +
            " organization.")
    @ApiResponse(responseCode = "500", description = "Internal server error. An unexpected error occurred on the server.")
    @DeleteMapping(path = "{orgaName}")
    public ResponseEntity<Void> deleteOrganizationContext(
            @Parameter(hidden = true) JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "Name of the organization whose storage context needs to be deleted.") String orgaName
    ) throws StorageManagerException {
        if (!authHelper.isSuperuser(token)) {
            throw new StorageManagerException(INSUFFICIENT_PRIVILEGE);
        }
        storageManagerService.deleteOrganizationContext(orgaName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


}
