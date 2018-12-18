#!/usr/bin/groovy
package com.capgemini.productionline.configuration;

import org.junit.Test

class SonarQubeTest {

  @Test
  public void testSomething() {
    Object context = null
    SonarQube sq = new SonarQube(context, "username", "http://localhost:9000/");
    assert sq.username == "username"
    assert sq.sonarQubeBaseUrl == "http://localhost:9000/"
    assert sq.context == context
  }
}