#!/usr/bin/env groovy
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import com.cloudbees.plugins.credentials.common.*
import hudson.util.Secret

/**
 * Method for creating a global credential object in the jenkins context.
 * 	<p>
 * @param id
 *    uniqe id for references in Jenkins
 * @param desc
 * 		description for the credentials object.
 * @param username
 * 		username of the credentials object.
 * @param password
 * 		password of the credentials object.
 */
def call(String id, String desc, String username, String password) {
  myJenkins = Jenkins.instance
  Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, desc, username, password)
  println "Add credentials " + id + " in global store"
  SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
}