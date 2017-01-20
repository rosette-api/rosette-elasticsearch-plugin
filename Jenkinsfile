node {
    env.JAVA_HOME="${tool 'java8'}"
    env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
    try {
        stage("Clean up") {
            step([$class: 'WsCleanup'])
        }
        stage("Checkout Code") {
            checkout scm
        }
        stage("Build") {
            withMaven(maven: "Basis", mavenLocalRepo: "$JENKINS_HOME/.m2/repository") {
                sh "mvn clean install"
            }
            archiveArtifacts artifacts: "target/releases/*.zip", fingerprint: true
        }
        slack(true)
    } catch (e) {
        currentBuild.result = "FAILED"
        slack(false)
        throw e
    }
}

def slack(boolean success) {
    def color = success ? "#00FF00" : "#FF0000"
    def status = success ? "SUCCESSFUL" : "FAILED"
    def message = status + ": Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})"
    slackSend(color: color, channel: "#juggernaut", message: message)
}
