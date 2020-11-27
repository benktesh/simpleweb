package Main;

import io.vertx.core.Vertx;

import io.vertx.ext.web.client.WebClient;

import java.io.IOException;

public class Bootstrap {

	private static BasicConfig basicConfig = null;

	public static WebClient webclient = null;

	static Vertx vertx;

	public static void vertx() {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new HttpServer(vertx, basicConfig));
		webclient = WebClient.create(vertx);
	}

	public static void main(String[] args) throws IOException {
		vertx();
	}

}
