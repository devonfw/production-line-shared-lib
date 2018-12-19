#!/usr/bin/groovy
package com.capgemini.productionline.build;

import org.junit.Test
import java.nio.file.Files

class MavenTest {

  @Test
  public void testCreateSettingsSecurity() {
    def home = Files.createTempDirectory("pl-maven").toString()
    Maven maven = new Maven(home)
    assert !maven.m2.exists()

    maven.createSettingsSecurity()

    assert maven.m2.isDirectory()
    def settingsSecurity = new File(maven.m2, "settings-security.xml")
    assert settingsSecurity.isFile()
    def xml = new XmlSlurper().parse(settingsSecurity)
    def encryptedPassword = xml.master.text()
    assert encryptedPassword.length() == 66
    assert encryptedPassword ==~ /\{[^}]{64}\}/
    maven.m2.deleteDir()
  }
}