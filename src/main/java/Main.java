import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.get("/").handler(ctx -> ctx.end("Hello Vert.x Web!"));

        // 1：ルートを増やす
        router.get("/hello").handler(ctx -> ctx.end("Hello from /hello"));

        // 2：ステータスコードとヘッダーを付ける
        router.get("/status").handler(ctx ->
                ctx.response()
                        .setStatusCode(201)
                        .putHeader("Content-Type", "text/plain; charset=utf-8")
                        .end("作成しました"));

        // 3：リクエスト情報を読む
        router.get("/info").handler(ctx -> {
            String method = ctx.request().method().name();
            String path = ctx.request().path();
            ctx.end("method=" + method + ", path=" + path);
        });

        // 4：JSONを返す
        router.get("/json").handler(ctx ->
                ctx.json(new JsonObject().put("message", "hello")));

        // 5：わざと end を呼ばない
        router.get("/hang").handler(ctx -> System.out.println("/hang が呼ばれました（でも応答しない）"));

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onSuccess(server ->
                        System.out.println("HTTP server started on port " + server.actualPort()));
    }
}