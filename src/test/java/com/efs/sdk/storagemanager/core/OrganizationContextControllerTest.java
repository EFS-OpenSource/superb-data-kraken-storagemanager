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
import com.efs.sdk.storagemanager.helper.Utils;
import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationContextControllerTest {

    @Mock
    private AuthHelper authHelper;

    @Mock
    private StorageManagerService storageManagerService;

    @InjectMocks
    private OrganizationContextController organizationContextController;

    private JwtAuthenticationToken token;
    private OrganizationContextDTO dto;
    private String orgaName;
    private MockedStatic<Utils> mockedUtils;

    @BeforeEach
    void setUp() {
        token = new JwtAuthenticationToken(Jwt.withTokenValue("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIi" +
                        "OiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2Q" +
                        "T4fwpMeJf36POk6yJV_adQssw5c").header("alg", "none")
                .claim("q", "q").build());
        dto = new OrganizationContextDTO();
        orgaName = "testOrg";
        mockedUtils = mockStatic(Utils.class);
        mockedUtils.when(Utils::getSubjectAsToken).thenReturn(token);
    }

    @AfterEach
    public void teardown() {
        mockedUtils.close();
    }

    @Test
    @Disabled("Test is not required, as the check to verify is unecessary as it will always be invoked")
    void testCreateOrganizationResources_success() throws StorageManagerException {
        when(authHelper.isSuperuser(token)).thenReturn(true);

        ResponseEntity<Void> response = organizationContextController.createOrganizationContext(token, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authHelper, times(1)).isSuperuser(token);
        verify(storageManagerService, times(1)).createOrganizationContext(dto);
    }

    @Test
    @Disabled("Test is not required, as there is no check for the super user")
    void testCreateOrganizationResources_insufficientPrivilege() throws StorageManagerException {
        when(authHelper.isSuperuser(token)).thenReturn(false);

        StorageManagerException exception = new StorageManagerException(StorageManagerException.STORAGEMANAGER_ERROR.INSUFFICIENT_PRIVILEGE);
        try {
            organizationContextController.createOrganizationContext(token, dto);
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
            assertEquals(exception.getMessage(), e.getReason());
        }

        verify(authHelper, times(1)).isSuperuser(token);
        verify(storageManagerService, times(0)).createOrganizationContext(dto);
    }

    @Test
    void testDeleteOrganizationResources_success() throws StorageManagerException {
        when(authHelper.isSuperuser(token)).thenReturn(true);

        ResponseEntity<Void> response = organizationContextController.deleteOrganizationContext(token, orgaName);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authHelper, times(1)).isSuperuser(token);
        verify(storageManagerService, times(1)).deleteOrganizationContext(orgaName);
    }

    @Test
    void testDeleteOrganizationResources_insufficientPrivilege() throws StorageManagerException {
        when(authHelper.isSuperuser(token)).thenReturn(false);

        StorageManagerException exception = new StorageManagerException(StorageManagerException.STORAGEMANAGER_ERROR.INSUFFICIENT_PRIVILEGE);
        try {
            organizationContextController.deleteOrganizationContext(token, orgaName);
        } catch (StorageManagerException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
            assertEquals(exception.getMessage(), e.getMessage());
        }

        verify(authHelper, times(1)).isSuperuser(token);
        verify(storageManagerService, times(0)).deleteOrganizationContext(orgaName);
    }

}
