import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.json.*;

public class App {
    private static final int PORT = 8080;
    private static final String STATIC_DIR = "static";
    private static final Map<String, String> users = new ConcurrentHashMap<>(); // username -> password
    private static final Map<String, String> sessions = new ConcurrentHashMap<>(); // sessionId -> username
    private static final Map<String, List<Message>> history = new ConcurrentHashMap<>(); // username -> messages
    private static final List<Intent> intents = new ArrayList<>();
    private static final Random random = new Random();
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "the", "is", "are", "am", "i", "you", "a", "an", "of", "to", "in", "on", "at", "for", "with", "and", "or", "do", "me", "my", "it", "this", "that", "your", "so", "as", "by", "be", "was", "were", "can", "will", "how", "what", "who", "when", "where", "why", "which"
    ));

    public static void main(String[] args) throws Exception {
        // Demo user
        users.put("user", "password");
        loadIntents();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/register", App::handleRegister);
        server.createContext("/login", App::handleLogin);
        server.createContext("/logout", App::handleLogout);
        server.createContext("/chat", App::handleChat);
        server.createContext("/history", App::handleHistory);
        server.createContext("/clear_history", App::handleClearHistory);
        server.createContext("/", App::handleStatic);
        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("Server started on http://localhost:" + PORT);
        server.start();
    }

    // --- Intent Loading ---
    private static void loadIntents() {
        try {
            String json = new String(Files.readAllBytes(Paths.get("intents.json")));
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String tag = obj.getString("tag");
                JSONArray patternsArr = obj.getJSONArray("patterns");
                JSONArray responsesArr = obj.getJSONArray("responses");
                List<String> patterns = new ArrayList<>();
                List<String> responses = new ArrayList<>();
                for (int j = 0; j < patternsArr.length(); j++) patterns.add(patternsArr.getString(j));
                for (int j = 0; j < responsesArr.length(); j++) responses.add(responsesArr.getString(j));
                intents.add(new Intent(tag, patterns, responses));
            }
        } catch (IOException | JSONException e) {
            System.err.println("Failed to load intents.json: " + e.getMessage());
        }
    }

    // --- HTTP Handlers ---
    private static void handleRegister(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { sendJson(ex, 405, "{}", null); return; }
        Map<String, String> data = parseJsonBody(ex);
        String username = data.get("username");
        String password = data.get("password");
        if (username == null || password == null) {
            sendJson(ex, 200, "{\"success\":false,\"error\":\"Username and password required.\"}", null);
            return;
        }
        if (users.containsKey(username)) {
            sendJson(ex, 200, "{\"success\":false,\"error\":\"Username already exists.\"}", null);
            return;
        }
        users.put(username, password);
        sendJson(ex, 200, "{\"success\":true}", null);
    }

    private static void handleLogin(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("GET")) {
            serveFile(ex, "login.html");
            return;
        }
        Map<String, String> data = parseJsonBody(ex);
        String username = data.get("username");
        String password = data.get("password");
        if (username == null || password == null || !users.containsKey(username) || !users.get(username).equals(password)) {
            sendJson(ex, 200, "{\"success\":false,\"error\":\"Invalid credentials\"}", null);
            return;
        }
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, username);
        List<String> headers = List.of("Set-Cookie: session=" + sessionId + "; Path=/");
        sendJson(ex, 200, "{\"success\":true}", headers);
    }

    private static void handleLogout(HttpExchange ex) throws IOException {
        String sessionId = getSessionId(ex);
        if (sessionId != null) sessions.remove(sessionId);
        ex.getResponseHeaders().add("Location", "/login");
        ex.sendResponseHeaders(302, -1);
    }

    private static void handleChat(HttpExchange ex) throws IOException {
        if (!isLoggedIn(ex)) { redirectToLogin(ex); return; }
        Map<String, String> data = parseJsonBody(ex);
        String userMsg = data.getOrDefault("message", "");
        String username = getUsername(ex);
        if (userMsg.isEmpty()) {
            sendJson(ex, 200, "{\"response\":\"Please enter a message.\"}", null);
            return;
        }
        String tag = predictIntent(userMsg);
        String botReply = getResponse(tag);
        history.computeIfAbsent(username, k -> new ArrayList<>()).add(new Message("user", userMsg));
        history.get(username).add(new Message("bot", botReply));
        sendJson(ex, 200, "{\"response\":\"" + escapeJson(botReply) + "\"}", null);
    }

    private static void handleHistory(HttpExchange ex) throws IOException {
        if (!isLoggedIn(ex)) { redirectToLogin(ex); return; }
        String username = getUsername(ex);
        List<Message> msgs = history.getOrDefault(username, new ArrayList<>());
        StringBuilder sb = new StringBuilder();
        sb.append("{\"history\":[");
        for (int i = 0; i < msgs.size(); i++) {
            Message m = msgs.get(i);
            sb.append("{\"sender\":\"").append(m.sender).append("\",\"message\":\"").append(escapeJson(m.message)).append("\",\"timestamp\":\"\"}");
            if (i < msgs.size() - 1) sb.append(",");
        }
        sb.append("]}");
        sendJson(ex, 200, sb.toString(), null);
    }

    private static void handleClearHistory(HttpExchange ex) throws IOException {
        if (!isLoggedIn(ex)) { redirectToLogin(ex); return; }
        String username = getUsername(ex);
        history.remove(username);
        sendJson(ex, 200, "{\"success\":true}", null);
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        serveFile(ex, path.substring(1));
    }

    // --- Helpers ---
    private static void serveFile(HttpExchange ex, String file) throws IOException {
        Path p = Paths.get(STATIC_DIR, file);
        if (!Files.exists(p)) {
            ex.sendResponseHeaders(404, -1);
            return;
        }
        String mime = Files.probeContentType(p);
        ex.getResponseHeaders().add("Content-Type", mime != null ? mime : "application/octet-stream");
        byte[] bytes = Files.readAllBytes(p);
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
    private static void sendJson(HttpExchange ex, int code, String json, List<String> extraHeaders) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        if (extraHeaders != null) for (String h : extraHeaders) ex.getResponseHeaders().add(h.split(": ",2)[0], h.split(": ",2)[1]);
        byte[] bytes = json.getBytes();
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
    private static Map<String, String> parseJsonBody(HttpExchange ex) throws IOException {
        String body = new BufferedReader(new InputStreamReader(ex.getRequestBody())).lines().collect(Collectors.joining("\n"));
        Map<String, String> map = new HashMap<>();
        try {
            JSONObject obj = new JSONObject(body);
            for (String key : obj.keySet()) {
                map.put(key, obj.getString(key));
            }
        } catch (JSONException e) {
            // ignore, return empty map
        }
        return map;
    }
    private static String getSessionId(HttpExchange ex) {
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String cookie : cookies) {
            for (String c : cookie.split(";")) {
                String[] kv = c.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals("session")) return kv[1];
            }
        }
        return null;
    }
    private static boolean isLoggedIn(HttpExchange ex) {
        String sessionId = getSessionId(ex);
        return sessionId != null && sessions.containsKey(sessionId);
    }
    private static String getUsername(HttpExchange ex) {
        String sessionId = getSessionId(ex);
        return sessionId != null ? sessions.get(sessionId) : null;
    }
    private static void redirectToLogin(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Location", "/login");
        ex.sendResponseHeaders(302, -1);
    }
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // --- Intent Prediction ---
    private static String normalize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    private static String predictIntent(String text) {
        String normText = normalize(text);
        // 1. Try exact pattern match (normalized)
        for (Intent intent : intents) {
            for (String pattern : intent.patterns) {
                if (normText.equals(normalize(pattern))) {
                    return intent.tag;
                }
            }
        }
        // 2. Try significant word match (not stopword, length >= 3)
        for (Intent intent : intents) {
            for (String pattern : intent.patterns) {
                String[] words = normalize(pattern).split(" ");
                for (String word : words) {
                    if (word.length() >= 3 && !STOPWORDS.contains(word) && normText.contains(word)) {
                        return intent.tag;
                    }
                }
            }
        }
        return "fallback";
    }
    private static String getResponse(String tag) {
        for (Intent intent : intents) {
            if (intent.tag.equals(tag)) {
                return intent.responses.get(random.nextInt(intent.responses.size()));
            }
        }
        return "Sorry, I didn't understand that.";
    }

    // --- Data Classes ---
    private static class Message {
        String sender, message;
        Message(String s, String m) { sender = s; message = m; }
    }
    private static class Intent {
        String tag;
        List<String> patterns, responses;
        Intent(String t, List<String> p, List<String> r) { tag = t; patterns = p; responses = r; }
    }
} 