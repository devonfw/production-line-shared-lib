package com.capgemini.productionline.configuration

import groovy.json.*

/**
* Contains the configuration methods of the nexus component
* <p>
*     The main purpose collecting configuration methods.
*
* Created by cdjahan on 14.12.2018.
*/

class Nexus implements Serializable {

  def String adminUser = ""
  def String adminPasswd = ""
  def String nexusHostUrl = ""



  Nexus (adminUser, adminPasswd, nexusHostUrl) {
    this.adminUser = adminUser
    this.adminPasswd = adminPasswd
    this.nexusHostUrl = nexusHostUrl
  }

  /**
   * Method for adding a groovy script for repository creation to the repository manager that should be run by the backend Server. The method uses the groovy script API
   * <p>
   * @param repoName
   *    uniqe name for the repository to be created
   * @param scriptName
   *    Script name that should be used to add the script to the repository manager, as the nexus Script API will be used
   * @param type
   *    Type of the script used with script API (groovy per default).
   */
  public Object addScript (String repoName, String scriptName, String type="groovy") {

    def result_json = new JsonSlurper().parseText('{}')
    def result_staus = true;
    def jsonDataSlurper = new JsonSlurper().parseText('{}')

    jsonDataSlurper << [name: scriptName,
    type: type,
    content: "repository.createMavenHosted('${repoName}')" ];

    def jsonDataOutput = JsonOutput.toJson(jsonDataSlurper)

    def proc = ["curl", "-u", "${adminUser}:${adminPasswd}", "-X", "POST", "-v", "-H", "Content-Type: application/json", "-d", "${jsonDataOutput}", "${nexusHostUrl}/service/rest/v1/script/"].execute()
    Thread.start { System.err << proc.err }

    def sout = new StringBuilder(), serr = new StringBuilder();

    proc.consumeProcessOutput(sout, serr)
    proc.waitForOrKill(1000)
    if (!sout.toString().isEmpty()) {
      result_staus = false;
    }

    result_json << [status: result_staus,
    message: sout.toString()];

    return result_json;
  }

  /**
   * Method runs a script that has been previously added to the repository manager.
   * <p>
   * @param scriptName
   *    Script name that should be used to add the script to the repository manager, as the nexus Script API will be used
   */
  public Object runScript (String scriptName) {

    def result_json = new JsonSlurper().parseText('{}')
    def result_staus = true;

    // Thread.start { System.err << proc.err }

    def sout = new StringBuilder(), serr = new StringBuilder();
    // The Script was successfully added. and can be executed.
    def proc = ["curl", "-u", "${adminUser}:${adminPasswd}", "-X", "POST", "-v", "-H", "Content-Type: text/plain", "${nexusHostUrl}/service/rest/v1/script/${scriptName}/run"].execute()
    proc.waitForOrKill(1000)
    proc.consumeProcessOutput(sout, serr)
    // if the repository creation fails, the result may contain the failure cause
    if (sout.toString().contains("failure")) {
      result_staus = false;
    }

    result_json << [status: result_staus,
    message: sout.toString()  ];

    return result_json;
    }

  public Object addMavenHostedRepo(String repoName, String scriptName) {
    def addScript = addScript(repoName, scriptName);
    if (!addScript.status) {
      return addScript
    }
    else return runScript(scriptName);
  }

}
