package com.capgemini.productionline.configuration

import hudson.tools.InstallSourceProperty

import jenkins.model.Jenkins

import org.jenkinsci.plugins.docker.commons.tools.DockerTool
import org.jenkinsci.plugins.docker.commons.tools.DockerToolInstaller

class DockerConfiguration implements  Serializable {
    def context

    DockerConfiguration(context) {
        this.context = context
    }

    public Boolean installDocker(String toolName, String label, String version, String toolHome) {
        def inst = Jenkins.get()

        def desc = inst.getDescriptor("org.jenkinsci.plugins.docker.commons.tools.DockerTool")

        def installations = []
        def install = true

        // Iteration over already exiting installation, they will be added to the installation list
        for (i in desc.getInstallations()) {
            installations.push(i)
            step.println i

            if (i.name == toolName) {
                install = false
            }
        }

        if (install) {
            try {
                def installer = new DockerToolInstaller(label, version)
                def installerProps = new InstallSourceProperty([installer])
                def installation = new DockerTool(toolName, toolHome, [installerProps])
                installations.push(installation)

                desc.setInstallations(installations.toArray(new DockerTool[0]))

                desc.save()
            } catch (Exception ex) {
                context.println ex
                context.println("Installation error  ")
                return false
            }
        }

        return true
    }
}
