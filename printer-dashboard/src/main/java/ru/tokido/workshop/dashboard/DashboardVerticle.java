package ru.tokido.workshop.dashboard;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;
import io.vertx.servicediscovery.types.HttpEndpoint;
import ru.tokido.workshop.common.MicroServiceVerticle;

/**
 * @author tokido
 *
 */
public class DashboardVerticle extends MicroServiceVerticle {

    private CircuitBreaker circuit;
    private WebClient client;

    @Override
    public void start(Future<Void> future) {
        super.start();
        Router router = Router.router(vertx);
        //Event bus bridge
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        BridgeOptions options = new BridgeOptions();
        options
                .addOutboundPermitted(new PermittedOptions().setAddress("universe"))
//                .addOutboundPermitted(new PermittedOptions().setAddress("audit"))
//                .addOutboundPermitted(new PermittedOptions().setAddress("service.audit"))
//                .addInboundPermitted(new PermittedOptions().setAddress("service.audit"))
                .addOutboundPermitted(new PermittedOptions().setAddress("vertx.circuit-breaker"));


        sockJSHandler.bridge(options);
        router.route("/eventbus/*").handler(sockJSHandler);

        //Discovery endpoint
        ServiceDiscoveryRestEndpoint.create(router, discovery);

        //
        router.get("/operations").handler(this::callAuditService);

        //static
        router.route("/*").handler(StaticHandler.create());

        //circuit breaker create
        circuit = CircuitBreaker.create("http-audit-service", vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(2)
                        .setFallbackOnFailure(true)
                        .setResetTimeout(2000)
                        .setTimeout(1000))
        .openHandler(v -> retrieveAuditService());

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(9999, ar -> {
                    if (ar.failed()) {
                        future.fail(ar.cause());
                    } else {
                        retrieveAuditService();
                        future.complete();
                    }
                });
    }

    @Override
    public void stop() throws Exception {
        if (client !=null){
            client.close();
        }
        circuit.close();
    }

    private Future<Void> retrieveAuditService() {
        return Future.future(future -> {
            HttpEndpoint.getWebClient(discovery,
                    new JsonObject().put("name", "audit"), client -> {
                        this.client = client.result();
                        future.handle(client.map((Void)null));
                    });
        });
    }

    private void callAuditService(RoutingContext context) {
        if (client == null){
            context.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(200)
                    .end(new JsonObject().put("message", "No audit service").encode());
        } else {
            client.get("/").send(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    context.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(response.body());
                }
            });
        }
    }

//    private void callAuditService2(RoutingContext context) {
//        HttpServerResponse response = context.response()
//                .putHeader("content-type", "application/json")
//                .setStatusCode(200);
//        circuit.executeWithFallback(
//                future ->
//                        client.get("/").send(ar -> future.handle(ar.map(HttpResponse::body))),
//                t -> Buffer.buffer("{\"message\":\"No audit service, or unable to call it\"}")
//        )
//                .setHandler(ar -> response.end(ar.result()));
//    }


}
