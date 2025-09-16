pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'yourdockerhubusername/my-app'
        IMAGE_TAG = 'latest'
    }

    options {
        skipStagesAfterUnstable()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Building application..."
                // Replace below with your build command (e.g., Maven, npm, etc.)
                sh './gradlew build' // For Java projects with Gradle
            }
        }

        stage('Test') {
            steps {
                echo "Running tests..."
                // Replace with actual test commands
                sh './gradlew test'
            }
        }

        stage('Docker Build') {
            steps {
                echo "Building Docker image..."
                script {
                    docker.build("${DOCKER_IMAGE}:${IMAGE_TAG}")
                }
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        export KUBECONFIG=$KUBECONFIG_FILE
                        helm upgrade --install my-app ./helm/my-app \
                          --set image.repository=${DOCKER_IMAGE} \
                          --set image.tag=${IMAGE_TAG} \
                          --namespace default \
                          --create-namespace
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '🎉 Deployment successful!'
        }
        failure {
            echo '❌ Pipeline failed.'
        }
        always {
            echo '📦 Pipeline finished.'
        }
    }
}
