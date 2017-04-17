package ru.tokido.workshop.data;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * This verticle exposes a HTTP endpoint to retrieve the current /
 * last values of the printer data (printer).
 * @author tokido
 */
public class RestDataAPIVerticle extends AbstractVerticle {

    private Map<String, JsonObject> dataprinters = new HashMap<>();

    @Override
    public void start() throws Exception {
        vertx.eventBus().<JsonObject>consumer(GeneratorConfigVerticle.ADDRESS, message -> {
            // Printers are json objects you can retrieve from the message body
            // The map is structured as follows: name -> printer
            JsonObject dataprinter = message.body();
            dataprinters.put(dataprinter.getString("name"), dataprinter);
             });
            // Create a HTTP server that returns the printers
            // The request handler returns a specific printer
            // if the `name` parameter is set, or the whole map if none.
            // To write the response use: `request.response().end(content)`
            // Responses are returned as JSON, so don't forget
            // the "content-type": "application/json" header.
            // If the symbol is set but not found, you should return 404.
            // Once the request handler is set,

            vertx.createHttpServer()
                    .requestHandler(request -> {
                        HttpServerResponse response = request.response()
                                .putHeader("content-type", "application/json");

                        String printer = request.getParam("name");
                        if (printer == null) {
                            String content = Json.encodePrettily(dataprinters);
                            response
                                    .end(content);
                        } else {
                            JsonObject dataprinter = dataprinters.get(printer);
                            if (dataprinter == null) {
                                response.setStatusCode(404).end();
                            } else {
                                response
                                        .end(dataprinter.encodePrettily());
                            }
                        }
                    })
                    .listen(config().getInteger("http.port", 8080), ar -> {
                        if (ar.succeeded()) {
                            System.out.println("Server started on port "+ar.result().actualPort());
                        } else {
                            System.out.println("Cannot start the server: "+ar.cause());
                        }
                    });
    }
}
