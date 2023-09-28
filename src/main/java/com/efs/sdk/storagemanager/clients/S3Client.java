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

import java.util.List;

/**
 * Interface for the S3 operations needed by the storage client
 */
public interface S3Client {

    /**
     * Get the name of the S3 bucket used as SDK storage
     *
     * @return name of the bucket
     */
    String getBucketName();

    /**
     * Check if prefix already exists
     * <p>
     * Returns true if any object key containing the given prefix (including the
     * prefix itself as object key) exists in the bucket.
     *
     * @param prefix the prefix
     * @return if the prefix exists
     */
    boolean prefixExists(String prefix);

    /**
     * Check if object already exists
     *
     * @param objectKey the object key
     * @return if object key already exists
     */
    boolean objectExists(String objectKey);

    /**
     * Create an empty (zero length) object
     * <p>
     * Can be used to create a virtual folder. Files with an object key ending on a
     * slash ('/') character are often used to represent folders inside a bucket.
     * Does nothing if the object key already exists.
     *
     * @param objectKey the object key
     */
    void createEmptyObject(String objectKey);

    /**
     * Delete all objects with given prefix (including the prefix as object key)
     *
     * @param prefix the prefix
     */
    void deleteObjectsByPrefix(String prefix);

    /**
     * Create IAM policy
     * <p>
     * Create an access policy for Identity and Access Management (IAM).
     * The policy documents should use the same schema as AWS IAM Policy documents
     * (i.e. Canned Policies in MinIO).
     *
     * @param policyName name of the policy to be created
     * @param policy     policy as JSON string
     */
    void createIamPolicy(String policyName, String policy);

    /**
     * Delete IAM policy
     *
     * @param policyName name of the policy to be deleted
     */
    void deleteIamPolicy(String policyName);

    /**
     * Lists IAM policies by prefix
     *
     * @param prefix the prefix
     * @return list of IAM policies
     */
    List<String> listIamPolicies(String prefix);
}
