package com.tomrcl.swapi;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import org.apache.log4j.BasicConfigurator;

public class MainVerticle extends AbstractVerticle {

  HttpServer server;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    OpenAPI3RouterFactory.create(this.vertx, "src/resources/swapi.yml", ar -> {
      if (ar.succeeded()) {
        OpenAPI3RouterFactory routerFactory = ar.result(); // (1)

        routerFactory.addHandlerByOperationId("42", routingContext ->
          routingContext
            .response() // (1)
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json") // (2)
            .end(String.valueOf(42)) // (3)
        );

        routerFactory.addHandlerByOperationId("getPeople", routingContext ->
          routingContext
            .response() // (1)
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json") // (2)
            .end(new JsonArray().encode()) // (3)
        );

        // router
        Router router = routerFactory.getRouter(); // <1>
        router.errorHandler(404, routingContext -> { // <2>
          JsonObject errorObject = new JsonObject() // <3>
            .put("code", 404)
            .put("message",
              (routingContext.failure() != null) ?
                routingContext.failure().getMessage() :
                "Not Found"
            );
          routingContext
            .response()
            .setStatusCode(404)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode()); // <4>
        });
        router.errorHandler(400, routingContext -> {
          JsonObject errorObject = new JsonObject()
            .put("code", 400)
            .put("message",
              (routingContext.failure() != null) ?
                routingContext.failure().getMessage() :
                "Validation Exception"
            );
          routingContext
            .response()
            .setStatusCode(400)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode());
        });

        server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost")); // (5)
        server.requestHandler(router).listen(); // (6)

        startPromise.complete(); // Complete the verticle start
      } else {
        // Something went wrong during router factory initialization
        startPromise.fail(ar.cause());
      }
    });
  }

  @Override
  public void stop() {
    this.server.close();
  }

  public static void main(String[] args) {
    BasicConfigurator.configure();

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
