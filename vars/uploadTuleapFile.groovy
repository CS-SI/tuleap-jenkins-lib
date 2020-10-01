#!/usr/bin/groovy

/*
 * Copyright 2020, CS Systemes d'Information, http://www.c-s.fr
 * 
 * This file is part of Tuleap Jenkins Library project
 *     https://www.github.com/CS-SI/tuleap-jenkins-lib
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
def call(Map config) {

    // Configuration
    def serverPath = config.tuleapServer;
    def accessKey = config.accessKey;
    def projectId = config.projectId;
    def packageId = config.packageId;
    def packageName = config.packageName;
    def releaseId = config.releaseId;
    def releaseName = config.releaseName;
    def fileName = config.fileName;
    def filePath = config.filePath;

    int retcode = 1;

    if (serverPath == null) {
        error "The tuleapServer parameter must be set"
    }
    // Remove possible trailing slashes: uneeded as we add a slash
    serverPath = serverPath.replaceAll('/+$',"")

    if (accessKey == null) {
        error "The accessKey parameter must be set"
    }

    if (fileName == null) {
        error "The fileName parameter must be set"
    }

    if (filePath == null) {
        error "The filePath parameter must be set"
    }

    def fileSize = sh(returnStdout: true, script: "stat -c %s '${filePath}'").trim()

    if (packageName != null) {
        packageId = getTuleapPackageId(serverPath, accessKey, projectId, packageName)
    }

    if (releaseName != null) {
        releaseId = getTuleapReleaseId(serverPath, accessKey, packageId, releaseName)

        if (releaseId == null) {
            // Create release
            releaseId = createTuleapRelease(serverPath, accessKey, packageId, releaseName)
        }
    }

    if (releaseId == null) {
        error 'Release Id is required'
    }

    // Delete existing file
    deleteTuleapFile(serverPath, accessKey, releaseId, fileName)

    // Create file in release
    uploadHref = createTuleapFile(serverPath, accessKey, releaseId, fileName, fileSize)
    if (uploadHref == null) {
        error 'Protocol error: Missing upload ref'
    }

    // Envoi contenu
    echo "Uploading '${filePath}' as '${fileName}' on '${serverPath}'"

    sh "curl -X PATCH -H 'X-Auth-AccessKey: ${accessKey}' \
    -H 'Content-Type: application/offset+octet-stream' \
    -H 'Content-Length: ${fileSize}' \
    -H 'Upload-Offset: 0' \
    -H 'Tus-Resumable: 1.0.0' \
    --data-binary '@${filePath}' \
    '${serverPath}${uploadHref}'"

    echo "Uploaded '${filePath}' as '${fileName}' on '${serverPath}'"

    return retcode
}
