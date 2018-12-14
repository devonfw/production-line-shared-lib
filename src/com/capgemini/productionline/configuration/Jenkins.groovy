package com.capgemini.productionline.configuration.jenkins

// following imports needed for the credential objects
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import com.cloudbees.plugins.credentials.common.*
import hudson.util.Secret
import hudson.tools.*


import jenkins.model.*;
import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig;


// following imports needed jenkins plugin installation
// import jenkins.model.*

/**
 * Contains the configuration methods of the jenkins component
 * <p>
 *     The main purpose collecting configuration methods.
 *
 * Created by tlohmann on 19.10.2018.
 */
 class JenkinsConfiguration implements Serializable {
  /**
   * Method for creating a global credential object in the jenkins context.
   * <p>
   * @param id
   *    uniqe id for references in Jenkins
   * @param desc
   *    description for the credentials object.
   * @param username
   *    username of the credentials object.
   * @param password
   *    password of the credentials object.
   */
  public UsernamePasswordCredentialsImpl createCredatialObjectUsernamePassword(String id, String desc, String username, String password) {
    // create credential object
    def credObj = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, desc, username, password)
    Credentials c = (Credentials) credObj
    println "Add credentials " + id + " in global store"
    // store global credential object
    SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
    return credObj
  }
 
  public deleteCredentialObject(String id) {
    println "Deleting credential " + id + " in global store"
    // TODO: add implementation   def deleteCredentials = CredentialsMatchers.withId(credentialsId)
  }

  /**
  * Method for adding a custom tool the Jenkins installation.
  * <p>
  * @param toolName
  *    uniqe name for the tool to be added
  * @param commandLabel
  *    label used to reference the install command
  * @param homeDir
  *    Home directory for the tool to be installed.
  * @param filePath
  *    file path where the installation script should be made available.
  * @param exportedPaths
  *    Exported paths for the tool to be installed.
  * @param toolHome
  *    Home directory .
  * @param additionalVariables
  *    Additional variables if available.
  */
  public Boolean addCustomTool(String toolName, String commandLabel, String homeDir, String filePath, String exportedPaths, String toolHome, String additionalVariables){

     def jenkinsExtensionList  = Jenkins.getInstance().getExtensionList(com.cloudbees.jenkins.plugins.customtools.CustomTool.DescriptorImpl.class)[0]

     def installs = jenkinsExtensionList.getInstallations()
     def found = installs.find {
       it.name == toolName
     }

     if ( found ) {
       println toolName + " is already installed. Nothing to do."
       return false
       } else {
         println "installing " + toolName + " tool"

         List installers = new ArrayList();

         // read the file content from
         File file = new File(filePath)

          installers.add(new CommandInstaller(commandLabel, file.text, toolHome))
          List<ToolProperty> properties = new ArrayList<ToolProperty>()
          properties.add(new InstallSourceProperty(installers))

         def newI = new CustomTool(toolName, homeDir, properties, exportedPaths, null, ToolVersionConfig.DEFAULT, additionalVariables)
         installs += newI

         jenkinsExtensionList.setInstallations( (com.cloudbees.jenkins.plugins.customtools.CustomTool[])installs );

         jenkinsExtensionList.save()

         return true;
       }
     }

  /**
   * Method for installing a jenkins plugin
   * <p>
   *    Installs the given list of Jenkins plugins if not installed already. Before installing a plugin, the UpdateCenter is updated.
   * @param pluginsToinstall
   *    list of plugins to install
   * @return
   *    Boolean value which reflects wether a plugin was installed and a restart is required
   */
  public Boolean installPlugin( pluginsToInstall ) {
    def newPluginInstalled = false
    def initialized = false
    def instance = Jenkins.getInstance()

    def pm = instance.getPluginManager()
    def uc = instance.getUpdateCenter()

    pluginsToInstall.each {
      def String pluginName = it
      // Check if the plugin is already installed.
      if (!pm.getPlugin(pluginName)) {
        println "Plugin not installed yet - Searching '$pluginName' in the update center."
        // Check for updates.
        if (!initialized) {
          uc.updateAllSites()
          initialized = true
        }

        def plugin = uc.getPlugin(pluginName)
        if (plugin) {
          println "Installing '$pluginName' Jenkins Plugin ..."
          def installFuture = plugin.deploy()
          while(!installFuture.isDone()) {
            sleep(3000)
          }
          newPluginInstalled = true
          println "... Plugin has been installed"
        } else {
          println "Could not find the '$pluginName' Jenkins Plugin."
        }
      } else {
        println "The '$pluginName' Jenkins Plugin is already installed."
      }
    }
    return newPluginInstalled
  }

  /**
   * Method for restarting jenkins
   * <p>
   *    perform a restart of the jenkins instance. This is necessary when for e.g. a new plugin is installed. The restart should allway be performed at the end of the configuration.
   * @param safeRestart
   *    Optional Boolean parameter stating if the restart should be safe (default: false)
   */
  public restartJenkins( safeRestart ) {
    def instance = Jenkins.getInstance()
    if ( safeRestart ) {
      instance.safeRestart()
    } else {
      instance.restart()
    }
  }

  //Restart jenkins instantely
  public restartJenkins() {
    restartJenkins( false )
  }
}
