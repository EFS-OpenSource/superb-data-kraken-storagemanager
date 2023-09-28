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
package com.efs.sdk.storagemanager.commons;

import org.springframework.http.HttpStatus;

public class StorageManagerException extends Exception {

    private final int errorCode;
    private final HttpStatus httpStatus;

    public StorageManagerException(STORAGEMANAGER_ERROR error) {
        super(error.code + ": " + error.msg);
        httpStatus = error.status;
        errorCode = error.code;
    }

    public StorageManagerException(STORAGEMANAGER_ERROR error, String additionalMessage) {
        super(error.code + ": " + error.msg + " " + additionalMessage);
        httpStatus = error.status;
        errorCode = error.code;
    }

    public StorageManagerException(String message) {
        this(STORAGEMANAGER_ERROR.UNKNOWN_ERROR, message);
    }

    /**
     * Provides the errors to the application.
     *
     * @author e:fs TechHub GmbH
     */
    public enum STORAGEMANAGER_ERROR {
        // unable creating resources

        UNABLE_CREATE_STORAGE_ACCOUNT(20002, HttpStatus.BAD_REQUEST, "unable to create storage account"),
        UNABLE_DELETE_STORAGE_ACCOUNT(20012, HttpStatus.BAD_REQUEST, "unable to delete storage account"),
        UNABLE_GET_TOKEN(20022, HttpStatus.BAD_REQUEST, "unable to retrieve token for user"),
        UNABLE_GET_ORGANIZATION(20023, HttpStatus.BAD_REQUEST, "unable to retrieve organization"),
        UNABLE_GET_SPACE(20024, HttpStatus.BAD_REQUEST, "unable to retrieve space"),
        UNABLE_GET_ORGANIZATIONS(20025, HttpStatus.BAD_REQUEST, "unable to retrieve organizations"),
        UNABLE_GET_SPACES(20026, HttpStatus.BAD_REQUEST, "unable to retrieve spaces"),
        INSUFFICIENT_PRIVILEGE(20031, HttpStatus.FORBIDDEN, "insufficient privilege"),
        UNABLE_LOAD_INTERNAL_RESOURCE(20041, HttpStatus.INTERNAL_SERVER_ERROR, "unable to load internal resource"),
        UNABLE_FIND_SPC_POLICY(20051, HttpStatus.CONFLICT, "unable to find policy"),
        MULTIPLE_POLICIES_FOUND(20052, HttpStatus.CONFLICT, "multiple policies found"),
        STORAGE_ACCOUNT_ALREADY_EXISTS(40000, HttpStatus.CONFLICT, "storage account already exists."),
        STORAGE_ACCOUNT_NAME_TAKEN(40001, HttpStatus.CONFLICT, ""),
        BAD_PERMISSION_AZURE_SERVICE_PRINCIPAL(50000, HttpStatus.BAD_GATEWAY, ""),
        UNKNOWN_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "unkown error.");


        private final int code;
        private final HttpStatus status;
        private final String msg;

        STORAGEMANAGER_ERROR(int code, HttpStatus status, String msg) {
            this.code = code;
            this.status = status;
            this.msg = msg;
        }

    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
