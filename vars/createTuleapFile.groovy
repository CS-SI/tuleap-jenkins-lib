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

import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

@NonCPS
def call(String serverPath, String accessKey, Integer releaseId, String fileName, String fileSize) {
    // Contenu du message
    def json  = new groovy.json.JsonBuilder()
    json    "release_id": releaseId,
            "name": fileName,
            "file_size": fileSize
    def message = json.toString()

    def jsonSlurper = new JsonSlurper()

    echo "File Creation Parameters: " + json.dump()

    // Connexion vers l'API Tuleap
    def http = new URL("${serverPath}/api/frs_files").openConnection() as HttpURLConnection

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

    if (fileInfo.upload_href == null) {
        error 'Protocol error: Missing upload ref'
    }

    return fileInfo.upload_href
}
