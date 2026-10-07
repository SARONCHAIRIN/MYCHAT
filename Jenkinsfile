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

        GOOGLE_APPLICATION_CREDENTIALS =
            credentials('mychat-firebase-service-account')
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
                    java -version
                    ./mvnw --version

                    test -n "$DB_URL" || {
                        echo "DB_URL missing"
                        exit 1
                    }

                    test -n "$DB_USERNAME" || {
                        echo "DB_USERNAME missing"
                        exit 1
                    }

                    test -n "$DB_PASSWORD" || {
                        echo "DB_PASSWORD missing"
                        exit 1
                    }

                    test -n "$GOOGLE_APPLICATION_CREDENTIALS" || {
                        echo "Firebase credentials missing"
                        exit 1
                    }

                    test -f "$GOOGLE_APPLICATION_CREDENTIALS" || {
                        echo "Firebase credential file not found"
                        exit 1
                    }

                    echo "MYCHAT CI environment configured."
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