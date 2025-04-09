def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            IMAGE_NAME = 'default' 
            TAG = 'latest'
            GIT_URL = 'https://github.com/Abhiramikannan/spring-boot-hello-world.git'
            GIT_BRANCH = 'main'
        }

        stages {
            stage('Setup Environment') {
                steps {
                    script {
                        env.IMAGE_NAME = config.image_name ?: 'abhiramikannan/javaimage'
                        env.TAG = config.tag ?: 'latest'
                        env.GIT_URL = config.git_url ?: 'https://github.com/Abhiramikannan/spring-boot-hello-world.git'
                        env.GIT_BRANCH = config.git_branch ?: 'main'
                    }
                }
            }

            stage('Checkout') {
                steps {
                    git url: "${env.GIT_URL}", branch: "${env.GIT_BRANCH}"
                }
            }

            stage('Build and Test') {
                steps {
                    sh 'mvn clean install'
                }
            }

            stage('Build Docker Image') {
                steps {
                    sh "docker build -t ${env.IMAGE_NAME}:${env.TAG} ."
                }
            }

            stage('Login & Push Docker Image') {
                steps {
                    withCredentials([usernamePassword(
                        credentialsId: config.docker_credentials_id,
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER -p $DOCKER_PASS"
                        sh "docker push ${env.IMAGE_NAME}:${env.TAG}"
                    }
                }
            }

            stage('Deploy to Kubernetes') {
                steps {
                    sh "kubectl delete deployment --all"
                    sh "kubectl create deployment hello1springboot --image=${env.IMAGE_NAME}:${env.TAG} --dry-run=client -o yaml > deploy.yaml"
                    sh "kubectl apply -f deploy.yaml"
                }
            }
        }

        post {
            success {
                echo "✅ Deployment succeeded!"
            }
            failure {
                echo "❌ Deployment failed."
            }
        }
    }
}
