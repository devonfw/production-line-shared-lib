#!/usr/bin/groovy
package com.capgemini.productionline.configuration

import groovy.json.JsonSlurper

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
     * Method for creating a SonarQube token for SQ user. This method is working only for ProductionLine instances.
     * Backward compatibility
     * <p>
     * @param login_id
     *    SonarQube user login id - for this user token will be generated
     * @param token_name
     *    name of new token which will be created
     */
    public createSQToken(String login_id, String token_name) {
        def oldUsername = this.username
        this.username = login_id
        def token = getAuthToken(token_name)
        this.username = oldUsername
        return token
    }

    /**
     * Method for checking if a SonarQube token for SQ user exists. This method is working only for ProductionLine instances.
     * <p>
     * @param login_id
     *    SonarQube user login id
     * @param token_name
     *    name of token which will be checked
     */
    public boolean getSQToken(String login_id, String token_name) {
        try {
            def get = new URL(this.sonarQubeBaseUrl + "/api/user_tokens/search?login=" + login_id).openConnection();
            get.setRequestProperty( 'X-Forwarded-User', login_id )
            get.setRequestProperty( 'X-Forwarded-Groups', 'admins' )
            def getRC = get.getResponseCode();
            if(getRC.equals(200)) {
                JsonSlurper slurper = new JsonSlurper()
                Map parsedJson = slurper.parseText(get.getInputStream().getText())
                if(parsedJson.userTokens.name.contains(token_name)) {
                    return true
                } else {
                    return false
                }
            } else {
                println("Unable to get SQ token with. HTTP code " + get.getResponseCode())
                return false
            }
        } catch (Exception ex) {
            println("Unable to get SQ token " + ex)
            return false
        }
    }

    /**
     * Method for revoking the SonarQube token from SQ user. This method is working only for ProductionLine instances.
     * <p>
     * @param login_id
     *    SonarQube user login id
     * @param token_name
     *    name of token which will be revoked
     */
    public boolean revokeSQToken(String login_id, String token_name) {
        try {
            def post = new URL(this.sonarQubeBaseUrl + "/api/user_tokens/revoke?login=" + login_id + "&name=" + token_name).openConnection();
            post.setRequestMethod("POST")
            post.setDoOutput(true)
            post.setRequestProperty( 'X-Forwarded-User', login_id )
            post.setRequestProperty( 'X-Forwarded-Groups', 'admins' )
            def postRC = post.getResponseCode();
            if(postRC.equals(204)) {
                println("SQ token " + token_name + " has been successful revoked")
                return true
            } else {
                println("SQ token deletion failed with. HTTP code " + get.getResponseCode())
                return false
            }
        } catch (Exception ex) {
            println("SQ token deletion failed " + ex)
            return false
        }
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
     * Method for restarting SonarQube. This method is working only for ProductionLine instances.
     *
     */
    public boolean restartSonarQube() {
        return this.restartSonar()
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

    /**
     * Method for installing plugins in SonarQube. This method is working only for ProductionLine instances.
     * Backward compatibility
     * <p>
     * @param plugin_id
     *    SonarQube plugin ID
     */
    public boolean installSonarQubePlugins(String plugin_id) {
        try {
            if (this.installPlugin(plugin_id) != null) {
                return true
            }
        } catch (Exception ex) {
            println("Unable to get SQ token " + ex)
        }
            return false
    }

    /**
     * Method for checking plugins in SonarQube. This method is working only for ProductionLine instances.
     * <p>
     * @param plugin_id
     *    SonarQube plugin ID
     */
    public boolean getSonarQubePlugins(String plugin_id) {
        try {
            def get = new URL(this.sonarQubeBaseUrl + "/api/plugins/installed").openConnection();
            get.setRequestProperty( 'X-Forwarded-User', username )
            get.setRequestProperty( 'X-Forwarded-Groups', 'admins' )
            def getRC = get.getResponseCode();
            if(getRC == 200) {
                JsonSlurper slurper = new JsonSlurper()
                def parsedJson = slurper.parseText(get.getInputStream().getText())
                if (parsedJson.plugins.key.contains(plugin_id)){
                    this.context.println("Plugin " + plugin_id + ", is allready installed")
                    return true
                } else {
                    this.context.println("Plugin " + plugin_id + ", is not installed. HTTP code " + get.getResponseCode())
                    return false
                }
            }
        } catch (Exception ex) {
            this.context.println("Unable to get SQ token " + ex)
            return false
        }
    }

    /**
     * Method for checking status of SonarQube. This method is working only for ProductionLine instances.
     * Return UP, STARTING and DOWN
     */
    public getSonarQubeStatus() {
        try {
            def get = new URL(this.sonarQubeBaseUrl + "/api/system/status").openConnection();
            get.setRequestProperty( 'X-Forwarded-User', username )
            get.setRequestProperty( 'X-Forwarded-Groups', 'admins' )
            def getRC = get.getResponseCode();
            if(getRC ==200) {
                JsonSlurper slurper = new JsonSlurper()
                def parsedJson = slurper.parseText(get.getInputStream().getText())
                this.context.println("SonarQube server is UP")
                return parsedJson.status
            } else {
                this.context.println("Unable to check SQ status. HTTP code " + get.getResponseCode())
                return "DOWN"
            }
        } catch (Exception ex) {
            this.context.println("SonarQube server is unavailable " + ex)
            return "DOWN"
        }
    }
}
