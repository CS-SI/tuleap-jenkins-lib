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

@NonCPS
def call(String serverPath, String accessKey, Integer releaseId, String fileName) {
    // Connexion vers l'API Tuleap
    def http = new URL("${serverPath}/api/frs_release/${releaseId}/files").openConnection() as HttpURLConnection

    http.setRequestMethod('GET')
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("X-Auth-AccessKey", accessKey)

    def jsonSlurper = new JsonSlurper()

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
}
