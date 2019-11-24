#!/usr/bin/groovy
package com.capgemini.productionline.configuration;

class SonarQube implements Serializable {
    String username
    String sonarQubeBaseUrl
    def context

    /**
     * Initiate a new 'SonarQube' instance by providing a username and the SonarQube base URL.
     * <p>
     * @param context
     *    The pipeline context (usually the parameter should be set to 'this')
     * @param username
     *    A valid SonarQube username.
     * @param sonarQubeBaseUrl
     *    The base URL of the SonarQube server.
     * </p>
     */
    SonarQube(context, String username) {
        this.context = context
        this.username = username
        this.sonarQubeBaseUrl = ProductionLineGlobals.SONARQUBE_BASE_URL
    }

    /**
     * Initiate a new 'SonarQube' instance by providing a username and the SonarQube base URL.
     * <p>
     * @param context
     *    The pipeline context (usually the parameter should be set to 'this')
     * @param username
     *    A valid SonarQube username.
     * @param sonarQubeBaseUrl
     *    The base URL of the SonarQube server.
     * </p>
     */
    SonarQube(context, String username, String sonarQubeBaseUrl) {
        this.context = context
        this.username = username
        this.sonarQubeBaseUrl = sonarQubeBaseUrl
    }

    /**
     * Create a new 'SonarQube' API Token for the user of this instance.
     * <p>
     * @param tokenName
     *    The name of the token to create.
     * @param username
     *    A valid SonarQube username.
     * </p>
     */
    def getAuthToken(String tokenName) {
        def response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: this.sonarQubeBaseUrl + '/api/user_tokens/generate?name=' + tokenName
        def parsedJsonResponse = this.context.readJSON text: response.getContent()

        // Return a token 
        return parsedJsonResponse.token
    }

    def getSonarVersion() {
        def response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'GET', url: "${this.sonarQubeBaseUrl}/api/server/version"
        return response.getContent()
    }

    def restartSonar() {
        def response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: "${this.sonarQubeBaseUrl}/api/system/restart"
        return response.getContent()
    }

    def addWebhook(String webhookName, String webhookUrl) {
        def response
        if (this.getSonarVersion() > '7.1') {
            response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: "${this.sonarQubeBaseUrl}/api/webhooks/create?name=${webhookName}&url=${webhookUrl}"
        } else {
            response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: "${this.sonarQubeBaseUrl}/api/settings/set?key=sonar.webhooks.global&values=jenkins&values=http%3A%2F%2Fjenkins-core%3A8080%2Fjenkins%2Fsonarqube-webhook%2F"
        }
        return response.getContent()
    }

    def importQualityProfile() {
        // TODO
    }

    def installPlugin() {
        // TODO
    }
}
