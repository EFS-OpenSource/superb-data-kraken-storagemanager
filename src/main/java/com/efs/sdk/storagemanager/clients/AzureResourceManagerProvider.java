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

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Prod-Implementation of {@link AzureResourceManagerProvider} without Logging
 */
@Component
@Profile("azure")
public class AzureResourceManagerProvider {

    @Value("${storagemanager.storage.azure.user.tenant}")
    private final String tenantId;
    @Value("${storagemanager.storage.azure.user.client-id}")
    private final String clientId;
    @Value("${storagemanager.storage.azure.user.client-secret}")
    private final String clientSecret;

    @Value("${storagemanager.storage.azure.user.subscription-id:}")
    private final String subscriptionId;

    private AzureResourceManager azureResourceManager;

    public AzureResourceManagerProvider(@Value("${storagemanager.storage.azure.user.tenant}") String tenantId, @Value("${storagemanager.storage.azure.user" +
            ".client-id}") String clientId, @Value("${storagemanager.storage.azure.user.client-secret}") String clientSecret, @Value("${storagemanager" +
            ".storage.azure.user.subscription-id:}") String subscriptionId) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.subscriptionId = subscriptionId;
    }

    /**
     * Setup <code>AzureResourceManager</code>
     *
     * @return AzureResourceManager
     */
    public AzureResourceManager azure() {
        if (azureResourceManager == null) {
            setupEnvironment();

            final AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            final TokenCredential credential = new DefaultAzureCredentialBuilder().authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint()).build();

            AzureResourceManager.Authenticated authenticate = AzureResourceManager.configure().withLogLevel(HttpLogDetailLevel.NONE).authenticate(credential,
                    profile);
            if (subscriptionId == null || subscriptionId.isBlank()) {
                azureResourceManager = authenticate.withDefaultSubscription();
            } else {
                azureResourceManager = authenticate.withSubscription(subscriptionId);
            }
        }
        return azureResourceManager;
    }

    /**
     * Setup environment-variables (workaround for distroless docker-image)
     */
    private void setupEnvironment() {
        updateEnvironment("AZURE_TENANT_ID", tenantId);
        updateEnvironment("AZURE_CLIENT_ID", clientId);
        updateEnvironment("AZURE_CLIENT_SECRET", clientSecret);
    }

    /**
     * Sets property if not set
     *
     * @param propertyName the name of the property
     * @param value        the value of the property
     */
    private void updateEnvironment(String propertyName, String value) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue == null || propertyValue.isBlank()) {
            System.setProperty(propertyName, value);
        }
    }

}
