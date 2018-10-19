#!/usr/bin/env groovy

class ProductionLineGlobals implements Serializable {
    def static final String CONFLUENCE_BASE_URL = "http://confluence-core:8090/confluence"
    def static final String GERRIT_BASE_URL = "http://gerrit-core:8080/gerrit"
    def static final String GITLAB_BASE_URL = "http://gitlab-core:80/gitlab"
    def static final String GRAFANA_BASE_URL = "http://grafana-core:3000/grafana"
    def static final String GRAYLOG_BASE_URL = "http://graylog-core:9000/graylog"
    def static final String JENKINS_BASE_URL = "http://jenkins-core:8080/jenkins"
    def static final String JIRA_BASE_URL = "http://jira-core:8080/jira"
    def static final String NEXUS_BASR_URL = "	http://nexus3-core:8081/nexus3/"
    def static final String SCM_BASE_URL = "http://scm-core:8080/scm"
    def static final String SONARQUBE_BASE_URL = "http://sonarqube-core:9000/sonarqube"
}