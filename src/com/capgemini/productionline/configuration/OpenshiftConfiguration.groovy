package com.capgemini.productionline.configuration

import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.openshift.jenkins.plugins.ClusterConfig
import com.openshift.jenkins.plugins.OpenShift
import com.openshift.jenkins.plugins.OpenShiftClientTools
import com.openshift.jenkins.plugins.OpenShiftTokenCredentials

import hudson.tools.CommandInstaller
import hudson.tools.InstallSourceProperty
import hudson.util.Secret

import jenkins.model.Jenkins

class OpenshiftConfiguration implements Serializable {
    def context

    OpenshiftConfiguration(context) {
        this.context = context
    }

    public Boolean installOpenshift(String toolName, String commandLabel, String batchString, String homeDir) {

        def inst = Jenkins.get()

        def desc = inst.getDescriptor("com.openshift.jenkins.plugins.OpenShiftClientTools")

        def installations = []
        def install = true

        // Iteration over already exiting installation, they will be added to the installation list
        for (i in desc.getInstallations()) {
            installations.push(i)

            if (i.name == toolName) {
                install = false
            }
        }

        if (install) {
            try {

                def installer = new CommandInstaller(commandLabel, batchString, homeDir)

                def installerProps = new InstallSourceProperty([installer])
                def installation = new OpenShiftClientTools(toolName, "", [installerProps])
                installations.push(installation)

                desc.setInstallations(installations.toArray(new OpenShiftClientTools[0]))

                desc.save()
            } catch (Exception ex) {
                context.println ex
                context.println("Installation error  ")
                return false
            }
        }

        return true
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
    public OpenShiftTokenCredentials createCredatialObjectUsernamePassword(String id, String desc, String token) {

        OpenShiftTokenCredentials found = SystemCredentialsProvider.getInstance().getCredentials().find {
            if (it instanceof OpenShiftTokenCredentials) {
                it.getId() == id
            }
        } as OpenShiftTokenCredentials

        if(!found) {
            // create credential object
            def credObj = new OpenShiftTokenCredentials(CredentialsScope.GLOBAL, id, desc, Secret.fromString(token))
            context.println "Add credentials " + id + " in global store"
            // store global credential object
            SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), credObj)
            return credObj
        }

        return found
    }

    public void addOpenshiftGlobalConfiguration(String clusterName, String clusterUrl, String clusterCredential, String clusterProject) {
        OpenShift.DescriptorImpl openshiftDSL = (OpenShift.DescriptorImpl)Jenkins.get().getDescriptor("com.openshift.jenkins.plugins.OpenShift")

        def found = openshiftDSL.getClusterConfigs().find {
            it.getName() == clusterName
        }

        if (!found) {
            ClusterConfig cluster1 = new ClusterConfig(clusterName)
            cluster1.setServerUrl(clusterUrl)
            cluster1.setCredentialsId(clusterCredential)
            cluster1.setDefaultProject(clusterProject)
            cluster1.setSkipTlsVerify(true)

            openshiftDSL.addClusterConfig(cluster1)
            openshiftDSL.save()
        } else {
            context.println "Openshift configuration with name ${clusterName} already existes"
        }
    }
}
