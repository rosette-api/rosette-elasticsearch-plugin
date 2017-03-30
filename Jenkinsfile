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
            //Specific Job that tests against the latest version of Elasticsearch
            if ("${env.JOB_BASE_NAME}".contains('latest')) {
                withMaven(maven: "Basis", mavenLocalRepo: "$JENKINS_HOME/.m2/repository") {
                    sh "mvn -Delasticsearch.version=\$(curl -s http://search.maven.org/solrsearch/select?q=g:%22org.elasticsearch%22+AND+a:%22elasticsearch%22 | sed -n 's/.*latestVersion\":\"\\([0-9]\\.[0-9]\\.[0-9]\\).*/\\1/p') clean verify"
                }
            } else {
                withMaven(maven: "Basis", mavenLocalRepo: "$JENKINS_HOME/.m2/repository") {
                    sh "mvn clean install"
                }
                archiveArtifacts artifacts: "plugin/target/releases/*.zip", fingerprint: true
            }
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
