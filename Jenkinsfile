pipeline {
    agent any

    triggers {
        githubPush()
    }

    parameters {
        string(name: 'SERVER_HOST', defaultValue: '192.168.0.17', description: '배포 서버 호스트')
        string(name: 'SERVER_PORT', defaultValue: '6937', description: '배포 서버 SSH 포트')
        string(name: 'SERVER_USER', defaultValue: 'jaebeom', description: '배포 서버 사용자')
        string(name: 'DEPLOY_PATH', defaultValue: '/home/jaebeom/workspace/codism-file-storage', description: '프로젝트 경로')
    }

    stages {
        stage('Build and Deploy to k3s') {
            steps {
                echo 'Docker 이미지 빌드 및 k3s 배포'
                script {
                    sshagent(['server-pem']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no -p ${params.SERVER_PORT} \
                                ${params.SERVER_USER}@${params.SERVER_HOST} '
                                set -e
                                cd ${params.DEPLOY_PATH}
                                git pull origin master

                                # Docker 이미지 빌드
                                docker build -t codism-file-storage:latest .

                                # k3s에 이미지 import
                                docker save codism-file-storage:latest -o /tmp/codism-file-storage.tar
                                sudo k3s ctr images import /tmp/codism-file-storage.tar
                                rm /tmp/codism-file-storage.tar

                                # k3s 롤링 재시작
                                export KUBECONFIG=/home/jaebeom/.kube/config
                                kubectl apply -f k8s/configmap.yaml
                                kubectl apply -f k8s/secret.yaml
                                kubectl apply -f k8s/deployment.yaml
                                kubectl apply -f k8s/service.yaml
                                kubectl rollout restart deployment/codism-file-storage -n codism-prod
                                kubectl rollout status deployment/codism-file-storage -n codism-prod --timeout=120s

                                echo "k3s 배포 완료"
                                kubectl get pods -n codism-prod -l app=codism-file-storage
                            '
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                sshagent(['server-pem']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no -p ${params.SERVER_PORT} \
                            ${params.SERVER_USER}@${params.SERVER_HOST} '
                            docker image prune -af --filter "until=1h"
                            docker builder prune -af --filter "until=1h"
                        '
                    """
                }
            }
        }
        success {
            echo '배포 성공!'
        }
        failure {
            echo '배포 실패!'
        }
    }
}
