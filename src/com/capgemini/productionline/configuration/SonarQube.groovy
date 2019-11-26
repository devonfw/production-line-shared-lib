#!/usr/bin/groovy
package com.capgemini.productionline.configuration

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

    /**
     *  Get the SonarQube version
     * @return the version
     */
    def getSonarVersion() {
        def response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'GET', url: "${this.sonarQubeBaseUrl}/api/server/version"
        return response.getContent()
    }

    /**
     *  Restart the SonarQube server
     * @return the HTTP response content
     */
    def restartSonar() {
        def response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: "${this.sonarQubeBaseUrl}/api/system/restart"
        return response.getContent()
    }

    /**
     *  Add a global webhook in SonarQube
     * @param webhookName the webhook name
     * @param webhookUrl the webhook url
     * @return the HTTP response content
     */
    def addWebhook(String webhookName, String webhookUrl) {
        def response
        if (getSonarVersion() > '7.1') {
            response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: "${this.sonarQubeBaseUrl}/api/webhooks/create?name=${webhookName}&url=${webhookUrl}"
        } else {
            response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: """${
                this.sonarQubeBaseUrl
            }/api/settings/set?key=sonar.webhooks.global&fieldValues=%7B%22name%22%3A%22${
                webhookName
            }%22%2C%22url%22%3A%22${webhookUrl}%22%7D"""
        }
        return response.getContent()
    }

    def importQualityProfile() {
        // TODO
    }

    /**
     *  Install a plugin in the SonarQube using the marketplace
     * @param pluginName The plugin name
     * @return the api response content
     */
    def installPlugin(String pluginName) {
        def response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'X-Forwarded-User', value: username], [maskValue: true, name: 'X-Forwarded-Groups', value: 'admins']], httpMode: 'POST', url: "${this.sonarQubeBaseUrl}/api/plugins/install?key=${pluginName}"
        return response.getContent()
    }
}
