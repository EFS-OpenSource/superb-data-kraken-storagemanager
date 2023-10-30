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
package com.efs.sdk.storagemanager;

import com.efs.sdk.logging.AuditLogger;
import com.efs.sdk.storagemanager.commons.StorageManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.FORBIDDEN;
import static com.efs.sdk.storagemanager.commons.StorageManagerException.STORAGEMANAGER_ERROR.UNKNOWN_ERROR;
import static com.efs.sdk.storagemanager.helper.Utils.getSubjectAsToken;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    protected ResponseEntity<Object> handleException(RuntimeException e, WebRequest request) {
        LOG.error(e.getMessage(), e);
        return handleStorageManagerException(new StorageManagerException(UNKNOWN_ERROR), request);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(RuntimeException e, WebRequest request) {
        LOG.error(e.getMessage(), e);
        Iterable<String> iter = request::getHeaderNames;
        Optional<String> authHeader = StreamSupport.stream(iter.spliterator(), false)
                .filter(p -> p.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)).findFirst();
        if (authHeader.isPresent()) {
            String requestURI = ((ServletWebRequest) request).getRequest().getRequestURI();
            AuditLogger.error(LOG, "triggered access denied on context {}", getSubjectAsToken(),
                    requestURI);
        }
        return handleStorageManagerException(new StorageManagerException(FORBIDDEN), request);
    }

    @ExceptionHandler(value = StorageManagerException.class)
    private ResponseEntity<Object> handleStorageManagerException(StorageManagerException ex, WebRequest request) {
        LOG.error(ex.getMessage(), ex);
        // Creating a map to hold the error details.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", ex.getHttpStatus().value());
        body.put("error", ex.getHttpStatus().getReasonPhrase());
        body.put("errorCode", ex.getErrorCode());
        body.put("message", ex.getMessage());

        // Return a new ResponseEntity with the error details in the body, and the HTTP status code in the response.
        return new ResponseEntity<>(body, ex.getHttpStatus());
    }
}
