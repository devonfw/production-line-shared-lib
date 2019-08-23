#!/usr/bin/env groovy
package com.capgemini.productionline.configuration

class ProductionLineGlobals implements Serializable {
    static final String CONFLUENCE_BASE_URL = "http://confluence-core:8090/confluence"
    static final String GERRIT_BASE_URL = "http://gerrit-core:8080/gerrit"
    static final String GITLAB_BASE_URL = "http://gitlab-core:80/gitlab"
    static final String GRAFANA_BASE_URL = "http://grafana-core:3000/grafana"
    static final String GRAYLOG_BASE_URL = "http://graylog-core:9000/graylog"
    static final String JENKINS_BASE_URL = "http://jenkins-core:8080/jenkins"
    static final String JIRA_BASE_URL = "http://jira-core:8080/jira"
    static final String NEXUS_BASR_URL = "	http://nexus3-core:8081/nexus3/"
    static final String SCM_BASE_URL = "http://scm-core:8080/scm"
    static final String SONARQUBE_BASE_URL = "http://sonarqube-core:9000/sonarqube"
}