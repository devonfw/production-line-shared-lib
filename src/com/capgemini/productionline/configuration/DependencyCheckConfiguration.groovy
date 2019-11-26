package com.capgemini.productionline.configuration

import hudson.tools.InstallSourceProperty
import org.jenkinsci.plugins.DependencyCheck.tools.DependencyCheckInstallation
import org.jenkinsci.plugins.DependencyCheck.tools.DependencyCheckInstaller

class DependencyCheckConfiguration implements Serializable {
    def context

    DependencyCheckConfiguration(context) {
        this.context = context
    }

    def installDependencyCheck(String toolName, String version, String toolHome) {
        def inst = Jenkins.get()

        def desc = inst.getDescriptor(DependencyCheckInstallation.class)

        def installations = []
        def install = true

        // Iteration over already exiting installation, they will be added to the installation list
        for (i in desc.getInstallations()) {
            installations.push(i)
            this.context.println i

            if (i.name == toolName) {
                install = false
            }
        }

        if (install) {
            try {
                def installer = new DependencyCheckInstaller(version)
                def installerProps = new InstallSourceProperty([installer])
                def installation = new DependencyCheckInstallation(toolName, toolHome, [installerProps])
                installations.push(installation)

                desc.setInstallations(installations.toArray(new DependencyCheckInstallation[0]))

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
