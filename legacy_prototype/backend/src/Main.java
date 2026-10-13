import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import controller.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORT = 8080;
    private static final String FRONTEND_DIR = "frontend";

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // =================================================================
            // API Route Handlers
            // =================================================================
            AuthController authController = new AuthController();
            server.createContext("/api/login", authController);
            server.createContext("/api/auth", authController);

            EmployeePortalController employeeController = new EmployeePortalController();
            server.createContext("/api/employee", employeeController);
            server.createContext("/api/skills/extract-text", employeeController);
            server.createContext("/api/skills/extract-cert", employeeController);

            SkillVerificationController verificationController = new SkillVerificationController();
            server.createContext("/api/skills/verification-queue", verificationController);
            server.createContext("/api/skills/verify", verificationController);
            server.createContext("/api/skills/endorse", verificationController);
            server.createContext("/api/skills/audit-logs", verificationController);
            server.createContext("/api/skills/taxonomy", verificationController);
            server.createContext("/api/team/directory", verificationController);
            server.createContext("/api/leaves", verificationController);
            server.createContext("/api/tasks", verificationController);

            server.createContext("/api/developers", new DeveloperController());
            server.createContext("/api/skills", new SkillController());
            server.createContext("/api/projects", new ProjectController());
            server.createContext("/api/milestones", new MilestoneController());
            server.createContext("/api/allocations", new AllocationController());
            server.createContext("/api/availability", new AvailabilityController());
            server.createContext("/api/conflicts", new DashboardController());
            server.createContext("/api/skill-gaps", new DashboardController());
            server.createContext("/api/dashboard", new DashboardController());
            server.createContext("/api/notifications", new NotificationController());
            server.createContext("/api/simulations", new SimulationController());

            // =================================================================
            // Static Frontend Web Assets Handler (Serves /frontend directory)
            // =================================================================
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(12));
            server.start();

            System.out.println("==================================================================");
            System.out.println("  SIST CSE AI Capstone Project — Project Resource Allocation System");
            System.out.println("  Built with: Pure Java SE + HttpServer + JDBC (MySQL 8.0)");
            System.out.println("==================================================================");
            System.out.println("  [OK] Server listening on: http://localhost:" + PORT);
            System.out.println("  [OK] Frontend accessible at: http://localhost:" + PORT + "/index.html");
            System.out.println("  [OK] REST APIs available under: http://localhost:" + PORT + "/api/*");
            System.out.println("  Ready for academic viva evaluation and demonstration!");
            System.out.println("==================================================================");

        } catch (IOException e) {
            System.err.println("Fatal: Could not start HttpServer on port " + PORT + ": " + e.getMessage());
        }
    }

    /**
     * StaticFileHandler - Serves HTML5, CSS3, JavaScript, and static files directly
     * from the frontend/ directory without requiring any external web server.
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html") || path.isEmpty()) {
                path = "/index.html";
            }

            String relPath = path.startsWith("/") ? path.substring(1) : path;
            if (relPath.isEmpty()) relPath = "index.html";

            File file = new File(FRONTEND_DIR, relPath);
            if (!file.exists()) {
                // Check relative to parent directory if launched from backend/
                File parentDirFallback = new File("../" + FRONTEND_DIR, relPath);
                if (parentDirFallback.exists()) {
                    file = parentDirFallback;
                }
            }

            if (!file.exists() || file.isDirectory()) {
                file = new File(FRONTEND_DIR, "index.html");
                if (!file.exists()) {
                    file = new File("../" + FRONTEND_DIR, "index.html");
                }
            }

            if (!file.exists()) {
                String notFound = "404 Not Found: Static asset " + path + " could not be resolved.";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            System.out.println("  -> [WEB] Serving " + path + " (" + file.getName() + ")");

            String contentType = getContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getContentType(String filename) {
            if (filename.endsWith(".html") || filename.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            return "application/octet-stream";
        }
    }
}
