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
}