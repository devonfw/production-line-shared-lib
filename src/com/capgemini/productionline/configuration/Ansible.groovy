package com.capgemini.productionline.configuration

import hudson.tools.CommandInstaller
import hudson.tools.InstallSourceProperty
import hudson.tools.ToolProperty
import jenkins.model.Jenkins
import org.jenkinsci.plugins.ansible.AnsibleInstallation

class Ansible implements Serializable {

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
}
