package Main;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class HttpServer extends AbstractVerticle {
	private static Logger logger = LogManager.getLogger(HttpServer.class);
	private int httpPort = 8080;
	private Vertx vertx;

	int count = 0;

	private BasicConfig basicConfig = null;

	public HttpServer(Vertx vertx, BasicConfig basicConfig) {
		this.vertx = vertx;
		this.basicConfig = basicConfig;
	}

	@Override
	public void start() throws Exception {

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.get("/").handler(this::index);
		router.get("/index").handler(this::index);
		router.get("/home").handler(this::index);
		router.get("/hostname").handler(this::getHostIformation);
		router.post("/upload").handler(this::upload);

		vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("port", httpPort));
		System.out.println("Listening on: " + "http://localhost:" + httpPort);
	}

	private void getHostIformation(RoutingContext routingContext) {

		JsonObject hostinfo = new JsonObject();
		hostinfo.put("hostname", getSystemInfo().getValue("hostname", "unknown"));
		hostinfo.put("os.name", System.getProperty("os.name"));
		hostinfo.put("os.arch", System.getProperty("os.arch"));

		routingContext.response().end(hostinfo.toString());
	}

	private JsonObject getSystemInfo() {

		JsonObject systemInfo = new JsonObject();

		try {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostAddress();
			systemInfo.put("hostname", hostname);
			return systemInfo;
		} catch (UnknownHostException ex) {
			System.out.println("Hostname can not be resolved");
			return systemInfo;
		}
	}

	private void index(RoutingContext routingContext) {

		String hostname = getSystemInfo().getString("hostname", "Not Available");
		String methodName = new Object() {
		}.getClass().getEnclosingMethod().getName();
		logMethodCall(Arrays.asList("Called:", methodName));
		String indexFile = "<!DOCTYPE html>\r\n" + "<html lang=\"en\">\r\n" + "    <head>\r\n"
				+ "        <title>File Upload</title>\r\n"
				+ "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n"
				+ "    </head>\r\n" + "    <body>\r\n" + " 	   Please select a file and press 'upload' <br /> "
				+ "        <form method=\"POST\" action=\"upload\" enctype=\"multipart/form-data\" >\r\n"
				+ "            File:\r\n" + "            <input type=\"file\" name=\"file\" id=\"file\" /> <br/>\r\n"

				+ "            </br>\r\n"
				+ "            <input type=\"submit\" value=\"Upload\" name=\"upload\" id=\"upload\" />\r\n"
				+ "        </form>\r\n" + "        Host Name: " + hostname + "    </body>\r\n" + "</html>";

		routingContext.request().response().setStatusCode(HttpStatus.SC_OK)
				.sendFile("index.html");

	}


	private void logMethodCall(List<String> parameters) {
		logger.info(String.join(" ", parameters));
	}

	private void upload(RoutingContext routingContext) {
		HttpServerResponse response = routingContext.response();
		response.setStatusCode(200);
		logger.info("Handling teh upload here");
		String responseString = "Received: ";

		for (FileUpload f : routingContext.fileUploads()) {
			String fileName = f.fileName();
			Buffer uploaded = vertx.fileSystem().readFileBlocking(f.uploadedFileName());
			vertx.fileSystem().writeFileBlocking(fileName, uploaded);

			responseString = responseString + "\n " + f.fileName();
			// do whatever you need to do with the file (it is already saved
			// on the directory you wanted...

		}

		responseString = responseString + ".";
		logger.info(responseString);
		response.end(getUploadresponse(responseString));
		// routingContext.response().end();
	}

	private String getUploadresponse(String message) {

		message = "<!DOCTYPE html>\r\n" + "<html lang=\"en\">\r\n" + "    <head>\r\n"
				+ "        <title>File Upload Result</title>\r\n"
				+ "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n"
				+ "    </head>\r\n" + "    <body>\r\n" + message + "     <br />" + "     <a href=\"index\">Home</a>"
				+ "    </body>\r\n" + "</html>";
		return message;

	}

	private void sendAFile(HttpServerResponse response, String resolvedFile) {
		System.out.println("SenAFile called " + resolvedFile);
		String fileExtension = org.apache.commons.io.FilenameUtils.getExtension(resolvedFile);

		switch (fileExtension.toUpperCase()) {
		case "M3U8":

			response.putHeader("Content-Type", "application/x-mpegURL");
			break;
		case "TS":
			response.putHeader("Content-Type", "video/MP2T");
			break;

		default:
			response.putHeader("Content-Type", "application/vnd.ms-sstr+xml");
			break;

		}

		response.setChunked(true);
		response.setWriteQueueMaxSize(10000);
		response.putHeader("Accept-Ranges", "bytes");
		response.putHeader("Content-Disposition", "attachment");
		response.setStatusCode(200);
		vertx.fileSystem().open(resolvedFile, new OpenOptions(), file -> {
			AsyncFile asyncFile = file.result();
			Pump pump = Pump.pump(asyncFile, response);
			response.closeHandler(closeRes -> {
				// logger.info("connection close.");
				System.out.println("Connection clsoe");
				asyncFile.close();
			});
			response.exceptionHandler(exceptionRes -> {
				// logger.error(exceptionRes.getMessage(), exceptionRes);
				System.out.println(exceptionRes.getMessage());
				asyncFile.close();
			});
			asyncFile.endHandler(e -> {
				// logger.info("download completed.");
				System.out.println("completed");
				response.end();
				asyncFile.close();
			});
			pump.start();
		});
	}

}
