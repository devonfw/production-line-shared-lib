package com.capgemini.productionline.configuration.jenkins


// The following imports are needed for the credential objects
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import com.cloudbees.plugins.credentials.common.*
import hudson.util.Secret
import hudson.tools.*
import org.jenkinsci.plugins.scriptsecurity.scripts.*

// The below packages are used by the method installing Allure configurations.
import ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstaller;
import ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstallation;

// below packages is used by the method addNodeJS_Version
import jenkins.model.*
import hudson.model.*
import jenkins.plugins.nodejs.tools.*

import com.cloudbees.jenkins.plugins.customtools.CustomTool
import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig

import hudson.util.Secret
import hudson.tools.CommandInstaller
import hudson.tools.InstallSourceProperty
import hudson.tools.ToolProperty
import jenkins.*
import hudson.*
import jenkins.model.Jenkins

import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

import hudson.plugins.sonar.*
import hudson.plugins.sonar.model.TriggersConfig


import hudson.tasks.Maven.MavenInstaller
import hudson.tasks.Maven.MavenInstallation

import com.cloudbees.jenkins.plugins.sshcredentials.impl.*

import org.jenkinsci.plugins.configfiles.maven.*
import org.jenkinsci.plugins.configfiles.maven.security.*
import org.jenkinsci.plugins.ansible.AnsibleInstallation
import hudson.tools.CommandInstaller

// The below packages are used by the method setting SSH Jenkins Agent

import hudson.model.Node.Mode
import hudson.slaves.*
import hudson.plugins.sshslaves.SSHLauncher
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry
import hudson.plugins.sshslaves.verifiers.*


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
    * <p>
    *  This method approves all scripts that are waiting for Approval
    * @return
    *    Boolean value which reflects wether the signature has been added or not
  */
  public boolean approvePendingScripts() {
    ScriptApproval sa = ScriptApproval.get();

    // approve scripts
    for (ScriptApproval.PendingScript pending : sa.getPendingScripts()) {
      sa.approveScript(pending.getHash());
      println "Approved Script: " + pending.script
    }
    return true;
  }

  /**
    * <p>
    *  This method approves all signatures that are waiting for Approval
    * @return
    *    Boolean value which reflects wether the signature has been added or not
  */
  public boolean approvePendingSignatures() {
    ScriptApproval sa = ScriptApproval.get();

    // approve scripts
    for (ScriptApproval.PendingSignature pending : sa.getPendingSignatures()) {
      sa.approveSignature(pending.signature);
      println "Approved Signature: " + pending.signature
    }
    return true;
  }

  /**
    * <p>
    *  This method approves a given Signature
    * @param signature
    *    The string representing the signature that needs to be approved.
    * @return
    *    Boolean value which reflects wether the signature has been added or not
  */
  public boolean approveSignature(String signature) {
    def  ScriptApproval sa = ScriptApproval.get();
    try {
    sa.approveSignature(signature);
    } catch (IOException e) {
    	println "Exception: " + e.getMessage()
    }
    sa.save();
    return true;
  }

  /**
    * <p>
    *  This method approves signatures stored in a given file.
    * @param filePath
    *    The string representing the file path where the signature are stored.
    * @return
    *    Boolean value which reflects wether the signature has been added or not
  */
  public boolean approveSignatureFromFile(String filePath) {
    def  ScriptApproval sa = ScriptApproval.get();
    File file = new File(filePath)
    try {
    sa.approveSignature(signature);
    } catch (IOException e) {
    	println "Exception: " + e.getMessage()
    }
    sa.save();
    return true;
  }

  /**
    * <p>
    *  This method add a new NodeJS version using the NodeJS plugin. .
    * @param @required installName
    *    Name that should be diplay to identity the installation
    * @param @required nodeJS_Version
    *    NodeJS Version that should be installed.
    * @param @optional npmPackages
    *    List of npm packages that should be used.
    * @param @optional home
    *    Home
    * @param @optional npmPackagesRefreshHours
    *
    * @return
    *    Boolean value which reflects wether the installation was successfull or not
  */
  public boolean addNodeJS_Version(String installName, String nodeJS_Version, String npmPackages="", String home="", long npmPackagesRefreshHours=100) {

    def inst = Jenkins.getInstance()

    def desc = inst.getDescriptor("jenkins.plugins.nodejs.tools.NodeJSInstallation")

    def installations = [];
    def install = true

    // Iteration over already exiting installation, they will be added to the installation list
    for (i in desc.getInstallations()) {
    	installations.push(i)

      if (i.name == installName) {
        install = false
      }
    }

    if (install) {
      try {
        def installer = new NodeJSInstaller(nodeJS_Version, npmPackages, npmPackagesRefreshHours)
        def installerProps = new InstallSourceProperty([installer])
        def installation = new NodeJSInstallation(installName, home, [installerProps])
        installations.push(installation)

        desc.setInstallations(installations.toArray(new NodeJSInstallation[0]))

        desc.save()
      } catch(Exception ex) {
        println("Installation error  ");
        return false;
      }
    }
    
    return true
  }

  /**
    * <p>
    *  This method add a new configuration for the Allure Plugin...
    * @param @required mavenVersion
    *    Maven version that should be used to configure the plugin
    * @param @required commandLineInstallerName
    *    Name that should be used to identify the new configuration
    * @param @optional home
    *    Home
    * @return
    *    Boolean value which reflects wether the installation was successfull or not
  */
  public boolean addAllurePluginConfig(String mavenVersion, String commandLineInstallerName, String home="") {

    def inst = Jenkins.getInstance()

    def desc = inst.getDescriptor("ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstallation")

    def installations = [];

    // Iteration over already exiting installation, they will be added to the installation list
    for (i in desc.getInstallations()) {
    	installations.push(i)
    }

    try {
    def installer = new AllureCommandlineInstaller(mavenVersion)
    def installerProps = new InstallSourceProperty([installer])
    def installation = new AllureCommandlineInstallation(commandLineInstallerName, home, [installerProps])
    installations.push(installation)

    desc.setInstallations(installations.toArray(new AllureCommandlineInstallation[0]))

    desc.save()
    } catch(Exception ex) {
         println("Installation error  ");
         return false;
    }
    return true
  }

  
 /**
    * <p>
    *  This method add a new Maven config to the jenkins Configuration .
    * @param @required mavenName
    *    String used to identify the new Maven config.
    * @param @required MavenVersion
    *    Maven Version.
    *
    * @return
    *    Boolean value which reflects wether the installation was successfull or not
  */  
  
  public boolean addMavenPluginConfig(String mavenVersion, String mavenName) {

  println("Checking Maven installations...")
 
// Grab the Maven "task" (which is the plugin handle).
def mavenPlugin = Jenkins.instance.getExtensionList(hudson.tasks.Maven.DescriptorImpl.class)[0]
 
// Check for a matching installation.
def maven3Install = mavenPlugin.installations.find {
    install -> install.name.equals(mavenName)
}
 
// If no match was found, add an installation.
if(maven3Install == null) {
    println("No Maven install found. Adding...")
 
  def newMavenInstall = new hudson.tasks.Maven.MavenInstallation(mavenName, null,
        [new hudson.tools.InstallSourceProperty([new hudson.tasks.Maven.MavenInstaller(mavenVersion)])]
    )
 
    mavenPlugin.installations += newMavenInstall
    mavenPlugin.save()
 
    println("Maven install added.")
} else {
    println("Maven install found. Done.")
}
  
}
  
  /**
    * <p>
    *  This method add a new Sonarqube server to the jenkins Configuration .
    * @param @required sonar_name
    *    String used to identify the new server that should be added.
    * @param @required sonar_server_url
    *    URL of the Sonarqube server.
    * @param @required sonar_auth_token
    *    Token used to communicate with the sonarqube server.
    * @param @optional sonar_mojo_version
    *    sonar_mojo_version
    * @param @optional sonar_additional_properties
    * @param @optional sonar_triggers
    * @param @optional sonar_additional_analysis_properties
    *
    * @return
    *    Boolean value which reflects wether the installation was successfull or not
  */

  public boolean addSonarqubeServer(String sonar_name, String sonar_server_url, String sonar_auth_token, String sonar_mojo_version="", String sonar_additional_properties="", sonar_triggers = new TriggersConfig(), String sonar_additional_analysis_properties="") {
    def instance = Jenkins.getInstance()

    def SonarGlobalConfiguration sonar_conf = instance.getDescriptor(SonarGlobalConfiguration)

    def sonar_inst = new SonarInstallation(
        sonar_name,
        sonar_server_url,
        sonar_auth_token,
        sonar_mojo_version,
        sonar_additional_properties,
        sonar_triggers,
        sonar_additional_analysis_properties
    )

    // Get the list of all sonarQube global configurations.
    def sonar_installations = sonar_conf.getInstallations()

    def sonar_inst_exists = false

    // Check if our installation is already present in the configuration
    sonar_installations.each {
        installation = (SonarInstallation) it
        if (sonar_inst.getName() == installation.getName()) {
            sonar_inst_exists = true
            println("Found existing installation: " + installation.getName())
            return false;
        }
    }

    // Add the new server to the server list when not exsiting.
    if (!sonar_inst_exists) {
        sonar_installations += sonar_inst
        sonar_conf.setInstallations((SonarInstallation[]) sonar_installations)
        try {
          sonar_conf.save()
        } catch(Exception ex) {
             println("Installation error  ");
             return false;
        }

      return true;
    }
  }
  
  /**
    * <p>
    *  This method add a new configuration for the Ansible Plugin.
    *  Example usage: addAnsibleInstallator("if [ ! `which ansible` ]\nthen\nsudo apt-get update\nsudo apt-get install
    *  ansible -y\nelse echo 'Ansible is already installed'\nfi", "ITaaS Ansible", "/usr/bin/")
    * @param @required commands
    *    Bash commands or script which should be used to configure the plugin
    * @param @required commandLineInstallerName
    *    Name that should be used to identify the new configuration
    * @param @optional home
    *    Tool home path
    * @return
    *    Boolean value which reflects wether the installation was successfull or not
  */
  public boolean addAnsibleInstallator(String commands, String commandLineInstallerName, String home="") {

    def inst = Jenkins.getInstance()

    def desc = inst.getDescriptor("org.jenkinsci.plugins.ansible.AnsibleInstallation")

    List installers = new ArrayList();
    List<ToolProperty> properties = new ArrayList<ToolProperty>()
        
    def installations = [];
    // Iteration over already exiting installation, they will be added to the installation list
    for (i in desc.getInstallations()) {
    	installations.push(i)
    }
    println("All existing ansible installators have been loaded.");
    try {
      installers.add(new CommandInstaller("", commands, home))  
      properties.add(new InstallSourceProperty(installers))
      def installation = new AnsibleInstallation(commandLineInstallerName, "", properties)

      installations.push(installation)
      desc.setInstallations(installations.toArray(new org.jenkinsci.plugins.ansible.AnsibleInstallation[0]))

      desc.save()
    } catch(Exception ex) {
         println("Error during ansible installtion. Exception: ${ex}");
         return false;
    }
    return true
  }

  /**
   * <p>
   *    The method adds a server credential to the Jenkins credential store
   * @param username
   *    Username to be added
   * @param artifactoryPassword
   *    password. Should be either the password or a path to the file containing the password. The parameter readFromFile described below should be properly set.
   * @param credentialID
   *    ID referencing the credential
   * @param description
   *    Credential description
   * @param readFromFile
   *    This flag indicates wether the password should be read from a file or is directly given as a parameter.
   *    true => The password should be read from a file. In this case the parameter artifactoryPassword is the path to the file, should be made accessible from the script
   *    false => the password should not be read from a file and the parameter artifactoryPassword is the password that will be used.
   */
  def boolean addServerCredentialsToStore(String username, String artifactoryPassword, String credentialID, String description, boolean readFromFile=false) {
    approveSignature("staticMethod com.cloudbees.plugins.credentials.domains.Domain")
    def domain = Domain.global()
    def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

    if (readFromFile) {
      artifactoryPassword = new File(artifactoryPassword).text.trim()
    }

    def user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialID, description, username, artifactoryPassword)

    return store.addCredentials(domain, user)
  }


  /**
   * <p>
   *    The method adds a server credential to the Maven server Configuration
   * @param configID
   *    ID referencing the Maven Congiguration file
   * @param serverID
   *    ID referencing the server that should be added to the Server list.
   * @param credentialID
   *    ID referencing the credential
   */
  def boolean addServerCredentialToMavenConfig(String configID="MavenSettings", String serverID, String credentialID) {
    def configStore = Jenkins.instance.getExtensionList('org.jenkinsci.plugins.configfiles.GlobalConfigFiles')[0]

    def cfg = configStore.getById("MavenSettings")

    if (checkCredentialInStore(credentialID)) {
      serverCredentialMapping = new ServerCredentialMapping(serverID, credentialID)
      cfg.serverCredentialMappings.add(serverCredentialMapping)
      return configStore.save(cfg)
    }
    else {
      return false;
    }
  }
  
  /**
    * <p>
    *   The method adds a SSH credential to the Jenkins credential store
    *   Example usage: addJenkinsSshCredentials('Jenkins Node SSH Key', 'IDCredential', '', 'ubuntu', sh(returnStdout: true, script: 'cat ssh_key'))
    * @param userName
    *    Username used to connect through the SSH
    * @param secret
    *    Secret used inside SSH connection
    * @param credentialID
    *    ID referencing the SSH credential
    * @param description
    *    Credential description
    * @param SSH_KEY
    *    Provided SSH key
  */
  public boolean addJenkinsSshCredentials(String description, String credentialID, String secret, String userName, String SSH_KEY) {
    approveSignature("staticMethod com.cloudbees.plugins.credentials.domains.Domain")

    def jenkinsMasterKeyParameters = [
      description:  description,
      id:           credentialID,
      secret:       secret,
      userName:     userName,
      key:          new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(SSH_KEY)
    ]

    //def jenkins = Jenkins.getInstance()
    def domain = Domain.global()
    def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

    // Define private key
    def privateKey = new BasicSSHUserPrivateKey(
      CredentialsScope.GLOBAL,
      jenkinsMasterKeyParameters.id,
      jenkinsMasterKeyParameters.userName,
      jenkinsMasterKeyParameters.key,
      jenkinsMasterKeyParameters.secret,
      jenkinsMasterKeyParameters.description
    )
    try {
      store.addCredentials(domain, privateKey)
    } catch(Exception ex) {
         println("Error during SSH credential adding. Exception: ${ex}");
         return false;
    }
    return true
  }

  /**
   * <p>
   *    The method checks if a Credential Object with the given ID exists in the Jenkins System Credential Store
   * @param credentialsID
   *    ID that we want to check the exsitance in the system crendential store
   */
  def checkCredentialInStore(String credentialsID) {
    approveSignature("staticMethod com.cloudbees.plugins.credentials.domains.Domain")
    def domain = Domain.global()

    def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

    value = store.getCredentials(domain).find {credential -> credential.getId() == credentialsID}
    return value != null;
  }
  
  /**
    * <p>
    *    The method adds a SSH credential to the Jenkins credential store
    * @param agentName
    *    Jenkins agent name
    * @param agentIP
    *    External Jenkins agent IP
    * @param credentialID
    *    ID referencing the credential
    * @param agentDescription
    *    Jenkins agent description
    * @param agentHome
    *    Jenkins agent home
    * @param agentExecutors
    *    Jenkins agent executors number
    * @param agentLabels
    *    Jenkins agent labels
    * @param sshPort
    *    Jenkins agent SSH port
    * @param jvmOptions
    *    Options passed to the java vm.
    * @param javaPath
    *    Path to the host jdk installation. If null the jdk will be auto detected.
    * @param prefixStartSlaveCmd
    *    This will prefix the start agent command. For instance if you want to execute the command with a different shell.
    * @param suffixStartSlaveCmd
    *    This will suffix the start agent command.
    * @param launchTimeoutSeconds
    *    Launch timeout in seconds
    * @param maxNumRetries
    *    The number of times to retry connection if the SSH connection is refused during initial connect
    * @param retryWaitTime
    *    The number of seconds to wait between retries
  */
  public boolean addJenkinsNode(String credentialID, String agentName, String agentIP, String agentDescription, String agentHome, String agentExecutors, String agentLabels, int sshPort,  String jvmOptions, String javaPath, String prefixStartSlaveCmd, String suffixStartSlaveCmd, Integer launchTimeoutSeconds, Integer maxNumRetries, Integer retryWaitTime) {

    SshHostKeyVerificationStrategy hostKeyVerificationStrategy = new NonVerifyingKeyVerificationStrategy()
    DumbSlave dumb = new DumbSlave(agentName,
            agentDescription,
            agentHome,
            agentExecutors,
            Mode.EXCLUSIVE,
            agentLabels,
            new SSHLauncher(agentIP, sshPort, credentialID, jvmOptions, javaPath, prefixStartSlaveCmd, suffixStartSlaveCmd, launchTimeoutSeconds, maxNumRetries, retryWaitTime, hostKeyVerificationStrategy),
            RetentionStrategy.INSTANCE)
    try {
      Jenkins.instance.addNode(dumb)
    } catch(Exception ex) {
         println("Error during Jenkins node creation. Exception: ${ex}");
         return false;
    }
    return true
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

  //JOBDSl cannot run automatically if security is enabled!
  public disableJobDSLScriptSecurity(){

    Jenkins j = Jenkins.instance

    if(!j.isQuietingDown()) {
        def job_dsl_security = j.getExtensionList('javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration')[0]

        if(job_dsl_security.useScriptSecurity) {
          job_dsl_security.useScriptSecurity = false
          println 'Job DSL script security has changed.  It is now disabled.'
          job_dsl_security.save()
          j.save()
        }
        else {
          println 'Nothing changed.  Job DSL script security already disabled.'
        }
    }
    else {
      println 'Shutdown mode enabled.  Configure Job DSL script security SKIPPED.'
    }
  }

  //JOBDSl cannot run automatically if security is enabled!
  public enableJobDSLScriptSecurity(){

    Jenkins j = Jenkins.instance

    if(!j.isQuietingDown()) {
        def job_dsl_security = j.getExtensionList('javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration')[0]

        if(!job_dsl_security.useScriptSecurity) {
          job_dsl_security.useScriptSecurity = true
          println 'Job DSL script security has changed.  It is now enabled.'
          job_dsl_security.save()
          j.save()
        }
        else {
          println 'Nothing changed.  Job DSL script security already enabled.'
        }
    }
    else {
      println 'Shutdown mode enabled.  Configure Job DSL script security SKIPPED.'
    }
  }
}
