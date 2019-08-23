#!/usr/bin/groovy
package com.capgemini.productionline.configuration;

import groovy.json.JsonSlurper
import org.junit.Test

class SonarQubeTest {

  @Test
  public void testSomething() {
    Object context = new Expando()
    context.httpRequest = { Map parameters -> 
      def response = new Expando()
      response.getContent = { -> '{"token":"magic-token-value"}' }
      return response
    }
    context.readJSON = { Map parameters -> new JsonSlurper().parseText(parameters.text as String)}
    SonarQube sq = new SonarQube(context, "username", "http://localhost:9000/");
    assert sq.username == "username"
    assert sq.sonarQubeBaseUrl == "http://localhost:9000/"
    assert sq.context == context

    assert sq.getAuthToken('dummy-user') == 'magic-token-value'
  }
}