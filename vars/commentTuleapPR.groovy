#!/usr/bin/groovy
import groovy.json.JsonSlurperClassic;
import groovy.json.JsonBuilder;
import java.net.URLEncoder;

def call(Map config) {

    // Configuration
    def accessKey = config.accessKey
    def serverPath = config.tuleapServer
    def targetProject = config.targetProject
    def targetRepo = config.targetRepo
    def comment = config.comment

    if (accessKey == null) {
        error "The accessKey parameter must be set"
    }
    if (serverPath == null) {
        error "The tuleapServer parameter must be set"
    }
    if (targetProject == null) {
        error "The targetProject parameter must be set"
    }
    if (targetRepo == null) {
        error "The targetRepo parameter must be set"
    }
    if (comment == null) {
        error "The comment parameter must be set"
    }

    def http = null
    def projectId = null
    def repoId = null
    def prId = null
    def retcode;
    def json;
    def jsonSlurper = new JsonSlurperClassic()

    /*
     * Deal with targetProject as string
     */
    if (targetProject instanceof String) {
        // Build Get parameter
        json = new groovy.json.JsonBuilder()
        json "shortname": targetProject
        def param = json.toString()
        param = URLEncoder.encode(param, "UTF-8")

        url = "${serverPath}/api/projects?query=${param}"
        http = new URL(url).openConnection() as HttpURLConnection

        http.setRequestMethod("GET")
        http.setRequestProperty("X-Auth-AccessKey", "${accessKey}")
        http.setRequestProperty("Accept", "application/json")

        retcode = 1
        try {
            http.connect()
        } finally {
            retcode = http.responseCode
            if (retcode != 200) {
                error "HTTTP GET ${url} : ${retcode}"
            }
            content = http.getInputStream().getText()
            def data = jsonSlurper.parseText(content)
            if (!data.empty) {
                projectId = data[0].id
            }
            http.disconnect()
        }
    } else {
        projectId = targetProject
    }

    /*
     * Deal with targetRepo as string
     */
    if (targetRepo instanceof String) {
        mapGit = []
        loop = true
        offset = 0
        while (loop) {
            url = "${serverPath}/api/projects/${projectId}/git?limit=3&offset=${offset}"
            http = new URL(url).openConnection() as HttpURLConnection

            http.setRequestMethod("GET")
            http.setRequestProperty("X-Auth-AccessKey", "${accessKey}")
            http.setRequestProperty("Accept", "application/json")

            retcode = 1
            try {
                http.connect()
            } finally {
                retcode = http.responseCode
                if (retcode != 200) {
                    error "HTTTP GET ${url} : ${retcode}"
                }
                content = http.getInputStream().getText()
                def data = jsonSlurper.parseText(content)
                if (!data.empty) {
                    mapGit += data.repositories
                }
                paginationSize = http.getHeaderField("x-pagination-size").toInteger()
                paginationLimit = http.getHeaderField("x-pagination-limit").toInteger()
                if (paginationSize == mapGit.size) {
                    loop = false
                } else {
                    offset += paginationLimit
                }
                http.disconnect()
            }
        }

        mapGit.each { e ->
            if (e.name == targetRepo) {
                repoId = e.id
                return true
            }
        }
    } else {
        repoId = targetRepo
    }

    if (repoId == null) {
        error "Repository Id is required"
    }


    /*
     * Get all open pull request
     */

    // Build Get parameter
    json = new groovy.json.JsonBuilder()
    json "status": "open"
    def dataStatus = json.toString()
    dataStatus = URLEncoder.encode(dataStatus, "UTF-8")

    openPrId = []
    loop = true
    offset = 0
    while (loop) {
        url = "${serverPath}/api/git/${repoId}/pull_requests?query=${dataStatus}&offset=${offset}"
        http = new URL(url).openConnection() as HttpURLConnection

        http.setRequestMethod("GET")
        http.setRequestProperty("X-Auth-AccessKey", "${accessKey}")
        http.setRequestProperty("Accept", "application/json")

        retcode = 1
        try {
            http.connect()
        } finally {
            retcode = http.responseCode
            if (retcode != 200) {
                error "HTTTP GET ${url} : ${retcode}"
            }
            content = http.getInputStream().getText()
            def data = jsonSlurper.parseText(content)
            if (!data.empty) {
                openPrId += data.collection
            }
            paginationSize = http.getHeaderField("x-pagination-size").toInteger()
            paginationLimit = http.getHeaderField("x-pagination-limit").toInteger()
            if (paginationSize == openPrId.size) {
                loop = false
            } else {
                offset += paginationLimit.toInteger()
            }
            http.disconnect()
        }
    }

    /*
     * Get the PR with the GIT_COMMIT of the current build
     */
    if (!openPrId.empty) {
        openPrId.each { e ->
            url = "${serverPath}/api/pull_requests/${e.id}"
            http = new URL(url).openConnection() as HttpURLConnection
            http.setRequestMethod('GET')
            http.setRequestProperty("X-Auth-AccessKey", "${accessKey}")
            http.setRequestProperty("Accept", 'application/json')
            retcode = 1;
            try {
                http.connect()
            } finally {
                retcode = http.responseCode
                if (retcode != 200) {
                    error "HTTTP GET ${url} : ${retcode}"
                }
                content = http.getInputStream().getText()
                def data = jsonSlurper.parseText(content)
                if (!data.empty) {
                    if (env.GIT_COMMIT == null || env.GIT_COMMIT.size() == 0) {
                        error "The GIT_COMMIT variable is not set"
                    }
                    if (data.reference_src == env.GIT_COMMIT) {
                        prId = data.id
                        http.disconnect()
                        return true;
                    }
                }
                http.disconnect()
            }
        }
    }

    if (prId == null) {
        // If no PR for this commit, exit the function
        echo "SKIPPED: No Pull Request open for this commit"
        return
    }


    /*
     * Post comment to PR
     */

    // Build POST data
    json = new groovy.json.JsonBuilder()
    json "content": comment
    dataComment = json.toString()

    url = "${serverPath}/api/pull_requests/${prId}/comments"
    http = new URL(url).openConnection() as HttpURLConnection

    http.setRequestMethod("POST")
    http.setDoOutput(true)
    http.setRequestProperty("X-Auth-AccessKey", "${accessKey}")
    http.setRequestProperty("Accept", "application/json")
    http.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    http.outputStream.write(dataComment.getBytes("UTF-8"))

    retcode = 1
    try {
        http.connect()
    } finally {
        retcode = http.responseCode
        http.disconnect()
    }

    if (retcode != 201) {
        echo "HTTP POST ${url} : ${retcode}"
        error "Post PR comment failed"
    }
}

