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
def call(String serverPath, String accessKey, Integer packageId, String releaseName) {
    // Contenu du message
    def json  = new groovy.json.JsonBuilder()
    json    "package_id": packageId,
            "name": releaseName
    def message = json.toString()

    echo "Release Creation Parameters " + json.dump()

    def jsonSlurper = new JsonSlurper()

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

    return release.id as Integer
}