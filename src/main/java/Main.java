import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        // 3-3：全リクエスト共通のログ（next() の基本）
        router.route().handler(ctx -> {
            long start = System.currentTimeMillis();
            System.out.println("→ " + ctx.request().method() + " " + ctx.request().path());

            ctx.response().endHandler(v -> {
                long time = System.currentTimeMillis() - start;
                System.out.println("← " + ctx.response().getStatusCode() + "（" + time + "ms）");
            });

            // 3-4：next() を消してみる
            ctx.next();  // 次のハンドラーへ
        });

        // 4-2：BodyHandlerを追加する
        //router.route().handler(BodyHandler.create());
        // 4-7：サイズ制限をかける
        router.route().handler(BodyHandler.create().setBodyLimit(1024 * 100));

        router.get("/").handler(ctx -> ctx.end("Hello Vert.x Web!"));

        // 1-1：ルートを増やす
        router.get("/hello").handler(ctx -> ctx.end("Hello from /hello"));

        // 1-2：ステータスコードとヘッダーを付ける
        router.get("/status").handler(ctx ->
                ctx.response()
                        .setStatusCode(201)
                        .putHeader("Content-Type", "text/plain; charset=utf-8")
                        .end("作成しました"));

        // 1-3：リクエスト情報を読む
        router.get("/info").handler(ctx -> {
            String method = ctx.request().method().name();
            String path = ctx.request().path();
            ctx.end("method=" + method + ", path=" + path);
        });

        // 1-4：JSONを返す
        router.get("/json").handler(ctx ->
                ctx.json(new JsonObject().put("message", "hello")));

        // 1-5：わざと end を呼ばない
        router.get("/hang").handler(ctx -> System.out.println("/hang が呼ばれました（でも応答しない）"));

        // 2-1：パスパラメータ
        router.get("/users/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            ctx.end("ユーザーID: " + id);
        });

        // 2-2：パラメータを2つ
        router.get("/users/:userId/orders/:orderId").handler(ctx -> {
            String userId = ctx.pathParam("userId");
            String orderId = ctx.pathParam("orderId");
            ctx.json(new JsonObject()
                    .put("userId", userId)
                    .put("orderId", orderId));
        });

        // 2-3：型付きパスパラメータ
        router.get("/products/:id::integer").handler(ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            ctx.end("商品ID（数値）: " + id);
        });

        // 2-4：クエリパラメータ
        router.get("/search").handler(ctx -> {
            String keyword = ctx.request().getParam("keyword");
            String page = ctx.request().getParam("page", "1");  // ないときの初期値
            ctx.end("keyword=" + keyword + ", page=" + page);
        });

        // 2-5：前方一致（ワイルドカード）
        router.get("/files/*").handler(ctx -> ctx.end("ファイル要求: " + ctx.pathParam("*")));

        // 2-6：登録順の罠
        router.get("/members/:name").handler(ctx ->
                ctx.end("メンバー: " + ctx.pathParam("name")));
        router.get("/members/me").handler(ctx ->
                ctx.end("ログイン中の自分の情報"));

        // 3-1：同じURLをメソッドで分ける
        router.get("/items").handler(ctx -> ctx.end("一覧を取得"));
        router.post("/items").handler(ctx -> ctx.end("新規作成"));
        router.put("/items/:id").handler(ctx -> ctx.end("ID " + ctx.pathParam("id") + " を更新"));
        router.delete("/items/:id").handler(ctx -> ctx.end("ID " + ctx.pathParam("id") + " を削除"));

        // 3-2：1つのルートで複数メソッドを受ける
        router.route("/ping")
                .method(HttpMethod.GET)
                .method(HttpMethod.POST)
                .handler(ctx -> ctx.end("pong（" + ctx.request().method() + "）"));

        // 3-5：ハンドラー間でデータを渡す
        router.get("/greet/:name").handler(ctx -> {
            String name = ctx.pathParam("name");
            ctx.put("message", "こんにちは、" + name + "さん");  // 預ける
            ctx.next();
        });
        router.get("/greet/:name").handler(ctx -> {
            String message = ctx.get("message");                // 受け取る
            ctx.end(message);
        });

        // 3-6：複数ハンドラーで少しずつ書き出す・順番を変える
        router.get("/steps").order(1).handler(ctx -> {
            ctx.response().write("step1\n");
            ctx.next();
        });
        router.get("/steps").order(0).handler(ctx -> {
            ctx.response().setChunked(true);  // 少しずつ書き出すときは最初に1回必要
            ctx.response().write("step0\n");
            ctx.next();
        });
        router.get("/steps").order(2).handler(ctx -> {
            ctx.response().end("step2\n");
        });

        // 4-1：BodyHandlerなしで読んでみる
        router.post("/echo").handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            ctx.end("受け取った内容: " + body);
        });

        // 4-3：JSONを受け取って登録する
        router.post("/products").handler(ctx -> {
            // 4-5：壊れたJSONを送ってみる
            JsonObject body;
            try {
                body = ctx.body().asJsonObject();
            } catch (DecodeException e) {
                ctx.response().setStatusCode(400);
                ctx.json(new JsonObject().put("error", "JSONの形式が正しくありません"));
                return;
            }
            if (body == null) {
                ctx.response().setStatusCode(400);
                ctx.json(new JsonObject().put("error", "リクエストボディが空です"));
                return;
            }

            String name = body.getString("name");
            Integer price = body.getInteger("price");
            // 4-4：入力チェックをする
            if (name == null || name.isBlank()) {
                ctx.response().setStatusCode(400);
                ctx.json(new JsonObject().put("error", "nameは必須です"));
                return;
            }
            if (price == null || price < 0) {
                ctx.response().setStatusCode(400);
                ctx.json(new JsonObject().put("error", "priceは0以上の数値で指定してください"));
                return;
            }

            JsonObject created = new JsonObject()
                    .put("id", 1)
                    .put("name", name)
                    .put("price", price);

            ctx.response().setStatusCode(201);
            ctx.json(created);
        });

        // 4-6：JSON以外を受け付けない
        router.post("/strict").consumes("application/json").handler(ctx ->
                ctx.json(ctx.body().asJsonObject()));

        // 4-8：フォームの値を読む
        router.post("/form").handler(ctx -> {
            String name = ctx.request().getFormAttribute("name");
            ctx.end("フォームから受け取った名前: " + name);
        });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onSuccess(server ->
                        System.out.println("HTTP server started on port " + server.actualPort()));
    }
}