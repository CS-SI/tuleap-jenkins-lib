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
def call(String serverPath, String accessKey, Integer packageId, String releaseName) {
    def releaseId = null;

    // Look for release in package
    if (packageId == null) {
        error 'Package Id is required'
    }

    def jsonSlurper = new JsonSlurper()

    def limit = 10;
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
            if (retcode < 200 || retcode >= 300) {
                err = jsonSlurper.parseText(http.getInputStream().getText())
                error "Failed to retrieve releases for package'${packageId}' : ${err.error.message} (${err.error.code})"
            }

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

    return releaseId as Integer
}
