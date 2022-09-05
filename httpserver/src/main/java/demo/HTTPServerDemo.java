package demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class HTTPServerDemo {

    static final int DEFAULT_HTTP_SERVER_PORT = 8080;
    static final int HTTP_OK = 200;

    public static void main(final String[] args) {
        HttpServer server;
        int port = DEFAULT_HTTP_SERVER_PORT;

        String serverPort = System.getenv("HTTP_SERVER_PORT");
        if (serverPort != null && serverPort.length() > 0) {
            try {
                port = Integer.parseInt(serverPort);
            } catch (Exception e) {
                System.out.println("Invalid port specified: " + serverPort);
                System.exit(1);
            }
        }

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Create end points
            // HttpContext rootContext = server.createContext("/");
            // rootContext.setHandler(HTTPServerDemo::handleRootRequest);
            HttpContext greetContext = server.createContext("/greet");
            greetContext.setHandler(HTTPServerDemo::handleGreetRequest);

            HttpContext timeContext = server.createContext("/gettime");
            timeContext.setHandler(HTTPServerDemo::handleTimeRequest);

            HttpContext convertContext = server.createContext("/convert");
            convertContext.setHandler(HTTPServerDemo::handleConvertRequest);

            HttpContext echoContext = server.createContext("/echo");
            echoContext.setHandler(HTTPServerDemo::handleEchoRequest);

            HttpContext deleteContext = server.createContext("/delete");
            deleteContext.setHandler(HTTPServerDemo::handleDeleteRequest);

            // start the HTTP server
            server.start();
            System.out.println("Started HTTP server on port " + port);
        } catch (IOException e) {
            System.out.println("Failed to start HTTP server on port " + port);
            System.exit(1);
        }
    }

    static Map<String, String> queryToMap(final String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&", 0)) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
        }
        return result;
    }

    private static void postResponse(int returnCode, String message, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(returnCode, message.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(message.getBytes());
        os.close();
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    // respond with a conversion of the form:
    // curl http://localhost:8080/'convert?source=inch&target=cm&value=1'
    private static void handleConvertRequest(HttpExchange exchange) throws IOException {
        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        String source = params.get("source");
        String target = params.get("target");
        String value = params.get("value");
        String message;
        int returnCode = HTTP_OK;
        if (isNullOrEmpty(source) || isNullOrEmpty(target) || isNullOrEmpty(value)) {
            message = "Missing query parameters";
            returnCode = 422;
        } else {
            Double result = 0.0;
            if (source.compareTo("inch") == 0 && target.compareTo("cm") == 0) {
                result = Double.valueOf(value) * 2.54;
                message = Double.toString(result);
            } else {
                message = "Unsupported conversion";
                returnCode = 422;
            }
        }
        postResponse(returnCode, message, exchange);
    }

    // post back value received
    // curl -v -d "this is a test" -X PUT localhost:8080/echo
    private static void handleEchoRequest(HttpExchange exchange) throws IOException {
        String message = "";
        int returnCode = HTTP_OK;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())
                && !"PUT".equalsIgnoreCase(exchange.getRequestMethod())
                && !"PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            message = "Invocation must be performed with POST/PUT/PATCH method";
            returnCode = 422;
        } else {
            StringBuilder sb = new StringBuilder();
            InputStream ios = exchange.getRequestBody();
            int i;
            while ((i = ios.read()) != -1) {
                sb.append((char) i);
            }
            if (sb.length() == 0) {
                returnCode = 411;
            } else {
                message = sb.toString();
            }
        }
        System.out.println(exchange.getRequestMethod() +
                " " +
                exchange.getRequestURI().toString() +
                " \"" + message + "\"");
        postResponse(returnCode, message, exchange);
    }

    // respond with a greeting
    private static void handleGreetRequest(HttpExchange exchange) throws IOException {
        System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI().toString());
        String message = "New greetings from the HTTPServerDemo crap!\n";
        postResponse(HTTP_OK, message, exchange);
    }

    // curl http://localhost:8080/gettime
    // respond with the current time and host running the server
    private static void handleTimeRequest(HttpExchange exchange) throws IOException {
        System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI().toString());
        String hostname = InetAddress.getLocalHost().getHostName();
        if (hostname == null) {
            hostname = InetAddress.getLocalHost().toString();
        }
        String message = "Host: " + hostname + " @ " + Instant.now() + "\n";
        postResponse(HTTP_OK, message, exchange);
    }

    // curl -X DELETE http://localhost:8080/delete/100
    private static void handleDeleteRequest(HttpExchange exchange) throws IOException {
        System.out.println(exchange.getRequestMethod() + " " + exchange.getRequestURI().toString());
        String message = "";
        int returnCode = HTTP_OK;
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            message = "Invocation must be performed with DELETE method";
            returnCode = 422;
        } else {
            message = exchange.getRequestURI().toString();
        }
        postResponse(returnCode, message, exchange);
    }
}
