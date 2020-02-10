Utilitary Jenkins Library to integrate with [Tuleap](https://www.tuleap.org/).

This library needs to be *trusted*, so it has to be declared in Jenkins in the [Global Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries).

# Features

Tuleap offers an API to report effective build status to the associated commit, allowing this information to be displayed on the pull request dashboard.
But this feature requires some non trivial actions, as described in the [related documentation](https://docs.tuleap.org/user-guide/pullrequest.html#configure-jenkins-to-tuleap-feedback).

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

# License

Cf. [license file](LICENSE.txt)

# References

More documentation to develop a library at <https://jenkins.io/doc/book/pipeline/shared-libraries/>
