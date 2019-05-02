pipeline {
    agent {
        docker {
            image "openjdk:7"
        }
    }
    stages {
        stage ('Build') {
            steps {
                sh './gradlew test'
            }
        }
    }
}