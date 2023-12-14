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

import com.azure.core.management.exception.ManagementException;
import com.azure.core.util.Context;
import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.storage.fluent.models.BlobServicePropertiesInner;
import com.azure.resourcemanager.storage.fluent.models.ManagementPolicyInner;
import com.azure.resourcemanager.storage.models.*;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobCorsRule;
import com.azure.storage.blob.models.BlobRetentionPolicy;
import com.azure.storage.blob.models.BlobServiceProperties;
import com.efs.sdk.common.domain.dto.OrganizationContextDTO;
import com.efs.sdk.common.domain.dto.SpaceContextDTO;
import com.efs.sdk.storagemanager.commons.StorageManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.*;
import static java.lang.String.format;

/**
 * Azure implementation of StorageClient
 */
@Component
@Profile("azure")
public class StorageClientAzure implements StorageClient {

    private static final Logger LOG = LoggerFactory.getLogger(StorageClientAzure.class);
    private final AzureResourceManagerProvider azureProvider;
    @Value("${storagemanager.storage.azure.cors.origins}")
    private List<String> allowedOrigins;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.blob-versions-until-archive-tier-days:2}")
    private float blobVersionsUntilArchiveDays;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.blob-versions-until-cool-tier-days:1}")
    private float blobVersionsUntilCoolTierDays;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.blob-versions-until-delete-days:14}")
    private float blobVersionsUntilDeleteDays;
    @Value("${storagemanager.storage.azure.retry.delay:10000}")
    private long delay;
    @Value("${storagemanager.storage.azure.cors.maxAge}")
    private int maxAge;
    @Value("${storagemanager.storage.azure.retry.maxRetries:5}")
    private int maxRetries;
    @Value("${storagemanager.storage.azure.region}")
    private String region;
    @Value("${storagemanager.storage.azure.resourcegroup}")
    private String resourceGroup;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.retention-time-deleted-blobs:14}")
    private int retentionTimeDeletedBlobs;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.retention-time-deleted-containers:14}")
    private int retentionTimeDeletedContainers;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.soft-delete-blobs-enabled:true}")
    private boolean softDeleteBlobsEnabled;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.soft-delete-containers-enabled:true}")
    private boolean softDeleteContainersEnabled;
    @Value("${storagemanager.storage.azure.storage-account-default-settings.versioning-blobs-enabled:true}")
    private boolean versioningBlobsEnabled;

    StorageClientAzure(AzureResourceManagerProvider azureProvider) {
        this.azureProvider = azureProvider;
    }

    /**
     * Creates a storage account for a specified organization within Azure.
     * <p>
     * The storage account is created within Azure's Resource Group, and its name corresponds
     * directly to the organization's name.
     *
     * <ul>
     *     <li>If a storage account with the same name already exists, a warning is logged and
     *     a {@link StorageManagerException} is thrown.</li>
     *     <li>On a `403` error, indicating insufficient permissions for the Azure Service Principal,
     *     a {@link StorageManagerException} with detailed information is thrown.</li>
     *     <li>For other unexpected Azure management errors, the error message is parsed and
     *     appropriately handled, with a {@link StorageManagerException} thrown for clarity.</li>
     *     <li>Once the storage account is created successfully, essential properties are set,
     *     such as enabling soft delete for blobs and containers, as well as activating blob versioning.</li>
     * </ul>
     *
     * @param organization Represents the organization context, with the organization's name being
     *                     used as the Azure Storage Account name.
     * @throws StorageManagerException Thrown if there's a naming conflict, insufficient permissions,
     *                                 or any unexpected error.
     * @see #getStorageAccount(String)
     * @see #getConnectionString(StorageAccount)
     * @see #setCors(String)
     * @see #setStorageAccountProperties(StorageAccount)
     */
    @Override
    public void createOrganizationStorage(OrganizationContextDTO organization) throws StorageManagerException {
        LOG.debug("Creating storage account for organization {}", organization.getName());
        Optional<StorageAccount> storageAccount = getStorageAccount(organization.getName());
        if (storageAccount.isPresent()) {
            LOG.warn("Storage Account '{}' already exists on storage - nothing to do!", organization.getName());
            throw new StorageManagerException(STORAGE_ACCOUNT_ALREADY_EXISTS);
        }
        AzureResourceManager azure = azureProvider.azure();
        StorageAccount newStorageAccount;
        try {
            newStorageAccount = azure.storageAccounts()
                    .define(organization.getName())
                    .withRegion(region)
                    .withExistingResourceGroup(resourceGroup)
                    .withSku(StorageAccountSkuType.STANDARD_LRS)
                    .create();

            setStorageAccountProperties(newStorageAccount);
        } catch (ManagementException e) {
            String message = e.getMessage();

            // Extract status code - this should be the first 3 digits in the error message
            int statusCode = Integer.parseInt(message.replaceAll("\\D+", "").substring(0, 3));
            LOG.error("ERROR MSG AZURE: {}", message);

            // Extract error code and message using regular expressions
            Pattern pattern = Pattern.compile("\\{\"code\":\"([^\"]+)\",\"message\":\"([^\"]+)\"\\}");
            Matcher matcher = pattern.matcher(message);
            String errorCode = "";
            String errorMessage = "";

            if (matcher.find()) {
                errorCode = matcher.group(1);
                errorMessage = matcher.group(2);
            }

            if (statusCode == 403) {
                throw new StorageManagerException(BAD_PERMISSION_AZURE_SERVICE_PRINCIPAL, errorCode + " - " + errorMessage);
            } else if (statusCode == 409) {
                throw new StorageManagerException(STORAGE_ACCOUNT_NAME_TAKEN, errorCode + " - " + errorMessage);
            } else {
                throw new StorageManagerException(UNKNOWN_ERROR);
            }

        }
        String connectionString = getConnectionString(newStorageAccount);
        setCors(connectionString);
        LOG.debug("Creating storage account for organization {} ... successful", organization.getName());
    }

    @Override
    public void createLoadingzone(OrganizationContextDTO organization) throws StorageManagerException {
        LOG.debug("Creating loadingzone for organization {}", organization.getName());
        createBlobContainer(LOADINGZONE, organization.getName());
    }

    /**
     * Sets the required storage account properties, such as enabling soft delete for blobs and containers,
     * and activating versioning for blobs.
     *
     * @param storageAccount The storage account to set the properties for.
     */
    private void setStorageAccountProperties(StorageAccount storageAccount) {
        if (softDeleteBlobsEnabled) {
            enableSoftDeleteBlobs(storageAccount);
        }

        if (softDeleteContainersEnabled) {
            enableSoftDeleteContainers(storageAccount);
        }

        if (versioningBlobsEnabled) {
            enableBlobVersioning(storageAccount);
        }
    }

    /**
     * Enables blob versioning for a specified storage account.
     * <p>
     * Additionally, a lifecycle management rule is created to delete blob versions based on specified criteria.
     *
     * @param storageAccount The storage account to enable blob versioning for.
     */
    private void enableBlobVersioning(StorageAccount storageAccount) {
        BlobServicePropertiesInner blobServicePropertiesInner = new BlobServicePropertiesInner();
        // Set versioning for blobs
        blobServicePropertiesInner.withIsVersioningEnabled(versioningBlobsEnabled);

        azureProvider.azure()
                .storageAccounts()
                .manager()
                .serviceClient()
                .getBlobServices().setServicePropertiesWithResponse(
                        resourceGroup,
                        storageAccount.name(),
                        blobServicePropertiesInner,
                        Context.NONE
                );

        // Create a lifecycle management rule to delete blob versions
        // The rule operates on the storage account level and is applied to all blockBlobs in all underlying storage containers
        azureProvider.azure()
                .storageAccounts()
                .manager()
                .serviceClient()
                .getManagementPolicies()
                .createOrUpdateWithResponse(
                        resourceGroup,
                        storageAccount.name(),
                        ManagementPolicyName.DEFAULT,
                        new ManagementPolicyInner()
                                .withPolicy(
                                        new ManagementPolicySchema()
                                                .withRules(
                                                        List.of(
                                                                new ManagementPolicyRule()
                                                                        .withEnabled(true)
                                                                        .withName("migrate-blob-versions-until-cool-archive-delete")
                                                                        .withType(RuleType.LIFECYCLE)
                                                                        .withDefinition(
                                                                                new ManagementPolicyDefinition()
                                                                                        .withActions(
                                                                                                new ManagementPolicyAction()
                                                                                                        .withVersion(
                                                                                                                new ManagementPolicyVersion()
                                                                                                                        .withTierToCool(
                                                                                                                                new DateAfterCreation()
                                                                                                                                        .withDaysAfterCreationGreaterThan(blobVersionsUntilCoolTierDays))
                                                                                                                        .withTierToArchive(
                                                                                                                                new DateAfterCreation()
                                                                                                                                        .withDaysAfterCreationGreaterThan(blobVersionsUntilArchiveDays))
                                                                                                                        .withDelete(
                                                                                                                                new DateAfterCreation()
                                                                                                                                        .withDaysAfterCreationGreaterThan(blobVersionsUntilDeleteDays))))
                                                                                        .withFilters(
                                                                                                new ManagementPolicyFilter()
                                                                                                        .withBlobTypes(List.of("blockBlob"))))))),
                        com.azure.core.util.Context.NONE);


    }

    /**
     * Enables soft delete for containers within a specified storage account.
     * <p>
     * This allows the containers to be recoverable for a specified retention period.
     *
     * @param storageAccount The storage account to enable soft delete for containers.
     */
    private void enableSoftDeleteContainers(StorageAccount storageAccount) {
        BlobServicePropertiesInner blobServicePropertiesInner = new BlobServicePropertiesInner();
        blobServicePropertiesInner.withContainerDeleteRetentionPolicy(new DeleteRetentionPolicy().withEnabled(softDeleteContainersEnabled).withDays(retentionTimeDeletedContainers));
        azureProvider.azure()
                .storageAccounts()
                .manager()
                .serviceClient()
                .getBlobServices().setServicePropertiesWithResponse(
                        resourceGroup,
                        storageAccount.name(),
                        blobServicePropertiesInner,
                        Context.NONE
                );
    }

    /**
     * Enables soft delete for blobs within a specified storage account.
     * <p>
     * This allows the blobs to be recoverable for a specified retention period.
     *
     * @param storageAccount The storage account to enable soft delete for blobs.
     */
    private void enableSoftDeleteBlobs(StorageAccount storageAccount) {
        // First, get the storage account keys to authenticate
        List<StorageAccountKey> keys = storageAccount.getKeys();

        // Using the first key
        String storageConnectionString =
                "DefaultEndpointsProtocol=https;AccountName=" + storageAccount.name() + ";AccountKey=" + keys.get(0).value() + ";EndpointSuffix=core" +
                        ".windows.net";

        // Create a blob service client using the connection string
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();

        // Set soft delete for blobs
        blobServiceClient.setProperties(blobServiceClient
                .getProperties()
                .setDeleteRetentionPolicy(new BlobRetentionPolicy()
                        .setEnabled(softDeleteBlobsEnabled)
                        .setDays(retentionTimeDeletedBlobs)
                )
        );
    }

    /**
     * {@inheritDoc}
     * <br>
     * A space storage corresponds to a StorageContainer with the name of the space within the StorageAccount of the organization.
     */
    @Override
    public void createSpaceStorage(SpaceContextDTO space) throws StorageManagerException {
        LOG.debug("Creating storage container for space {}", space.getName());
        createBlobContainer(space.getName(), space.getOrganization().getName());
    }

    /**
     * {@inheritDoc}
     * <br>
     * Deletes the Storage Account associated with the specified organization name within the configured ResourceGroup.
     * <p>
     * If the storage account does not exist, the method completes without taking any action.
     *
     * @param organization An instance of {@link OrganizationContextDTO} that represents the organization context.
     * @see #getStorageAccount(String)
     */
    @Override
    public void deleteOrganizationStorage(OrganizationContextDTO organization) {
        LOG.debug("deleting storage for organization {}", organization.getName());
        Optional<StorageAccount> opt = getStorageAccount(organization.getName());
        if (opt.isPresent()) {
            StorageAccount storageAccount = opt.get();
            AzureResourceManager azure = azureProvider.azure();
            azure.storageAccounts().deleteById(storageAccount.id());
        }
        LOG.debug("deleting storage for organization {} ...  successful", organization.getName());
    }

    /**
     * {@inheritDoc}
     * <br>
     * Deletes the storage container of the provided space.
     * <p>
     * If the container does not exist, the method completes without taking any action.
     *
     * @param space An instance of {@link SpaceContextDTO} that represents the space context.
     * @throws StorageManagerException If any issues arise while attempting to delete the storage container.
     * @see #getStorageContainer(SpaceContextDTO)
     */
    @Override
    public void deleteSpaceStorage(SpaceContextDTO space) throws StorageManagerException {
        LOG.debug("deleting storage container for space {}", space.getName());
        Optional<BlobContainerClient> storageContainer = getStorageContainer(space);
        storageContainer.ifPresent(c -> {
            if (c.exists()) {
                c.delete();
            }
        });
        LOG.debug("deleting storage container for space {} ... successful", space.getName());
    }

    private Optional<StorageAccount> getStorageAccount(String name) {
        AzureResourceManager azure = azureProvider.azure();
        return azure.storageAccounts().listByResourceGroup(resourceGroup).stream().filter(a -> a.name().equals(name)).findFirst();
    }

    private Optional<BlobContainerClient> getStorageContainer(SpaceContextDTO space) throws StorageManagerException {
        AzureResourceManager azure = azureProvider.azure();
        Optional<StorageAccount> account =
                azure.storageAccounts().listByResourceGroup(resourceGroup).stream().filter(a -> a.name().equals(space.getOrganization().getName())).findFirst();
        if (account.isPresent()) {
            StorageAccount storageAccount = account.get();
            BlobServiceClient blobServiceClient = getBlobServiceClient(storageAccount);

            return Optional.of(blobServiceClient.getBlobContainerClient(space.getName()));
        }
        return Optional.empty();
    }

    private BlobServiceClient getBlobServiceClient(StorageAccount storageAccount) throws StorageManagerException {
        String connectStr = getConnectionString(storageAccount);
        // Create a BlobServiceClient object which will be used to create a container client
        return new BlobServiceClientBuilder().connectionString(connectStr).buildClient();
    }

    /**
     * Create a blob container for a given storage account
     *
     * @param blobContainerName Blob Container name (space name)
     * @param organizationName  Organization name
     * @throws StorageManagerException
     */
    private void createBlobContainer(String blobContainerName, String organizationName)
            throws StorageManagerException {
        LOG.debug("Creating blob container {} for organization {}", blobContainerName, organizationName);
        BlobContainerClient blobContainer = null;
        String errorMessage = "";
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                Optional<StorageAccount> storageAccount = getStorageAccount(organizationName);
                if (storageAccount.isPresent()) {
                    if (ProvisioningState.SUCCEEDED.equals(storageAccount.get().provisioningState())) {
                        BlobServiceClient blobServiceClient = getBlobServiceClient(storageAccount.get());

                        if (!blobServiceClient.getBlobContainerClient(blobContainerName).exists()) {
                            blobContainer = blobServiceClient.createBlobContainer(blobContainerName);
                        } else {
                            break;
                        }
                    }
                }
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                LOG.warn(e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
                errorMessage = e.getMessage();
            }
        }
        if (blobContainer == null || !blobContainer.exists()) {
            throw new StorageManagerException(format("was not able to create container for storage account '%s' in organization '%s', reason '%s'",
                    blobContainerName, organizationName, errorMessage));
        }
        LOG.debug("Creating storage container {} for {} ... successful", blobContainer, organizationName);
    }

    /**
     * Gets the primary connection string of the given StorageAccount
     *
     * @param storageAccount the StorageAccount
     * @return the primary connection string
     * @throws StorageManagerException thrown on errors
     */
    private String getConnectionString(StorageAccount storageAccount) throws StorageManagerException {
        List<StorageAccountKey> storageAccountKeys = storageAccount.getKeys();
        Optional<StorageAccountKey> first = storageAccountKeys.stream().findFirst();
        if (first.isPresent()) {
            StorageAccountKey key = first.get();
            return format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net", storageAccount.name(), key.value());
        }
        throw new StorageManagerException("no connection-key found!");
    }

    private void setCors(String connectionString) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return;
        }

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
        BlobServiceProperties serviceProperties = blobServiceClient.getProperties();
        List<BlobCorsRule> corsRules = allowedOrigins.stream().map(this::buildCorsRule).toList();

        serviceProperties.setCors(corsRules);
        blobServiceClient.setProperties(serviceProperties);
    }

    private BlobCorsRule buildCorsRule(String origin) {
        BlobCorsRule rule = new BlobCorsRule();
        rule.setAllowedOrigins(origin);
        rule.setMaxAgeInSeconds(maxAge);

        String[] corsRules = CorsRuleAllowedMethodsItem.values().stream().map(ExpandableStringEnum::toString).toList().toArray(String[]::new);
        String allowed = String.join(",", corsRules);

        rule.setAllowedMethods(allowed);
        rule.setAllowedHeaders("*");
        rule.setExposedHeaders("*");

        return rule;
    }

}
