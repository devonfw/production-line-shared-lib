package com.capgemini.productionline.build

class Maven {

  final File m2

  Maven() {
    this(System.getProperty("user.home"))
  }

  Maven(String home) {
    def homeDir = new File(home)
    this.m2 = new File(homeDir, ".m2")
  }

  protected String generatePassword(int length) {
    String alphabet = (('A'..'Z')+('a'..'z')+('0'..'9')).join() 
    def key = new Random().with {
      (1..length).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
    }
    return key
  }

  public String encryptMasterPassword(String password) {
    def command = 'mvn --encrypt-master-password ' + password
    def process = command.execute()
    def encryptedPassword = process.text
    if (process.exitValue()) {
      println "maven --encrypt-master-password gave the following error: "
      println "[ERROR] ${process.getErrorStream()}"
      return null
    }
    if (encryptedPassword.endsWith('\n')) {
      return encryptedPassword - '\n'
    }
    return encryptedPassword
  }

  public void createSettingsSecurity() {
    def settingsSecurity = new File(this.m2, "settings-security.xml")
    if (settingsSecurity.exists()) {
      return
    }
    m2.mkdirs()
    def password = generatePassword(20)
    def encryptedPassword = encryptMasterPassword(password)
    def xml = '<?xml version="1.0" encoding="UTF-8"?>\n' + '<settingsSecurity>\n' + '  <master>' + encryptedPassword + '</master>\n' + '</settingsSecurity>\n'
    settingsSecurity.write xml
  }
}