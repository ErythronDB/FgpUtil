package org.gusdb.fgputil.server;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.IoUtil.closeQuietly;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.Wrapper;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * General use Jersey-based REST server that provides hooks for startup and
 * creation/closing of application-scoped resources, and graceful shutdown.
 * 
 * @author rdoherty
 */
public abstract class RESTServer {

  protected abstract ResourceConfig getResourceConfig();
  protected abstract ApplicationContext createApplicationContext(JSONObject config);

  private static ApplicationContext _applicationContext;

  private final int _port;
  private final JSONObject _config;

  public static ApplicationContext getApplicationContext() {
    return _applicationContext;
  }

  /**
   * Creates a REST server superclass with the passed args.  Args should be:
   * @param commandLineArgs
   */
  public RESTServer(String[] commandLineArgs) {
    // parse command line args into desired port and server config
    TwoTuple<Integer, JSONObject> parsedArgs = parseConfig(commandLineArgs);
    _port = parsedArgs.getFirst();
    _config = parsedArgs.getSecond();
  }

  public void start() {

    Wrapper<ApplicationContext> applicationContext = new Wrapper<>();
    Wrapper<HttpServer> server = new Wrapper<>();

    try {

      // create base URI for server
      URI baseUri = UriBuilder
          .fromUri("http://localhost/")
          .port(_port)
          .build();

      // create runtime environment (application-scoped data)
      applicationContext.set(createApplicationContext(_config));
      synchronized(RESTServer.class) {
        if (_applicationContext != null) {
          throw new RuntimeException("Only one RESTServer can be used per application.");
        }
        _applicationContext = applicationContext.get();
      }

      // build Grizzly server
      server.set(GrizzlyHttpServerFactory.createHttpServer(baseUri, getResourceConfig()));

      // add hook to shut down resources if JVM is asked to shut down
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        server.get().shutdownNow();
        closeQuietly(_applicationContext);
      }));

      // start the server
      server.get().start();

      System.out.println("Server started on port " + _port + ". Stop the application using CTRL+C");

      Thread.currentThread().join();
    }
    catch (Exception e) {
      System.out.println("An exception occurred and the server " +
          "must shut down." + NL + FormatUtil.getStackTrace(e));
      System.exit(3);
    }
    finally {
      if (server.hasObject()) server.get().shutdown();
      if (applicationContext.hasObject()) closeQuietly(applicationContext.get());
    }
  }

  private TwoTuple<Integer, JSONObject> parseConfig(String[] args) {
    if (args.length < 1 || args.length > 2 || !FormatUtil.isInteger(args[0])) {
      System.err.println("USAGE: fgpJava " + getClass().getName() + " <port> [<config-file>]");
      System.exit(1);
    }
    int port = Integer.parseInt(args[0]);
    JSONObject config = new JSONObject(args.length == 1 ? "{}" : readConfigFile(args[1]));
    return new TwoTuple<>(port, config);
  }

  private String readConfigFile(String fileName) {
    try (FileReader in = new FileReader(fileName)) {
      return IoUtil.readAllChars(in);
    }
    catch (IOException | JSONException e) {
      System.err.println("Error: Unable to read JSON config file " + fileName + ":" + NL + e.getMessage());
      System.exit(2);
      return null;
    }
  }
}