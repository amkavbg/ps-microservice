package ru.tokido.workshop.data;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.Random;

/**
 * It sets the new data on the 'universe' address on the event bus.
 */
public class PrinterDataVerticle extends AbstractVerticle {

    String name;
    String location;
    String ip;
    long period;
    int black_toner;
    int black_drum;
    int total_printed_black;


    int variation;
    private final Random random = new Random();

    /**
     * Method called when the verticle is deployed.
     */
    @Override
    public void start(){
        // Retrieve the configuration, and init the verticle.
        JsonObject config = config();
        init(config);
        // Every `period` ms, the given Handler is called.
        vertx.setPeriodic(period, l -> {
            mining();
            send();
        });
    }

    /**
     * Read the configuration
     * @param config the configuration
     */
    void init(JsonObject config) {
        period = config.getLong("period", 5000L);
        name = config.getString("name");
        location = config.getString("location", "НЕ УКАЗАНО");
        ip = config.getString("ip");
        variation = config.getInteger("variation", 100);
    }

    /**
     * Sends the printer data on the event bus
     */
    private void send() {
        vertx.eventBus().publish(GeneratorConfigVerticle.ADDRESS, toJson());
    }

    /**
     * Compute the printer data. Mining from printer.
     */
    void mining() {
        //COMPUTE ALL DATA HERE
        if (random.nextBoolean()) {
            black_toner = black_toner + random.nextInt(variation);
            black_drum = black_drum + random.nextInt(variation / 2);
            total_printed_black = total_printed_black + random.nextInt(variation);
        } else {
            black_toner = black_toner - random.nextInt(variation);
            black_drum = black_drum - random.nextInt(variation / 2);
            total_printed_black = total_printed_black - random.nextInt(variation);
        }

        if (black_toner <= 0) {
            black_toner = 100;
        }
        if (black_drum <= 0) {
            black_drum = 100;
        }
        if (total_printed_black <= 0) {
            total_printed_black = 100;
        }
    }

    /**
     * @return a json representation of the printer data.
     */
    private JsonObject toJson() {
        return new JsonObject()
                .put("name", name)
                .put("location", location)
                .put("ip", ip)
                .put("black-toner", black_toner)
                .put("black-drum", black_drum)
                .put("total-printed-black", total_printed_black);
    }

}
