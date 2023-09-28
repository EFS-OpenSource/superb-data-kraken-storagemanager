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
import com.efs.sdk.common.domain.model.Confidentiality;
import com.efs.sdk.storagemanager.commons.StorageManagerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.MULTIPLE_POLICIES_FOUND;
import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.UNABLE_FIND_SPC_POLICY;
import static com.efs.sdk.storagemanager.helper.ResourceReader.readFileToString;
import static java.lang.String.format;
import static java.lang.String.join;


/**
 * S3 implementation of StorageClient
 * <p>
 * Organization and space storages are implemented using object name prefixes for all
 * objects stored inside one common main bucket (example prefix "orga1/space3/")
 */
@Component
@Profile("s3")
public class StorageClientS3 implements StorageClient {

    private static final Logger LOG = LoggerFactory.getLogger(StorageClientS3.class);
    private static final String ROLE_SPC_PUBLIC_ACCESS = "spc_all_public";
    private static final String PROP_SPACE_LOADINGZONE = "loadingzone";
    private final S3Client s3;
    private final String bucketName;
    private final ObjectMapper objectMapper;

    public StorageClientS3(S3Client s3, ObjectMapper objectMapper) {
        this.s3 = s3;
        this.bucketName = s3.getBucketName();
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createOrganizationStorage(OrganizationContextDTO organization) {
        // Warn if any object with prefix for the organization already exists
        String prefix = organization.getName() + "/";
        if (s3.prefixExists(prefix)) {
            LOG.warn("Organization '{}' already exists. At least one object with prefix {}' already exists in bucket '{}'.", organization.getName(), prefix,
                    bucketName);
        }

        // Create a virtual folder for the organization if it doesn't exist already
        if (!s3.objectExists(prefix)) {
            LOG.info("Creating empty object '{}' as virtual folder in bucket '{}'.", prefix, bucketName);
            s3.createEmptyObject(prefix);
            // create loadingzone directly with organization
            String spacePrefix = format("%s/%s/", organization.getName(), PROP_SPACE_LOADINGZONE);
            s3.createEmptyObject(spacePrefix);
        } else {
            LOG.info("Object '{}' as virtual folder in bucket '{}' already exists.", prefix, bucketName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSpaceStorage(SpaceContextDTO space) {
        if (PROP_SPACE_LOADINGZONE.equalsIgnoreCase(space.getName())) {
            return;
        }

        String orgaPrefix = format("%s/", space.getOrganization().getName());
        String spacePrefix = format("%s/%s/", space.getOrganization().getName(), space.getName());

        if (s3.prefixExists(orgaPrefix)) {
            // Warn if any object with prefix for the space already exists
            if (s3.prefixExists(spacePrefix)) {
                LOG.warn("Space '{}' already exists inside organization '{}'. At least one object with prefix '{}' already exists in bucket '{}'.",
                        space.getName(), space.getOrganization().getName(), spacePrefix, bucketName);
            }

            // Create virtual folder for the space if it doesn't exist already
            if (!s3.objectExists(spacePrefix)) {
                LOG.info("Creating empty object '{}' as virtual folder in bucket '{}'.", spacePrefix, bucketName);
                s3.createEmptyObject(spacePrefix);
            } else {
                LOG.info("Object '{}' as virtual folder in bucket '{}' already exists.", spacePrefix, bucketName);
            }

            // Create IAM policies (or update if already existing)
            try {
                if (Confidentiality.PUBLIC.equals(space.getConfidentiality())) {
                    LOG.info("Creating public-access-policy for space '{}'", space.getName());
                    addPublicPolicy(space);
                }
                LOG.info("Creating IAM policies for space '{}'.", space.getName());
                createPolicy(space, "iam_policy_space_admin_tpl.json", "admin");
                createPolicy(space, "iam_policy_space_trustee_tpl.json", "trustee");
                createPolicy(space, "iam_policy_space_user_tpl.json", "user");
                createPolicy(space, "iam_policy_space_supplier_tpl.json", "supplier");
            } catch (Exception e) {
                LOG.error("Error creating IAM policies for space '{}': {}", space.getName(), e.getMessage());
            }
        } else {
            LOG.error("Space '{}' was not created, because organization '{}' does not exist. No object with prefix '{}' exists in bucket '{}'.",
                    space.getName(), space.getOrganization().getName(), orgaPrefix, bucketName);
        }
    }

    private void createPolicy(SpaceContextDTO space, String tplName, String scopeName) throws StorageManagerException {
        String iamPolicyJson = readPolicy(space, tplName);
        s3.createIamPolicy(join("_", space.getOrganization().getName(), space.getName(), scopeName), iamPolicyJson);
    }

    /**
     * Create global spc_all_public-policy if non-existant. Add  each space-statement for each public space.
     *
     * @param space the public space
     * @throws StorageManagerException thrown on error
     */
    private void addPublicPolicy(SpaceContextDTO space) throws StorageManagerException {
        try {
            List<String> iamPolicies = s3.listIamPolicies(ROLE_SPC_PUBLIC_ACCESS);
            if (iamPolicies.isEmpty()) {
                String iamPolicyJson = readPolicy(space, "iam_policy_space_all_public_tpl.json");
                s3.createIamPolicy(ROLE_SPC_PUBLIC_ACCESS, iamPolicyJson);
            }
            String iamPolicy = getPolicy(ROLE_SPC_PUBLIC_ACCESS);
            Map<String, Object> policyMap = objectMapper.readValue(iamPolicy, new TypeReference<Map<String, Object>>() {
            });
            List<Object> statements = getStatements(policyMap);

            List<Object> spaceStatements = getSpaceStatements(space);
            statements.addAll(spaceStatements);

            String newPolicy = updateStatements(policyMap, statements);
            updatePolicy(ROLE_SPC_PUBLIC_ACCESS, newPolicy);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private String updateStatements(Map<String, Object> policyMap, List<Object> statements) throws JsonProcessingException {
        policyMap.put("Statement", statements);
        return objectMapper.writeValueAsString(policyMap);
    }

    private List<Object> getSpaceStatements(SpaceContextDTO space) throws StorageManagerException, JsonProcessingException {
        String spacePolicy = readPolicy(space, "iam_policy_space_public_tpl.json");
        return objectMapper.readValue(spacePolicy, new TypeReference<List<Object>>() {
        });
    }

    private String readPolicy(SpaceContextDTO space, String tplFileName) throws StorageManagerException {
        return readFileToString("/" + tplFileName).replace("${bucket}", bucketName).replace("${organization}", space.getOrganization().getName()).replace("$" +
                "{space}", space.getName());
    }

    private List<Object> getStatements(Map<String, Object> policyMap) throws JsonProcessingException {
        String statementStr = objectMapper.writeValueAsString(policyMap.get("Statement"));
        return objectMapper.readValue(statementStr, new TypeReference<List<Object>>() {
        });
    }

    /**
     * Remove space-specific statements from spc_all_public-policy.
     *
     * @param space the public space
     * @throws StorageManagerException thrown on error
     */
    private void removePublicPolicy(SpaceContextDTO space) throws StorageManagerException {
        try {
            String iamPolicy = getPolicy(ROLE_SPC_PUBLIC_ACCESS);
            Map<String, Object> policyMap = objectMapper.readValue(iamPolicy, new TypeReference<Map<String, Object>>() {
            });
            List<Object> statements = getStatements(policyMap);

            List<Object> spaceStatements = getSpaceStatements(space);
            statements.removeAll(spaceStatements);
            String newPolicy = updateStatements(policyMap, statements);
            updatePolicy(ROLE_SPC_PUBLIC_ACCESS, newPolicy);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void updatePolicy(String policyName, String policy) {
        s3.deleteIamPolicy(policyName);
        s3.createIamPolicy(policyName, policy);
    }

    private String getPolicy(String roleName) throws StorageManagerException {
        List<String> iamPolicies = s3.listIamPolicies(roleName);
        if (iamPolicies.isEmpty()) {
            throw new StorageManagerException(UNABLE_FIND_SPC_POLICY, roleName);
        }
        if (iamPolicies.size() > 1) {
            throw new StorageManagerException(MULTIPLE_POLICIES_FOUND, roleName);
        }
        return iamPolicies.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOrganizationStorage(OrganizationContextDTO organization) {
        final String prefix = format("%s/", organization.getName());
        LOG.info("Deleting all objects with prefix '{}' from bucket '{}'.", prefix, bucketName);
        s3.deleteObjectsByPrefix(prefix);
        List<String> spacePolicies = s3.listIamPolicies(prefix);
        for (var policy : spacePolicies) {
            s3.deleteIamPolicy(policy);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSpaceStorage(SpaceContextDTO space) {
        try {
            final String prefix = format("%s/%s/", space.getOrganization().getName(), space.getName());
            LOG.info("Deleting all objects with prefix '{}' from bucket '{}'.", prefix, bucketName);
            s3.deleteObjectsByPrefix(prefix);
            if (Confidentiality.PUBLIC.equals(space.getConfidentiality())) {
                removePublicPolicy(space);
            }
            if (!space.getName().equalsIgnoreCase(PROP_SPACE_LOADINGZONE)) {
                s3.deleteObjectsByPrefix(prefix);
                s3.deleteIamPolicy(join("_", space.getOrganization().getName(), space.getName(), "admin"));
                s3.deleteIamPolicy(join("_", space.getOrganization().getName(), space.getName(), "trustee"));
                s3.deleteIamPolicy(join("_", space.getOrganization().getName(), space.getName(), "user"));
            }
        } catch (Exception e) {
            LOG.error("Error creating IAM policies for space '{}': {}", space.getName(), e.getMessage());
        }
    }

}
