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

import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.storagemanager.commons.StorageManagerException;
import com.efs.sdk.storagemanager.helper.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.INSUFFICIENT_PRIVILEGE;

@RequestMapping(value = SpaceContextController.ENDPOINT)
@RestController
@Tag(name = SpaceContextController.ENDPOINT, description = "Operations for managing storage resources of a space.")
public class SpaceContextController {

    private static final Logger LOG = LoggerFactory.getLogger(SpaceContextController.class);
    static final String VERSION = "v2.0";
    static final String ENDPOINT = "/" + VERSION + "/context/organization/{orgaName}/space/";

    private final StorageManagerService storageManagerService;
    private final AuthHelper authHelper;

    public SpaceContextController(AuthHelper authHelper, StorageManagerService storageManagerService) {
        this.authHelper = authHelper;
        this.storageManagerService = storageManagerService;
    }

    @Operation(
            summary = "Create the Storage Context for a Space.",
            description = """
                    Creates a storage context for the specified space within the given organization. <br>
                    Only superusers are authorized to perform this action.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Successfully created the storage context for the space.")
    @ApiResponse(responseCode = "400", description = "Bad request. Possible validation error or incorrect data.")
    @ApiResponse(responseCode = "403", description = "Forbidden. User doesn't have the required permissions.")
    @ApiResponse(responseCode = "500", description = "Internal server error. An unexpected error occurred on the server.")
    @PostMapping
    public ResponseEntity<Void> createSpaceContext(
            @Parameter(hidden = true, description = "Authenticated user token.") JwtAuthenticationToken token,
            @PathVariable @Parameter(description = "Name of the organization under which the space resides.") String orgaName,
            @Valid @RequestBody
            @Parameter(
                    description = "Details about the storage space context.",
                    required = true,
                    schema = @Schema(implementation = SpaceContextDTO.class)
            )
            SpaceContextDTO payload
    ) throws StorageManagerException {
        if (!authHelper.isSuperuser(token)) {
            AuditLogger.error(LOG, "insufficient permissions to create space context on organization  {} " +
                    "and space {}", token, orgaName, payload.getName());
            throw new StorageManagerException(INSUFFICIENT_PRIVILEGE);
        }

        storageManagerService.createSpaceContext(payload);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "Delete the Storage Context of a Space.",
            description = """
                    Initiates the removal of a storage context for the specified space within a given organization. 
                    This action will delete all associated storage resources of the space. 
                    Only superusers are authorized to perform this action.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Successfully deleted the storage context for the space.")
    @ApiResponse(responseCode = "400", description = "Bad request. Possible validation error or incorrect data.")
    @ApiResponse(responseCode = "403", description = "Forbidden. User doesn't have the required permission or is not a superuser.")
    @DeleteMapping(path = "{spaceName}")
    public ResponseEntity<Void> deleteSpaceContext(
            @Parameter(hidden = true, description = "Authenticated user token.") JwtAuthenticationToken token,
            @PathVariable
            @Parameter(
                    description = "Name of the organization under which the space resides.",
                    required = true
            )
            String orgaName,
            @PathVariable
            @Parameter(
                    description = "Name of the space whose storage context is to be deleted.",
                    required = true
            )
            String spaceName
    ) throws StorageManagerException {
        if (!authHelper.isSuperuser(token)) {
            AuditLogger.error(LOG, "insufficient permissions to delete space context on organization  {} " +
                    "and space {}", token, orgaName, spaceName);
            throw new StorageManagerException(INSUFFICIENT_PRIVILEGE);
        }
        storageManagerService.deleteSpaceContext(orgaName, spaceName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
