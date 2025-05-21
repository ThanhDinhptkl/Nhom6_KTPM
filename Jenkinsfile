pipeline {
    agent any
    
    tools {
        maven 'Maven'  // công cụ đã được cấu hình trong Jenkins
        jdk 'JDK'      // công cụ đã được cấu hình trong Jenkins
    }
    
    stages {
        stage('Build Discovery Service') {
            steps {
                bat 'cd BackEnd\\discovery-server && mvn clean package -DskipTests'
            }
        }
        
        stage('Deploy Discovery Service') {
            steps {
                bat 'docker run --rm -v %CD%:/workspace -v C:\\ProgramData\\Jenkins\\.ssh:/root/.ssh -w /workspace cytopia/ansible:latest-tools bash -c "cd ansible-simple && ANSIBLE_HOST_KEY_CHECKING=False ansible-playbook -i hosts deploy.yml -v"'
            }
        }
        
        stage('Health Check') {
            steps {
                echo "Waiting 60 seconds for service to start completely..."
                bat 'ping -n 60 127.0.0.1 > nul'
                
                script {
                    try {
                        powershell 'Invoke-WebRequest -Uri http://14.225.215.93:8761 -UseBasicParsing -TimeoutSec 30'
                        echo "Health check successful!"
                    } catch (Exception e) {
                        echo "Health check warning: ${e.message}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed! Test'
        }
    }
}