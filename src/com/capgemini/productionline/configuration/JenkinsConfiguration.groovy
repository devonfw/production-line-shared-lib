package com.capgemini.productionline.configuration

import com.cloudbees.jenkins.plugins.customtools.CustomTool
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.ToolVersionConfig
import hudson.EnvVars
import hudson.model.JDK
import hudson.model.Node.Mode
import hudson.plugins.sonar.SonarGlobalConfiguration
import hudson.plugins.sonar.SonarInstallation
import hudson.plugins.sonar.model.TriggersConfig
import hudson.plugins.sshslaves.SSHLauncher
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy
import hudson.plugins.sshslaves.verifiers.SshHostKeyVerificationStrategy
import hudson.slaves.DumbSlave
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.tasks.Maven
import hudson.tools.CommandInstaller
import hudson.tools.InstallSourceProperty
import hudson.tools.ToolProperty
import hudson.tools.ZipExtractionInstaller
import hudson.util.Secret
import jenkins.model.Jenkins
import jenkins.plugins.nodejs.tools.NodeJSInstallation
import jenkins.plugins.nodejs.tools.NodeJSInstaller
import org.jenkinsci.plugins.ansible.AnsibleInstallation
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval
import ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstallation
import ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstaller

/**
 * Contains the configuration methods of the jenkins component
 * <p>
 *     The main purpose collecting configuration methods.
 *
 * Created by tlohmann on 19.10.2018.
 */
class JenkinsConfiguration implements Serializable {
    def context

    JenkinsConfiguration(context) {
        this.context = context
    }

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
        context.println "Add credentials " + id + " in global store"
        // store global credential object
        SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
        return credObj
    }

    public StringCredentialsImpl createCredatialObjectSecretString(String id, String desc, String credential) {
        // create credential object
        try {
            Credentials credObj = new StringCredentialsImpl(CredentialsScope.GLOBAL, id, desc, Secret.fromString(credential))
            context.println "Add credentials " + id + " in global store"
            // store global credential object
            SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), credObj)
            return credObj
        } catch (e) {
            this.context.println('Error creating credential: ' + e)
        }

        return null
    }

    public deleteCredentialObject(String id) {
        context.println "Deleting credential " + id + " in global store"
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
    public Boolean addCustomTool(String toolName, String commandLabel, String homeDir, String filePath, String exportedPaths, String toolHome, String additionalVariables) {

        def jenkinsExtensionList = Jenkins.get().getExtensionList(CustomTool.DescriptorImpl.class)[0]

        def installs = jenkinsExtensionList.getInstallations()
        def found = installs.find {
            it.name == toolName
        }

        if (found) {
            context.println toolName + " is already installed. Nothing to do."
            return false
        } else {
            context.println "installing " + toolName + " tool"

            List installers = new ArrayList()

            // read the file content from
            File file = new File(filePath)

            installers.add(new CommandInstaller(commandLabel, file.text, toolHome))
            List<ToolProperty> properties = new ArrayList<ToolProperty>()
            properties.add(new InstallSourceProperty(installers))

            def newI = new CustomTool(toolName, homeDir, properties, exportedPaths, null, ToolVersionConfig.DEFAULT, additionalVariables)
            installs += newI

            jenkinsExtensionList.setInstallations((CustomTool[]) installs)

            jenkinsExtensionList.save()

            return true
        }
    }

    /**
     * Method for adding a custom tool the Jenkins installation.
     * <p>
     * @param toolName
     *    uniqe name for the tool to be added
     * @param commandLabel
     *    label used to reference the install command
     * @param batchString
     *    Script to install the tool
     * @param homeDir
     *    Home directory for the tool to be installed.
     */
    public Boolean addCustomTool(String toolName, String commandLabel, String batchString, String homeDir) {

        def jenkinsExtensionList = Jenkins.get().getExtensionList(CustomTool.DescriptorImpl.class)[0]

        def installs = jenkinsExtensionList.getInstallations()
        def found = installs.find {
            it.name == toolName
        }

        if (found) {
            context.println toolName + " is already installed. Nothing to do."
            return false
        } else {
            context.println "installing " + toolName + " tool"

            List installers = new ArrayList()


            installers.add(new CommandInstaller(commandLabel, batchString, homeDir))
            List<ToolProperty> properties = new ArrayList<ToolProperty>()
            properties.add(new InstallSourceProperty(installers))

            def newI = new CustomTool(toolName, "", properties, "", null, ToolVersionConfig.DEFAULT, "")
            installs += newI

            jenkinsExtensionList.setInstallations((CustomTool[]) installs)

            jenkinsExtensionList.save()

            return true
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
    public Boolean installPlugin(pluginsToInstall) {
        def newPluginInstalled = false
        def initialized = false
        def instance = Jenkins.get()

        def pm = instance.getPluginManager()
        def uc = instance.getUpdateCenter()

        pluginsToInstall.each {
            String pluginName = it
            // Check if the plugin is already installed.
            if (!pm.getPlugin(pluginName)) {
                context.println "Plugin not installed yet - Searching '$pluginName' in the update center."
                // Check for updates.
                if (!initialized) {
                    uc.updateAllSites()
                    initialized = true
                }

                def plugin = uc.getPlugin(pluginName)
                if (plugin) {
                    context.println "Installing '$pluginName' Jenkins Plugin ..."
                    def installFuture = plugin.deploy()
                    while (!installFuture.isDone()) {
                        sleep(3000)
                    }
                    newPluginInstalled = true
                    context.println "... Plugin has been installed"
                } else {
                    context.println "Could not find the '$pluginName' Jenkins Plugin."
                }
            } else {
                context.println "The '$pluginName' Jenkins Plugin is already installed."
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
        ScriptApproval sa = ScriptApproval.get()

        // approve scripts
        for (ScriptApproval.PendingScript pending : sa.getPendingScripts()) {
            sa.approveScript(pending.getHash())
            context.println "Approved Script: " + pending.script
        }
        return true
    }

    /**
     * <p>
     *  This method approves all signatures that are waiting for Approval
     * @return
     *    Boolean value which reflects wether the signature has been added or not
     */
    public boolean approvePendingSignatures() {
        ScriptApproval sa = ScriptApproval.get()

        // approve scripts
        for (ScriptApproval.PendingSignature pending : sa.getPendingSignatures()) {
            sa.approveSignature(pending.signature)
            context.println "Approved Signature: " + pending.signature
        }
        return true
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
        ScriptApproval sa = ScriptApproval.get()
        try {
            sa.approveSignature(signature)
        } catch (IOException e) {
            println "Exception: " + e.getMessage()
        }
        sa.save()
        return true
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
        ScriptApproval sa = ScriptApproval.get()
        File file = new File(filePath)
        try {
            sa.approveSignature(file.text.trim())
        } catch (IOException e) {
            println "Exception: " + e.getMessage()
        }
        sa.save()
        return true
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
    public boolean addNodeJS_Version(String installName, String nodeJS_Version, String npmPackages = "", String home = "", long npmPackagesRefreshHours = 100) {

        def inst = Jenkins.get()

        def desc = inst.getDescriptor("jenkins.plugins.nodejs.tools.NodeJSInstallation")

        def installations = []
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
            } catch (Exception ex) {
                context.println("Installation error  " + ex.getMessage())
                return false
            }
        }

        return true
    }

    /**
     * <p>
     *  This method add a new JDK version using the JDK plugin. .
     * @param @required installName
     *    Name that should be diplay to identity the installation
     * @param @required downloadUrl
     *    JDK download url.
     * @param @required jdkFolder
     *    Folder where jdk is extracted.
     *
     * @return
     *    Boolean value which reflects wether the installation was successfull or not
     */
    public boolean addJdkVersion(String installName, String downloadUrl, String jdkFolder) {

        def inst = Jenkins.get()

        def desc = inst.getDescriptor("hudson.model.JDK")

        def installations = []
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
                def installer = new ZipExtractionInstaller("", downloadUrl, jdkFolder)
                def installerProps = new InstallSourceProperty([installer])
                def installation = new JDK(installName, "", [installerProps])
                installations.push(installation)

                desc.setInstallations(installations.toArray(new JDK[0]))

                desc.save()
            } catch (Exception ex) {
                context.println("Installation error  " + ex.getMessage())
                return false
            }
        }

        return true
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
    public static boolean addAnsibleInstallator(String commands, String commandLineInstallerName, String home = "") {

        def inst = Jenkins.get()

        def desc = inst.getDescriptor("org.jenkinsci.plugins.ansible.AnsibleInstallation")

        List installers = new ArrayList()
        List<ToolProperty> properties = new ArrayList<ToolProperty>()

        def installations = []
        // Iteration over already exiting installation, they will be added to the installation list
        for (i in desc.getInstallations()) {
            installations.push(i)
        }
        println("All existing ansible installators have been loaded.")
        try {
            installers.add(new CommandInstaller("", commands, home))
            properties.add(new InstallSourceProperty(installers))
            def installation = new AnsibleInstallation(commandLineInstallerName, "", properties)

            installations.push(installation)
            desc.setInstallations(installations.toArray(new AnsibleInstallation[0]))

            desc.save()
        } catch (Exception ex) {
            println("Error during ansible installtion. Exception: ${ex}")
            return false
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
    public boolean addAllurePluginConfig(String mavenVersion, String commandLineInstallerName, String home = "") {

        def inst = Jenkins.get()

        def desc = inst.getDescriptor("ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstallation")

        def installations = []

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
        } catch (Exception ex) {
            context.println("Installation error  " + ex.getMessage())
            return false
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
    public void addMavenPluginConfig(String mavenVersion, String mavenName) {

        context.println("Checking Maven installations...")

        // Grab the Maven "task" (which is the plugin handle).
        def mavenPlugin = Jenkins.get().getExtensionList(Maven.DescriptorImpl.class)[0]

        // Check for a matching installation.
        def maven3Install = mavenPlugin.installations.find {
            install -> (install.name == mavenName)
        }

        // If no match was found, add an installation.
        if (maven3Install == null) {
            context.println("No Maven install found. Adding...")

            def newMavenInstall = new Maven.MavenInstallation(mavenName, null,
                    [new InstallSourceProperty([new Maven.MavenInstaller(mavenVersion)])]
            )

            mavenPlugin.installations += newMavenInstall
            mavenPlugin.save()

            context.println("Maven install added.")
        } else {
            context.println("Maven install found. Done.")
        }

    }

    /**
     * <p>
     *  This method add a new Sonarqube server to the jenkins Configuration .
     * @param @required sonar_name
     *    String used to identify the new server that should be added.
     * @param @required sonar_server_url
     *    URL of the Sonarqube server.
     * @param @required sonar_credentials_id
     *    Credentials id where the SonarQube token is stored
     * @param @optional sonar_auth_token
     *    SonarQube auth token. This is deprecated, send always to null
     * @param @optional sonar_mojo_version
     *    sonar_mojo_version
     * @param @optional sonar_additional_properties
     * @param @optional sonar_additional_analysis_properties
     * @param @optional sonar_triggers
     *
     * @return
     *    Boolean value which reflects wether the installation was successfull or not
     */
    public boolean addSonarqubeServer(String sonar_name, String sonar_server_url, String sonar_credentials_id, Secret sonar_auth_token = null, String sonar_webhook_id = null, String sonar_mojo_version = "", String
            sonar_additional_properties = "", String sonar_additional_analysis_properties = "", TriggersConfig sonar_triggers = new TriggersConfig()) {
        def instance = Jenkins.get()

        def sonar_conf = instance.getDescriptor(SonarGlobalConfiguration.class)

        def sonar_inst = new SonarInstallation(
                sonar_name,
                sonar_server_url,
                sonar_credentials_id,
                sonar_auth_token,
                sonar_webhook_id,
                sonar_mojo_version,
                sonar_additional_properties,
                sonar_additional_analysis_properties,
                sonar_triggers
        )

        // Get the list of all sonarQube global configurations.
        def sonar_installations = sonar_conf.getInstallations()

        def sonar_inst_exists = false

        // Check if our installation is already present in the configuration
        sonar_installations.each {
            installation = (SonarInstallation) it
            if (sonar_inst.getName() == installation.getName()) {
                sonar_inst_exists = true
                context.println("Found existing installation: " + installation.getName())
                return false
            }
        }

        // Add the new server to the server list when not exsiting.
        if (!sonar_inst_exists) {
            sonar_installations += sonar_inst
            sonar_conf.setInstallations((SonarInstallation[]) sonar_installations)
            try {
                sonar_conf.save()
            } catch (Exception ex) {
                context.println("Installation error  " + ex.getMessage())
                return false
            }

            return true
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
    public boolean addServerCredentialsToStore(String username, String artifactoryPassword, String credentialID, String description, boolean readFromFile = false) {
        approveSignature("staticMethod com.cloudbees.plugins.credentials.domains.Domain")
        def domain = Domain.global()
        def store = Jenkins.get().getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

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
    public boolean addServerCredentialToMavenConfig(String configID = "MavenSettings", String serverID, String credentialID) {
        GlobalConfigFiles configStore = Jenkins.get().getExtensionList(GlobalConfigFiles.class)[0]

        GlobalMavenSettingsConfig cfg = configStore.getById("MavenSettings") as GlobalMavenSettingsConfig

        if (checkCredentialInStore(credentialID)) {
            def serverCredentialMapping = new ServerCredentialMapping(serverID, credentialID)
            cfg.serverCredentialMappings.add(serverCredentialMapping)
            configStore.save(cfg)
            return true
        } else {
            return false
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
                description: description,
                id         : credentialID,
                secret     : secret,
                userName   : userName,
                key        : new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(SSH_KEY)
        ]

        //def jenkins = Jenkins.getInstance()
        def domain = Domain.global()
        def store = Jenkins.get().getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0]
                .getStore()

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
        } catch (Exception ex) {
            println("Error during SSH credential adding. Exception: ${ex}")
            return false
        }
        return true
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
    public boolean addJenkinsNode(String credentialID, String agentName, String agentIP, String agentDescription, String agentHome, String agentExecutors, String agentLabels, int sshPort, String jvmOptions, String javaPath, String prefixStartSlaveCmd, String suffixStartSlaveCmd, Integer launchTimeoutSeconds, Integer maxNumRetries, Integer retryWaitTime) {

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
            Jenkins.get().addNode(dumb)
        } catch (Exception ex) {
            println("Error during Jenkins node creation. Exception: ${ex}")
            return false
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

        def store = Jenkins.get().getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

        def value = store.getCredentials(domain).find { credential -> credential.getId() == credentialsID }
        return value != null
    }

    /**
     * Method for restarting jenkins
     * <p>
     *    perform a restart of the jenkins instance. This is necessary when for e.g. a new plugin is installed. The restart should allway be performed at the end of the configuration.
     * @param safeRestart
     *    Optional Boolean parameter stating if the restart should be safe (default: false)
     */
    public void restartJenkins(safeRestart) {
        def instance = Jenkins.get()
        if (safeRestart) {
            instance.safeRestart()
        } else {
            instance.restart()
        }
    }

    //Restart jenkins instantely
    public restartJenkins() {
        restartJenkins(false)
    }

    //JOBDSl cannot run automatically if security is enabled!
    public disableJobDSLScriptSecurity() {

        Jenkins j = Jenkins.get()

        if (!j.isQuietingDown()) {
            def job_dsl_security = j.getExtensionList('javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration')[0]

            if (job_dsl_security.useScriptSecurity) {
                job_dsl_security.useScriptSecurity = false
                context.println 'Job DSL script security has changed.  It is now disabled.'
                job_dsl_security.save()
                j.save()
            } else {
                context.println 'Nothing changed.  Job DSL script security already disabled.'
            }
        } else {
            context.println 'Shutdown mode enabled.  Configure Job DSL script security SKIPPED.'
        }
    }

    //JOBDSl cannot run automatically if security is enabled!
    public enableJobDSLScriptSecurity() {

        Jenkins j = Jenkins.get()

        if (!j.isQuietingDown()) {
            def job_dsl_security = j.getExtensionList('javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration')[0]

            if (!job_dsl_security.useScriptSecurity) {
                job_dsl_security.useScriptSecurity = true
                context.println 'Job DSL script security has changed.  It is now enabled.'
                job_dsl_security.save()
                j.save()
            } else {
                context.println 'Nothing changed.  Job DSL script security already enabled.'
            }
        } else {
            context.println 'Shutdown mode enabled.  Configure Job DSL script security SKIPPED.'
        }
    }

    /**
     * Add global environment variable to Jenkins -> Configuration
     * @param key the key of the environment variable
     * @param value the value of the envionment variable
     */
    public void addJenkinsGlobalEnvironmentVariable(String key, String value) {
        try {
            Jenkins instance = Jenkins.get()

            def globalNodeProperties = instance.getGlobalNodeProperties()
            List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class)

            EnvironmentVariablesNodeProperty newEnvVarsNodeProperty
            EnvVars envVars

            if (envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0) {
                newEnvVarsNodeProperty = new EnvironmentVariablesNodeProperty()
                globalNodeProperties.add(newEnvVarsNodeProperty)
                envVars = newEnvVarsNodeProperty.getEnvVars()
            } else {
                envVars = envVarsNodePropertyList.get(0).getEnvVars()
            }
            envVars.put(key, value)
            instance.save()
        } catch (e) {
            context.println e
        }
    }
}
