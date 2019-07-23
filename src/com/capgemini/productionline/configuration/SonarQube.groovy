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
    SonarQube(context, String username)  {
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
    SonarQube(context, String username, String sonarQubeBaseUrl)  {
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

    def importQualityProfile() {
        // TODO
    }

    def installPlugin() {
        // TODO
    }
}
