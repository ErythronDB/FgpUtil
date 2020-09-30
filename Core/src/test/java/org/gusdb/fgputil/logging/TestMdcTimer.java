package org.gusdb.fgputil.logging;

import java.io.File;
import java.io.FileInputStream;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.gusdb.fgputil.logging.MDCUtil;
import org.junit.Test;


public class TestMdcTimer {

  @Test
  public void testMdcTimer() throws Exception {

    // initialize configuration
    File log4jConfigFile = new File("src/test/resources/log4j.json");
    ConfigurationSource source = new ConfigurationSource(new FileInputStream(log4jConfigFile), log4jConfigFile);
    Configurator.initialize(null, source);

    // set MDC vars to emulate request
    MDCUtil.setRequestStartTime(System.currentTimeMillis());
    MDCUtil.setIpAddress("123.456.789.000");
    MDCUtil.setRequestedDomain("plasmodb.org");
    MDCUtil.setRequestId("1");
    MDCUtil.setSessionId("abcdefghijklmnopqrstuvwxyz");

    Logger log = Logger.getLogger(TestMdcTimer.class);
    log.info("Test 1");
    Thread.sleep(1234);
    log.info("Test 2");
  }
}