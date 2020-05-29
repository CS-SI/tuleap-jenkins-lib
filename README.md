Utilitary Jenkins Library to integrate with [Tuleap](https://www.tuleap.org/).

![Java CI](https://github.com/CS-SI/tuleap-jenkins-lib/workflows/Java%20CI/badge.svg)
[![Build Status](https://travis-ci.org/CS-SI/tuleap-jenkins-lib.svg?branch=develop)](https://travis-ci.org/CS-SI/tuleap-jenkins-lib)

This library needs to be *trusted*, so it has to be declared in Jenkins in the [Global Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries).

# Features

## Build status

Tuleap offers an API to report effective build status to the associated commit, allowing this information to be displayed on the pull request dashboard.
But this feature requires some non trivial actions, as described in the [related documentation](https://docs.tuleap.org/user-guide/code-review/pullrequest.html#configure-jenkins-to-tuleap-feedback).

The `sendTuleapStatus` step simplifies this action.
The expected parameters:

* `tuleapServer`: Server's URL (example: `https://tuleap.example.com`).
* `targetRepo`: Path to the repository (example: `project-name/repo-name.git`).
* `status`: Effective status (`"success"` / `"failure"`). If not set, the status is based on the current build (cf. `${currentBuild.currentResult}`).
* `gitToken`: Specific token to access the API. This token can be retrieved in the administration dashboard of the repository, in the **Token** tab.

As the Git Token is volatile (it is possible to revoke and regenerate this token) and quite sensitive, it is recommended to store this information as a Text Credential in Jenkins, credential associated to the folder of the project/repository concerned.

In the following examples, the API token is named `git-token` in Jenkins.

Declarative Pipeline example:

```groovy
pipeline {
    environment {
        GIT_TOKEN = credentials('git-token')
    }
    stages {
        // ...
    }
    post {
        always {
            sendTuleapStatus gitToken: this.env.GIT_TOKEN,
                             tuleapServer: 'https://tuleap.example.com',
                             targetRepo: 'project-name/repo-name.git'
        }
    }
}

```

Scripted Pipeline example:

```groovy
withCredentials([
    string(credentialsId: 'git-token', variable: 'token_git'),
]){
    sendTuleapStatus gitToken: this.env.token_git,
                     targetRepo: 'projet/depot-test.git',
                     status: "success"
}
```

## File upload

Tuleap offers an API to upload files in the [FRS](https://docs.tuleap.org/user-guide/documents-and-files/frs.html).
It is usefull to have the hability to upload file from the CI/CD pipeline in order to automatically store the delivery.

The `uploadTuleapFile` step simplify this action.
The expected parameters:

* `tuleapServer`: Server's URL (example: `https://tuleap.example.com`).
* `accessKey`: the API token of the user, generated from the personnal page.
* `fileName`: the file's name to create in the release.
* `filePath`: the path to the effective file to upload.

As the `accessKey` is volatile (it is possible to revoke and regenerate this token) and quite sensitive, it is recommended to store this information as a Text Credential in Jenkins, credential associated to the folder of the project/repository concerned.

Then, to decide where to upload the file, we have to provide other optional arguments:

* `projectId`: the Id (numeric) of the project.
* `packageId`: the Id (numeric) of the package concerned.
* `packageName`: the name of the package concerned, if packageId is not known. Requires a valid projectId.
* `releaseId`: the Id (numeric) of the release concerned.
* `releaseName`: the name of the release concerned, if releaseId is not known. Requires a valid projectId. The release would be created if not found.

Their usage depends on the situation:

1. We know the `releaseId`, so we just provide it.
2. We don't know the `releaseId`, but its name or we want to create it, so we provide `releaseName`. In this case, we have to provide the `packageId`. If we don't know the id but we know its name, we can provide `packageName`. But in this case, we have to provide the `projectId`.

In the following examples, the access key is named `tuleap-token` in Jenkins).

Declarative Pipeline example:

```groovy
pipeline {
    environment {
        API_KEY = credentials('tuleap-token')
    }
    stages {
        // ...
    }
    post {
        always {
            uploadTuleapFile tuleapServer: "https://tuleap.example.com",
                                accessKey: "${API_KEY}",
                                packageId: 110,
                                releaseName: "${BRANCH_NAME}",
                                fileName: "impl-1.0-SNAPSHOT.jar",
                                filePath: "${WORKSPACE}/impl/target/impl-1.0-SNAPSHOT.jar"
        }
    }
}

```

## Label pull request

Tuleap offers an API to update labels on a pull request. The pull request is determined by the commit hash of the current jenkins build.

The `labelTuleapPR` step simplify this action. The expected parameters:

* `accessKey` The API token of the user, generated from the personnal page.
* `tuleapServer` Server's URL.
* `targetProject` The project name or id where to search the PR.
* `targetRepo` The repository name or id where to search to PR.
* `addLabels` The label or list of label name to add on the PR.
* `rmLabels` The label or list of label name to remove on the PR.

As the `accessKey` is volatile (it is possible to revoke and regenerate this token) and quite sensitive, it is recommended to store this information as a Text Credential in Jenkins, credential associated to the folder of the project/repository concerned.

Concern the parameter `addLabels` and `rmLabels`, a least one of these parameter are required. Both could be also given in the same call.

In the following examples, the access key is named `tuleap-token` in Jenkins).

Declarative Pipeline example:

```groovy
pipeline {
    environment {
        TULEAP_ACCESS_KEY = credentials('tuleap-token')
    }
    stages {
        // ...
    }
    post {
        failure {
            labelTuleapPR accessKey: this.env.TULEAP_ACCESS_KEY,
                          tuleapServer: "https://tuleap.example.com",
                          targetProject: "project_name",
                          targetRepo: "git_repo_name",
                          addLabels: ["COMPILATION FAILED"]
        }
        success {
            labelTuleapPR accessKey: this.env.TULEAP_ACCESS_KEY,
                          tuleapServer: "https://tuleap.example.com",
                          targetProject: "project_name",
                          targetRepo: "git_repo_name",
                          rmLabels: ["COMPILATION FAILED"]
        }
    }
}
```

## Comment pull request

Tuleap offers an API to post a comment on a pull request. The pull request is determined by the commit hash of the current jenkins build.

The `commentTuleapPR` step simplify this action. The expected parameters:

* `accessKey` The API token of the user, generated from the personnal page.
* `tuleapServer` Server's URL.
* `targetProject` The project name or id where to search the PR.
* `targetRepo` The repository name or id where to search to PR.
* `comment` The comment to post on the PR.

As the `accessKey` is volatile (it is possible to revoke and regenerate this token) and quite sensitive, it is recommended to store this information as a Text Credential in Jenkins, credential associated to the folder of the project/repository concerned.

In the following examples, the access key is named `tuleap-token` in Jenkins).

Declarative Pipeline example:

```groovy
pipeline {
    environment {
        TULEAP_ACCESS_KEY = credentials('tuleap-token')
    }
    stages {
        // ...
    }
    post {
        always {
            commentTuleapPR accessKey: this.env.TULEAP_ACCESS_KEY,
                            tuleapServer: "https://tuleap.example.com",
                            targetProject: "project_name",
                            targetRepo: "git_repo_name",
                            comment: "Result of the build: ${env.BUILD_URL}"
        }
    }
}
```

# License

Cf. [license file](LICENSE.txt)

# References

More documentation to develop a library at <https://jenkins.io/doc/book/pipeline/shared-libraries/>
