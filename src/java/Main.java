import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8082"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/api/login", Main::login);
        server.createContext("/api/members", Main::members);
        server.createContext("/api/trainers", Main::trainers);
        server.createContext("/api/memberships", Main::memberships);
        server.createContext("/api/attendance", Main::attendance);
        server.createContext("/api/payments", Main::payments);
        server.createContext("/api/stats", Main::getStats);
        server.createContext("/", Main::serveFiles);

        server.setExecutor(null);
        server.start();

        System.out.println("Server started: http://localhost:8082");
    }

    private static void login(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String data = readBody(exchange);
        String email = extractValue(data, "email");
        String password = extractValue(data, "password");

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        String response;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                response = rs.next()
                        ? "{\"success\":true,\"message\":\"Login successful\"}"
                        : "{\"success\":false,\"message\":\"Invalid email or password\"}";
            }

        } catch (Exception e) {
            e.printStackTrace();
            response = "{\"success\":false,\"message\":\"Database connection failed\"}";
        }

        sendJsonResponse(exchange, response);
    }

    private static void members(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            getMembers(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
            addMember(exchange);
        } else if (method.equalsIgnoreCase("DELETE")) {
            deleteMember(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private static void addMember(HttpExchange exchange) throws IOException {
        String data = readBody(exchange);

        String name = extractValue(data, "name");
        String email = extractValue(data, "email");
        String phone = extractValue(data, "phone");
        String gender = extractValue(data, "gender");
        String joiningDate = extractValue(data, "joining_date");
        String membershipType = extractValue(data, "membership_type");

        String sql = "INSERT INTO members (name, email, phone, gender, joining_date, membership_type) VALUES (?, ?, ?, ?, ?, ?)";
        String response;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, gender);
            ps.setString(5, joiningDate);
            ps.setString(6, membershipType);

            ps.executeUpdate();

            response = "{\"success\":true,\"message\":\"Member added successfully\"}";

        } catch (Exception e) {
            e.printStackTrace();
            response = "{\"success\":false,\"message\":\"Failed to add member\"}";
        }

        sendJsonResponse(exchange, response);
    }

    private static void deleteMember(HttpExchange exchange) throws IOException {
        String[] parts = exchange.getRequestURI().getPath().split("/");

        if (parts.length < 4) {
            sendJsonResponse(exchange,
                    "{\"success\":false,\"message\":\"Member ID required\"}");
            return;
        }

        try {
            int memberId = Integer.parseInt(parts[3]);

            try (Connection con = DatabaseConnection.getConnection()) {
                con.setAutoCommit(false);

                try {
                    String[] queries = {
                        "DELETE FROM attendance WHERE member_id = ?",
                        "DELETE FROM memberships WHERE member_id = ?",
                        "DELETE FROM payments WHERE member_id = ?",
                        "DELETE FROM members WHERE id = ?"
                    };

                    int memberRows = 0;

                    for (int i = 0; i < queries.length; i++) {
                        try (PreparedStatement ps = con.prepareStatement(queries[i])) {
                            ps.setInt(1, memberId);

                            int rows = ps.executeUpdate();

                            if (i == 3) {
                                memberRows = rows;
                            }
                        }
                    }

                    if (memberRows == 0) {
                        con.rollback();

                        sendJsonResponse(exchange,
                                "{\"success\":false,\"message\":\"Member not found\"}");
                        return;
                    }

                    con.commit();

                    sendJsonResponse(exchange,
                            "{\"success\":true,\"message\":\"Member and related records deleted successfully\"}");

                } catch (Exception e) {
                    con.rollback();
                    throw e;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

            sendJsonResponse(exchange,
                    "{\"success\":false,\"message\":\"Unable to delete member\"}");
        }
    }

    private static void getMembers(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT * FROM members";
        boolean first = true;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"name\":\"").append(jsonEscape(rs.getString("name"))).append("\",")
                        .append("\"email\":\"").append(jsonEscape(rs.getString("email"))).append("\",")
                        .append("\"phone\":\"").append(jsonEscape(rs.getString("phone"))).append("\",")
                        .append("\"gender\":\"").append(jsonEscape(rs.getString("gender"))).append("\",")
                        .append("\"joining_date\":\"").append(rs.getString("joining_date")).append("\",")
                        .append("\"membership_type\":\"").append(jsonEscape(rs.getString("membership_type"))).append("\"")
                        .append("}");

                first = false;
            }

            json.append("]");
            sendJsonResponse(exchange, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, "[]");
        }
    }

    private static void trainers(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            getTrainers(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
            addTrainer(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private static void addTrainer(HttpExchange exchange) throws IOException {
        String data = readBody(exchange);

        String name = extractValue(data, "name");
        String email = extractValue(data, "email");
        String phone = extractValue(data, "phone");
        String specialization = extractValue(data, "specialization");

        String sql = "INSERT INTO trainers (name, email, phone, specialization) VALUES (?, ?, ?, ?)";
        String response;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, specialization);

            ps.executeUpdate();

            response = "{\"success\":true,\"message\":\"Trainer added successfully\"}";

        } catch (Exception e) {
            e.printStackTrace();
            response = "{\"success\":false,\"message\":\"Failed to add trainer\"}";
        }

        sendJsonResponse(exchange, response);
    }

    private static void getTrainers(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT * FROM trainers";
        boolean first = true;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"name\":\"").append(jsonEscape(rs.getString("name"))).append("\",")
                        .append("\"email\":\"").append(jsonEscape(rs.getString("email"))).append("\",")
                        .append("\"phone\":\"").append(jsonEscape(rs.getString("phone"))).append("\",")
                        .append("\"specialization\":\"").append(jsonEscape(rs.getString("specialization"))).append("\"")
                        .append("}");

                first = false;
            }

            json.append("]");
            sendJsonResponse(exchange, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, "[]");
        }
    }

    private static void memberships(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            getMemberships(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
            addMembership(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private static void addMembership(HttpExchange exchange) throws IOException {
        String data = readBody(exchange);

        String memberId = extractValue(data, "member_id");
        String planName = extractValue(data, "plan_name");
        String startDate = extractValue(data, "start_date");
        String endDate = extractValue(data, "end_date");
        String amount = extractValue(data, "amount");

        String sql = "INSERT INTO memberships (member_id, plan_name, start_date, end_date, amount) VALUES (?, ?, ?, ?, ?)";
        String response;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(memberId));
            ps.setString(2, planName);
            ps.setString(3, startDate);
            ps.setString(4, endDate);
            ps.setDouble(5, Double.parseDouble(amount));

            ps.executeUpdate();

            response = "{\"success\":true,\"message\":\"Membership added successfully\"}";

        } catch (Exception e) {
            e.printStackTrace();
            response = "{\"success\":false,\"message\":\"Failed to add membership\"}";
        }

        sendJsonResponse(exchange, response);
    }

    private static void getMemberships(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT * FROM memberships";
        boolean first = true;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"member_id\":").append(rs.getInt("member_id")).append(",")
                        .append("\"plan_name\":\"").append(jsonEscape(rs.getString("plan_name"))).append("\",")
                        .append("\"start_date\":\"").append(rs.getString("start_date")).append("\",")
                        .append("\"end_date\":\"").append(rs.getString("end_date")).append("\",")
                        .append("\"amount\":").append(rs.getDouble("amount"))
                        .append("}");

                first = false;
            }

            json.append("]");
            sendJsonResponse(exchange, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, "[]");
        }
    }

    private static void attendance(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            getAttendance(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
            addAttendance(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private static void addAttendance(HttpExchange exchange) throws IOException {
        String data = readBody(exchange);

        String memberId = extractValue(data, "member_id");
        String attendanceDate = extractValue(data, "attendance_date");
        String checkIn = extractValue(data, "check_in");
        String checkOut = extractValue(data, "check_out");

        String sql = "INSERT INTO attendance (member_id, attendance_date, check_in, check_out) VALUES (?, ?, ?, ?)";
        String response;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(memberId));
            ps.setString(2, attendanceDate);
            ps.setString(3, checkIn);

            if (checkOut == null || checkOut.isEmpty()) {
                ps.setNull(4, java.sql.Types.TIME);
            } else {
                ps.setString(4, checkOut);
            }

            ps.executeUpdate();

            response = "{\"success\":true,\"message\":\"Attendance marked successfully\"}";

        } catch (Exception e) {
            e.printStackTrace();
            response = "{\"success\":false,\"message\":\"Failed to mark attendance\"}";
        }

        sendJsonResponse(exchange, response);
    }

    private static void getAttendance(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT * FROM attendance";
        boolean first = true;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }

                String checkOut = rs.getString("check_out");

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"member_id\":").append(rs.getInt("member_id")).append(",")
                        .append("\"attendance_date\":\"").append(rs.getString("attendance_date")).append("\",")
                        .append("\"check_in\":\"").append(rs.getString("check_in")).append("\",")
                        .append("\"check_out\":\"").append(checkOut == null ? "" : checkOut).append("\"")
                        .append("}");

                first = false;
            }

            json.append("]");
            sendJsonResponse(exchange, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, "[]");
        }
    }

    private static void payments(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            getPayments(exchange);
        } else if (method.equalsIgnoreCase("POST")) {
            addPayment(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private static void addPayment(HttpExchange exchange) throws IOException {
        String data = readBody(exchange);

        String memberId = extractValue(data, "member_id");
        String amount = extractValue(data, "amount");
        String paymentDate = extractValue(data, "payment_date");
        String paymentMethod = extractValue(data, "payment_method");
        String status = extractValue(data, "status");

        String sql = "INSERT INTO payments (member_id, amount, payment_date, payment_method, status) VALUES (?, ?, ?, ?, ?)";
        String response;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(memberId));
            ps.setDouble(2, Double.parseDouble(amount));
            ps.setDate(3, java.sql.Date.valueOf(paymentDate));
            ps.setString(4, paymentMethod);
            ps.setString(5, status);

            ps.executeUpdate();

            response = "{\"success\":true,\"message\":\"Payment recorded successfully\"}";

        } catch (Exception e) {
            e.printStackTrace();
            response = "{\"success\":false,\"message\":\"Failed to record payment\"}";
        }

        sendJsonResponse(exchange, response);
    }

    private static void getPayments(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT * FROM payments ORDER BY id DESC";
        boolean first = true;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"member_id\":").append(rs.getInt("member_id")).append(",")
                        .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                        .append("\"payment_date\":\"").append(rs.getDate("payment_date")).append("\",")
                        .append("\"payment_method\":\"").append(jsonEscape(rs.getString("payment_method"))).append("\",")
                        .append("\"status\":\"").append(jsonEscape(rs.getString("status"))).append("\"")
                        .append("}");

                first = false;
            }

            json.append("]");
            sendJsonResponse(exchange, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, "[]");
        }
    }

    private static void getStats(HttpExchange exchange) throws IOException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM members) AS members, "
                + "(SELECT COUNT(*) FROM trainers) AS trainers, "
                + "(SELECT COUNT(*) FROM memberships) AS memberships, "
                + "(SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'Paid') AS payments";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String response = "{"
                        + "\"members\":" + rs.getInt("members") + ","
                        + "\"trainers\":" + rs.getInt("trainers") + ","
                        + "\"memberships\":" + rs.getInt("memberships") + ","
                        + "\"payments\":" + rs.getDouble("payments")
                        + "}";

                sendJsonResponse(exchange, response);
            }

        } catch (Exception e) {
            e.printStackTrace();

            sendJsonResponse(exchange,
                    "{\"success\":false,\"message\":\"Failed to load statistics\"}");
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    private static String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*\"([^\"]*)\""
        );

        Matcher matcher = pattern.matcher(json);

        return matcher.find() ? matcher.group(1) : "";
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static void sendJsonResponse(
            HttpExchange exchange,
            String response
    ) throws IOException {

        byte[] output = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, output.length);
        exchange.getResponseBody().write(output);
        exchange.close();
    }

    private static void serveFiles(HttpExchange exchange) throws IOException {
        String fileName = exchange.getRequestURI().getPath();

        if (fileName.equals("/")) {
            fileName = "/index.html";
        }

        Path file = Paths.get("web" + fileName).normalize();

        if (!Files.exists(file) || Files.isDirectory(file)) {
            String response = "404 - File Not Found";
            byte[] output = response.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(404, output.length);
            exchange.getResponseBody().write(output);
            exchange.close();
            return;
        }

        String contentType;

        if (fileName.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            contentType = "application/javascript; charset=UTF-8";
        } else {
            contentType = "text/plain; charset=UTF-8";
        }

        byte[] content = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
    }
}