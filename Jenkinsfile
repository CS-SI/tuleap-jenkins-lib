pipeline {
    agent {
        docker {
            image "openjdk:8"
        }
    }
    stages {
        stage ('Build') {
            steps {
                sh './gradlew test'
            }
        }
    }
    post {
        always {
            emailext body: """${env.PROJECT_NAME} - Build # ${env.BUILD_NUMBER} - ${currentBuild.currentResult}:

Check console output at ${env.BUILD_URL} to view the results.""", subject: "${env.PROJECT_NAME} - Build # ${env.BUILD_NUMBER} - ${currentBuild.currentResult}!", recipientProviders: [developers(), requestor()]
        }
    }
}
