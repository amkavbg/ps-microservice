package ru.tokido.workshop.data;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ru.tokido.workshop.common.MicroServiceVerticle;

/**
 * a verticle generating info data about knows printer based on the configuration.
 */
public class GeneratorConfigVerticle extends MicroServiceVerticle {

    public static final String ADDRESS = "universe";

    /**
     * This method is called when the verticle is deployed.
     */
    @Override
    public void start() {
        super.start();

        //Read the configuration/ and deploy a PrinterDataVerticle for each printer in the config
        JsonArray printers = config().getJsonArray("printers");
        for (Object p : printers) {
            JsonObject printer  = (JsonObject) p;
            // Deploy the verticle with a configuration.
            vertx.deployVerticle(PrinterDataVerticle.class.getName(), new DeploymentOptions()
                    .setConfig(printer));
        }
        // Deploy another verticle without configuration.
        vertx.deployVerticle(RestDataAPIVerticle.class.getName(), new DeploymentOptions()
        .setConfig(config()));

        // Publish the services in the discovery infrastructure.
        publishMessageSource("printer-data", ADDRESS, rec -> {
            if (!rec.succeeded()) {
                rec.cause().printStackTrace();
            }
            System.out.println("Printer-Data service published : "+ rec.succeeded());
        });
        publishHttpEndPoint("printers", "localhost", config()
        .getInteger("http.port", 8080), ar -> {
            if (ar.failed()) {
                ar.cause().printStackTrace();
            } else {
                System.out.println("Printers (REST endpoint) service published : "+ ar.succeeded());
            }
        });
    }
}
