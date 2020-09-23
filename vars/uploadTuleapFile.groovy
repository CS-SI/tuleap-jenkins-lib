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

import groovy.json.JsonSlurper;
import groovy.json.JsonBuilder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.FileSystems;

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

    def file = new File(filePath)

    if (!file.exists()) {
        error "File ${filePath} does not exist!"
    }

    def jsonSlurper = new JsonSlurper()

    def limit = 10;

    if (packageName != null) {
        // Look for package in project
        if (projectId == null) {
            error 'Project Id is required'
        }

        def offset = 0;
        while (packageId == null && offset >= 0) {
            // Connexion vers l'API Tuleap
            def http = new URL("${serverPath}/api/projects/${projectId}/frs_packages?limit=${limit}&offset=${offset}").openConnection() as HttpURLConnection

            http.setRequestMethod('GET')
            http.setRequestProperty("Accept", 'application/json')
            http.setRequestProperty("X-Auth-AccessKey", accessKey)

            def packages
            try {
                http.connect()
                retcode = http.responseCode

                echo "Listing project '${projectId}' (retcode = ${retcode})"
                if (retcode < 200 || retcode >= 300)
                    error "Failed to list packages for project '${projectId}'"

                packages = jsonSlurper.parseText(http.getInputStream().getText())
            } finally {
                http.disconnect()
            }

            echo "Packages " + packages.dump()
            for (pack in packages) {
                if (pack != null && pack.label == packageName)
                    packageId = pack.id
            }

            if (packages.size() > 0)
                // Prepare next loop
                offset = offset + limit
            else
                // No more data to read
                offset = -1
        }
    }

    if (releaseName != null) {
        // Look for release in package
        if (packageId == null) {
            error 'Package Id is required'
        }

        def offset = 0;
        while (releaseId == null && offset >= 0) {
            // Connexion vers l'API Tuleap
            def http = new URL("${serverPath}/api/frs_packages/${packageId}/frs_release?limit=${limit}&offset=${offset}").openConnection() as HttpURLConnection

            http.setRequestMethod('GET')
            http.setRequestProperty("Accept", 'application/json')
            http.setRequestProperty("X-Auth-AccessKey", accessKey)

            def releases
            try {
                http.connect()
                retcode = http.responseCode

                echo "Listing package '${packageId}' (retcode = ${retcode})"
                if (retcode < 200 || retcode >= 300)
                    error "Failed to retrieve releases"

                releases = jsonSlurper.parseText(http.getInputStream().getText())
            } finally {
                http.disconnect()
            }

            echo "Releases " + releases.dump()
            for (release in releases.collection) {
                if (release != null && release.name == releaseName)
                    releaseId = release.id
            }

            if (releases.collection.size() > 0)
                // Prepare next loop
                offset = offset + limit
            else
                // No more data to read
                offset = -1
        }

        if (releaseId == null) {
            // Create release

            // Contenu du message
            def json  = new groovy.json.JsonBuilder()
            json    "package_id": packageId,
                    "name": releaseName
            def message = json.toString()

            echo "Release Creation Parameters " + json.dump()

            // Connexion vers l'API Tuleap
            def http = new URL("${serverPath}/api/frs_release").openConnection() as HttpURLConnection

            http.setRequestMethod('POST')
            http.setDoOutput(true)
            http.setRequestProperty("Accept", 'application/json')
            http.setRequestProperty("X-Auth-AccessKey", accessKey)
            http.setRequestProperty("Content-Type", 'application/json; charset=UTF-8')
            http.outputStream.write(message.getBytes("UTF-8"))

            def release
            try {
                http.connect()
                retcode = http.responseCode

                echo "Creating release '${releaseName}' (retcode = ${retcode})"
                if (retcode < 200 || retcode >= 300)
                    error "Failed to create release"

                release = jsonSlurper.parseText(http.getInputStream().getText())
            } finally {
                http.disconnect()
            }

            echo "Release " + release.dump()
            releaseId = release.id
        }
    }

    if (releaseId == null) {
        error 'Release Id is required'
    }

    // Delete existing file

    // Connexion vers l'API Tuleap
    def http = new URL("${serverPath}/api/frs_release/${releaseId}/files").openConnection() as HttpURLConnection

    http.setRequestMethod('GET')
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("X-Auth-AccessKey", accessKey)

    def files
    try {
        http.connect()
        retcode = http.responseCode

        echo "Listing release '${releaseId}' (retcode = ${retcode})"
        if (retcode < 200 || retcode >= 300)
            error "Failed to list release '${releaseId}'"

        files = jsonSlurper.parseText(http.getInputStream().getText())
    } finally {
        http.disconnect()
    }

    echo "Files " + files.dump()
    for (f in files.files) {
        if (f.name == fileName) {
            http = new URL("${serverPath}/api/frs_files/${f.id}").openConnection() as HttpURLConnection
            http.setRequestMethod('DELETE')
            http.setRequestProperty("X-Auth-AccessKey", accessKey)

            try {
                http.connect()
                retcode = http.responseCode
            } finally {
               http.disconnect()
            }

            echo "Delete '${fileName}' (retcode = ${retcode})"
            if (retcode < 200 || retcode >= 300)
                error "Failed to delete ${fileName} (id=${f.id})"
        }
    }

    // Create file in release

    // Contenu du message
    def json  = new groovy.json.JsonBuilder()
    json    "release_id": releaseId,
            "name": fileName,
            "file_size": file.length()
    def message = json.toString()

    echo "File Creation Parameters (${filePath} as ${fileName}) " + json.dump()

    // Connexion vers l'API Tuleap
    http = new URL("${serverPath}/api/frs_files").openConnection() as HttpURLConnection

    // Préparation de la requete
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json; charset=UTF-8')
    http.setRequestProperty("X-Auth-AccessKey", accessKey)
    http.outputStream.write(message.getBytes("UTF-8"))
    
    def fileInfo
    try {
        http.connect()
        retcode = http.responseCode

        echo "Creating '${fileName}' (retcode = ${retcode})"
        if (retcode < 200 || retcode >= 300)
            error "Failed to create '${fileName}'"

        fileInfo = jsonSlurper.parseText(http.getInputStream().getText())
    } finally {
        http.disconnect()
    }

    echo "FileInfo " + fileInfo.dump()

    // Envoi contenu

    if (fileInfo.upload_href == null) {
        error 'Protocol error: Missing upload ref'
    }

    http = new URL("${serverPath}${fileInfo.upload_href}").openConnection() as HttpURLConnection

    // Préparation de la requete
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("X-Http-Method-Override", 'PATCH')
    http.setRequestProperty("Content-Type", 'application/offset+octet-stream')
    http.setRequestProperty("Tus-Resumable", '1.0.0')
    http.setRequestProperty("Upload-Offset", '0')
    http.setRequestProperty("X-Auth-AccessKey", accessKey)

    http.outputStream << file.text

    def uploadInfo
    try {
        http.connect()
        retcode = http.responseCode
        def offset = http.getHeaderFields()['Upload-Offset']
        if (offset != null)
            // Retrieve only the first element
            offset = offset[0]

        echo "Upload '${fileName}' (retcode = ${retcode} offset = ${offset})"
        if (retcode < 200 || retcode >= 300)
            error "Failed to upload '${fileName}'"

        if (offset == null || Integer.parseInt(offset) < file.length())
            error "Upload failed in a single run: ${offset} < ${file.length()}. Complete TUS protocol is not supported yet."
    } finally {
        http.disconnect()
    }

    echo "Upload '${filePath}' as '${fileName}' on '${serverPath}' (retcode = ${retcode})"

    return retcode
}
