@Library('jenkins-pipeline-shared-libraries')_

import org.kie.jenkins.MavenCommand

changeAuthor = env.ghprbPullAuthorLogin ?: CHANGE_AUTHOR
changeBranch = env.ghprbSourceBranch ?: CHANGE_BRANCH
changeTarget = env.ghprbTargetBranch ?: CHANGE_TARGET

optaplannerRepo = 'optaplanner'
quickstartsRepo = 'optaplanner-quickstarts'
kogitoRuntimesRepo = 'kogito-runtimes'
quarkusRepo = 'quarkus'

pipeline {
    agent {
        label 'kie-rhel7 && kie-mem16g'
    }
    tools {
        maven 'kie-maven-3.6.2'
        jdk 'kie-jdk11'
    }
    options {
        timeout(time: 120, unit: 'MINUTES')
    }
    environment {
        MAVEN_OPTS = '-Xms1024m -Xmx4g'
    }
    stages {
        stage('Initialize') {
            steps {
                script {
                    mailer.buildLogScriptPR()

                    checkoutRuntimesRepo()
                    checkoutRepo(optaplannerRepo)
                    dir(quickstartsRepo) {
                        // If the PR to OptaPlanner targets the 'master' branch, we assume the branch 'development' for quickstarts.
                        String quickstartsChangeTarget = changeTarget == 'master' ? 'development' : changeTarget
                        githubscm.checkoutIfExists(quickstartsRepo, changeAuthor, changeBranch, 'kiegroup', quickstartsChangeTarget, true)
                    }
                }
            }
        }
        stage('Build quarkus') {
            when {
                expression { return getQuarkusBranch() }
            }
            steps {
                script {
                    checkoutQuarkusRepo()
                    getMavenCommand(quarkusRepo, false)
                        .withProperty('quickly')
                        .run('clean install')
                }
            }
        }
        stage('Build Kogito Runtimes skipping tests') {
            steps {
                script {
                    getMavenCommand(kogitoRuntimesRepo)
                        .skipTests(true)
                        .withProperty('skipITs', true)
                        .run('clean install')
                }
            }
        }
        stage('Build OptaPlanner') {
            steps {
                script {
                    getMavenCommand(optaplannerRepo)
                        .withProfiles(['run-code-coverage'])
                        .withProperty('full')
                        .run('clean install')
                }
            }
        }
        stage('Analyze OptaPlanner by SonarCloud') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'SONARCLOUD_TOKEN', variable: 'SONARCLOUD_TOKEN')]) {
                        getMavenCommand(optaplannerRepo)
                                .withOptions(['-e', '-nsu'])
                                .withProperty('sonar.projectKey', 'org.optaplanner:optaplanner')
                                .withProfiles(['sonarcloud-analysis'])
                                .run('validate')
                    }
                }
            }
        }
        stage('Build OptaPlanner Quickstarts') {
            steps {
                script {
                    getMavenCommand(quickstartsRepo)
                        .run('clean install')
                }
            }
        }
    }
    post {
        always {
            sh '$WORKSPACE/trace.sh'
            junit '**/target/surefire-reports/**/*.xml, **/target/failsafe-reports/**/*.xml'
        }
        failure {
            script {
                mailer.sendEmail_failedPR()
            }
        }
        unstable {
            script {
                mailer.sendEmail_unstablePR()
            }
        }
        fixed {
            script {
                mailer.sendEmail_fixedPR()
            }
        }
        cleanup {
            script {
                // Clean also docker in case of usage of testcontainers lib
                util.cleanNode('docker')
            }
        }
    }
}

void checkoutRepo(String repo, String dirName=repo) {
    dir(dirName) {
        githubscm.checkoutIfExists(repo, changeAuthor, changeBranch, 'kiegroup', changeTarget, true)
    }
}

void checkoutRuntimesRepo() {
    String targetBranch = changeTarget
    String [] versionSplit = targetBranch.split("\\.")
    if (versionSplit.length == 3
        && versionSplit[0].isNumber()
        && versionSplit[1].isNumber()
       && versionSplit[2] == 'x') {
        targetBranch = "${Integer.parseInt(versionSplit[0]) - 7}.${versionSplit[1]}.x"
    } else {
        echo "Cannot parse changeTarget as release branch so going further with current value: ${changeTarget}"
       }
    dir(kogitoRuntimesRepo) {
        githubscm.checkoutIfExists(kogitoRuntimesRepo, changeAuthor, changeBranch, 'kiegroup', targetBranch, true)
    }
}

void checkoutQuarkusRepo() {
    dir(quarkusRepo) {
        checkout(githubscm.resolveRepository(quarkusRepo, 'quarkusio', getQuarkusBranch(), false))
    }
}

MavenCommand getMavenCommand(String directory, boolean addQuarkusVersion=true) {
    mvnCmd = new MavenCommand(this, ['-fae'])
                .inDirectory(directory)
    if (addQuarkusVersion && getQuarkusBranch()) {
        mvnCmd.withProperty('version.io.quarkus', '999-SNAPSHOT')
    }
    return mvnCmd
}

String getQuarkusBranch() {
    return env['QUARKUS_BRANCH']
}