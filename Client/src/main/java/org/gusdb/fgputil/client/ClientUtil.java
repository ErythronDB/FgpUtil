package org.gusdb.fgputil.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.web.HttpMethod;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientUtil {

  private static final Logger LOG = Logger.getLogger(ClientUtil.class);

  public static boolean LOG_RESPONSE_HEADERS = false;

  public static <T> T getResponseObject(String url, Class<T> responseObjectClass) throws IOException {
    return new ObjectMapper().readerFor(responseObjectClass).readValue(new URL(url));
  }

  public static <T> T getResponseObject(String url, Class<T> responseObjectClass, Map<String,String> headers) throws Exception {
    return new ObjectMapper().readerFor(responseObjectClass).readValue(
      makeAsyncGetRequest(url, MediaType.APPLICATION_JSON, headers).getInputStream()
    );
  }

  public static ResponseFuture makeAsyncGetRequest(
      String url, String expectedResponseType) {
    return makeAsyncGetRequest(url, expectedResponseType, Collections.emptyMap());
  }

  public static ResponseFuture makeAsyncPostRequest(
      String url, String requestBody, String requestMimeType, String expectedResponseType) {
    return makeAsyncPostRequest(url, requestBody, requestMimeType, expectedResponseType, Collections.emptyMap());
  }

  public static ResponseFuture makeAsyncPostRequest(
      String url, Object postBodyObject, String expectedResponseType) {
    return makeAsyncPostRequest(url, postBodyObject, expectedResponseType, Collections.emptyMap());
  }

  public static ResponseFuture makeAsyncGetRequest(
      String url, String expectedResponseType, Map<String,String> additionalHeaders) {
    LOG.info("Will send following GET request to " + url);
    return makeAsyncRequest(url, expectedResponseType,
      invoker -> invoker.get(), additionalHeaders);
  }

  public static ResponseFuture makeAsyncPostRequest(
      String url, String requestBody, String requestMimeType, String expectedResponseType, Map<String,String> additionalHeaders) {
    LOG.info("Will send following POST request to " + url + "\n" + requestBody);
    return makeAsyncRequest(url, expectedResponseType,
      invoker -> invoker.post(Entity.entity(requestBody,requestMimeType)), additionalHeaders);
  }

  public static ResponseFuture makeAsyncPostRequest(
      String url, Object postBodyObject, String expectedResponseType, Map<String,String> additionalHeaders) {
    return makeAsyncPostRequest(url, JsonUtil.serializeObject(postBodyObject),
        MediaType.APPLICATION_JSON, expectedResponseType, additionalHeaders);
  }

  private static ResponseFuture makeAsyncRequest(String url, String expectedResponseType,
      Function<AsyncInvoker, Future<Response>> responseProducer, Map<String,String> additionalHeaders) {

    MultivaluedMap<String,Object> headers = new MultivaluedHashMap<>();

    // add accept header using passed response type or ALL if null
    headers.add(HttpHeaders.ACCEPT, expectedResponseType == null
        ? MediaType.WILDCARD : expectedResponseType);

    // add jersey header tracing header if logging flag turned on
    if (LOG_RESPONSE_HEADERS) {
      headers.add("X-Jersey-Tracing-Accept", "any");
    }

    // add any extra headers the caller submitted, overriding if necessary
    for (Entry<String,String> header : additionalHeaders.entrySet()) {
      headers.putSingle(header.getKey(), header.getValue());
    }

    return new ResponseFuture(
      responseProducer.apply(
        ClientBuilder.newClient()
          .target(url)
          .request()
          .headers(headers)
          .async()), LOG_RESPONSE_HEADERS);
  }

  public static CloseableResponse makeRequest(String url, HttpMethod method, Optional<JSONObject> body, Map<String,String> headers) {
    Client client = ClientBuilder.newClient();
    WebTarget webTarget = client.target(url);
    Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
    switch(method) {
      case POST:
        return new CloseableResponse(invocationBuilder
            .headers(new MultivaluedHashMap<>(headers))
            .post(Entity.entity(body.map(JSONObject::toString)
            .orElseThrow(() -> new RuntimeException("Body is required for POST")), MediaType.APPLICATION_JSON)));
      case GET:
        body.ifPresent(b -> LOG.warn("JSONObject passed to method to generated GET request; it will be ignored."));
        return new CloseableResponse(invocationBuilder
            .headers(new MultivaluedHashMap<>(headers))
            .get());
      default:
        throw new RuntimeException("Only POST and GET methods are currently supported (not " + method + ").");
    }
  }

  public static String readSmallResponseBody(InputStream smallResponseStream) throws IOException {
    try (ByteArrayOutputStream str = new ByteArrayOutputStream()) {
      IoUtil.transferStream(str, smallResponseStream);
      return str.toString();
    }
  }

  public static String readSmallResponseBody(Response smallResponse) throws IOException {
    if (!smallResponse.hasEntity()) return "";
    try (InputStream body = (InputStream)smallResponse.getEntity()) {
      return readSmallResponseBody(body);
    }
  }
}
