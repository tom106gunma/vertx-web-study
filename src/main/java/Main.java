import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.get("/").handler(ctx -> ctx.end("Hello Vert.x Web!"));

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onSuccess(server ->
                        System.out.println("HTTP server started on port " + server.actualPort()));
    }
}