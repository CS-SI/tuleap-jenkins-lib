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
def call(String serverPath, String accessKey, Integer projectId, String packageName) {
    def packageId = null;

    // Look for package in project
    if (projectId == null) {
        error 'Project Id is required'
    }

    def jsonSlurper = new JsonSlurper()

    def limit = 10;
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
            if (retcode < 200 || retcode >= 300) {
                err = jsonSlurper.parseText(http.getInputStream().getText())
                error "Failed to list packages for project '${projectId}' : ${err.error.message} (${err.error.code})"
            }

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
    return packageId as Integer
}
