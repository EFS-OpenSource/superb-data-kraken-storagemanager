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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * S3 client implementation using the MinIO Java SDK
 * <p>
 * This implementation works for the MinIO S3 Server as Amazon S3 compatible cloud storage.
 */
@Component
@Profile("s3")
public class S3ClientDefault implements S3Client {

    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;

    public S3ClientDefault(@Value("${storagemanager.storage.s3.endpoint}") String endpoint, @Value("${storagemanager.storage.s3.accessKey}") String accessKey,
            @Value("${storagemanager.storage.s3.secretKey}") String secretKey, @Value("${storagemanager.storage.s3.bucketName}") String bucketName) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBucketName() {
        return this.bucketName;
    }

    @Override
    public boolean prefixExists(String prefix) {
        // TODO: auto-generated code
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean objectExists(String objectKey) {
        // TODO: auto-generated code
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createEmptyObject(String objectKey) {
        // TODO: auto-generated code
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteObjectsByPrefix(String prefix) {
        // TODO: auto-generated code
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createIamPolicy(String policyName, String policy) {
        // TODO: auto-generated code
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listIamPolicies(String prefix) {
        // TODO: auto-generated code
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteIamPolicy(String policyName) {
        // TODO: auto-generated code
    }
}
