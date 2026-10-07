pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
    JAVA_HOME = '/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home'
    PATH = "${JAVA_HOME}/bin:${env.PATH}"

    DB_URL = credentials('mychat-db-url')
    DB_USERNAME = credentials('mychat-db-username')
    DB_PASSWORD = credentials('mychat-db-password')
}

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Environment') {
            steps {
                sh '''
                    echo "=== Java ==="
                    java -version

                    echo "=== Maven ==="
                    ./mvnw --version
                '''
            }
        }

        stage('Compile') {
            steps {
                sh './mvnw clean compile'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }

        stage('Package') {
            steps {
                sh './mvnw package -DskipTests'
            }
        }
    }

    post {
        success {
            echo 'MYCHAT backend CI passed.'
        }

        failure {
            echo 'MYCHAT backend CI failed.'
        }

        always {
            junit allowEmptyResults: true,
                testResults: 'target/surefire-reports/*.xml'

            archiveArtifacts artifacts: 'target/*.jar',
                allowEmptyArchive: true,
                fingerprint: true
        }
    }
}