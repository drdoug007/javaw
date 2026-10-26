package dastec.one.javaw.examples.log4j;

import dastec.one.javaw.dom.DOM;
import dastec.one.javaw.dom.Element;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.simple.SimpleLogger;

/**
 * Enterprise Log4j2 Logging Showcase for WebAssembly.
 * Demonstrates:
 * 1. LogManager logger factory and category-based logger instances.
 * 2. Standard Log4j2 severity levels (TRACE, DEBUG, INFO, WARN, ERROR, FATAL).
 * 3. ThreadContext (Mapped Diagnostic Context / MDC) trace correlation.
 * 4. Parameterized message formatting with '{}' placeholders.
 * 5. Interactive DOM live log viewer with level filtering and simulation actions.
 */
public class Log4j2Demo {

    private static final Logger appLogger = LogManager.getLogger(Log4j2Demo.class);
    private static final Logger authLogger = LogManager.getLogger("AuthService");
    private static final Logger dbLogger = LogManager.getLogger("DatabasePool");
    private static final Logger paymentLogger = LogManager.getLogger("PaymentGateway");

    private static int requestSequence = 100;

    public static void runSimulation() {
        ThreadContext.put("env", "wasm-edge");
        ThreadContext.put("region", "us-east-1");
        ThreadContext.put("traceId", "req-8f4b-" + (++requestSequence));

        appLogger.info("Log4j2 logging engine initialized in WebAssembly runtime");
        authLogger.debug("Validating bearer authentication token for user: {}", "doug_admin");
        authLogger.info("User session established for userId: {}", "usr_49201");
        dbLogger.info("Connection leased from pool [active={}, idle={}]", 12, 8);
        paymentLogger.warn("Payment provider response delayed (latency: {} ms)", 480);
        dbLogger.error("Query timeout while scanning index on partition: {}", "sh_europe_04");
        paymentLogger.fatal("Payment gateway connection reset by peer! Triggering circuit breaker", null);

        ThreadContext.clearAll();
    }

    public static void simulateAuth() {
        ThreadContext.put("traceId", "auth-" + (++requestSequence));
        authLogger.info("Authentication request received from IP {}", "192.168.1.105");
        authLogger.debug("Checking credentials against identity store...");
        authLogger.info("User {} successfully authenticated with roles: [ADMIN, OPERATOR]", "admin_user");
        ThreadContext.clearAll();
    }

    public static void simulateDatabase() {
        ThreadContext.put("traceId", "db-" + (++requestSequence));
        dbLogger.debug("Acquiring lock on table '{}' for batch update", "orders");
        dbLogger.info("Executed batch update across {} records in {} ms", 1540, 24);
        dbLogger.info("Connection returned to pool. Health check: OK", null);
        ThreadContext.clearAll();
    }

    public static void simulateWarning() {
        ThreadContext.put("traceId", "warn-" + (++requestSequence));
        paymentLogger.warn("High memory pressure detected: {} MB heap allocated", 890);
        paymentLogger.warn("Retrying transaction tx_{} (attempt {} of {})", "9938a", 2, 3);
        ThreadContext.clearAll();
    }

    public static void simulateError() {
        ThreadContext.put("traceId", "err-" + (++requestSequence));
        dbLogger.error("Transaction rollback triggered due to optimistic lock conflict on order_{}", 88219);
        paymentLogger.fatal("Unhandled exception downstream: HTTP 503 Service Unavailable", null);
        ThreadContext.clearAll();
    }

    public static void start() {
        Element root = DOM.getElementById("log4j-app");
        if (root == null) return;

        Element card = DOM.createElement("div");
        card.setStyle("background", "#1e293b");
        card.setStyle("padding", "24px");
        card.setStyle("borderRadius", "10px");
        card.setStyle("color", "#f8fafc");
        card.setStyle("boxShadow", "0 4px 12px rgba(0,0,0,0.3)");

        // Title and description
        Element title = DOM.createElement("h3");
        title.setInnerText("Log4j2 Enterprise Logging Engine");
        title.setStyle("color", "#38bdf8");
        title.setStyle("marginBottom", "8px");
        card.appendChild(title);

        Element desc = DOM.createElement("p");
        desc.setInnerText("Structured Log4j2 logging in WebAssembly with Mapped Diagnostic Context (MDC), parameter substitution, and severity filtering.");
        desc.setStyle("color", "#94a3b8");
        desc.setStyle("fontSize", "13px");
        desc.setStyle("marginBottom", "16px");
        card.appendChild(desc);

        // Control buttons toolbar
        Element toolbar = DOM.createElement("div");
        toolbar.setStyle("display", "flex");
        toolbar.setStyle("flexWrap", "wrap");
        toolbar.setStyle("gap", "8px");
        toolbar.setStyle("marginBottom", "16px");

        toolbar.appendChild(createBtn("Run Full Lifecycle", "#2563eb", "runLog4jSimulation"));
        toolbar.appendChild(createBtn("Auth Event", "#059669", "simulateAuthEvent"));
        toolbar.appendChild(createBtn("DB Metrics", "#0284c7", "simulateDbEvent"));
        toolbar.appendChild(createBtn("Simulate Warning", "#d97706", "simulateWarnEvent"));
        toolbar.appendChild(createBtn("Simulate Error", "#dc2626", "simulateErrEvent"));
        toolbar.appendChild(createBtn("Clear Console", "#475569", "clearLogConsole"));

        card.appendChild(toolbar);

        // Log Console Window
        Element consoleContainer = DOM.createElement("div");
        consoleContainer.setStyle("background", "#020617");
        consoleContainer.setStyle("border", "1px solid #334155");
        consoleContainer.setStyle("borderRadius", "8px");
        consoleContainer.setStyle("padding", "12px");
        consoleContainer.setStyle("height", "260px");
        consoleContainer.setStyle("overflowY", "auto");
        consoleContainer.setStyle("fontFamily", "monospace");
        consoleContainer.setStyle("fontSize", "13px");
        consoleContainer.setAttribute("id", "log4j-console-stream");

        card.appendChild(consoleContainer);
        root.appendChild(card);

        // Hook log listener to stream entries into the UI console
        SimpleLogger.addListener((level, loggerName, formattedMessage, rawMessage) -> {
            Element stream = DOM.getElementById("log4j-console-stream");
            if (stream == null) return;

            Element entry = DOM.createElement("div");
            entry.setStyle("padding", "3px 0");
            entry.setStyle("borderBottom", "1px solid #0f172a");
            entry.setStyle("display", "flex");
            entry.setStyle("gap", "8px");
            entry.setStyle("alignItems", "center");

            Element badge = DOM.createElement("span");
            badge.setInnerText("[" + level.name() + "]");
            badge.setStyle("fontWeight", "bold");
            badge.setStyle("fontSize", "11px");
            badge.setStyle("padding", "2px 6px");
            badge.setStyle("borderRadius", "4px");

            if (level.equals(Level.FATAL)) {
                badge.setStyle("background", "#7f1d1d");
                badge.setStyle("color", "#fecaca");
            } else if (level.equals(Level.ERROR)) {
                badge.setStyle("background", "#991b1b");
                badge.setStyle("color", "#fee2e2");
            } else if (level.equals(Level.WARN)) {
                badge.setStyle("background", "#78350f");
                badge.setStyle("color", "#fef3c7");
            } else if (level.equals(Level.INFO)) {
                badge.setStyle("background", "#064e3b");
                badge.setStyle("color", "#a7f3d0");
            } else {
                badge.setStyle("background", "#1e1b4b");
                badge.setStyle("color", "#c7d2fe");
            }

            Element text = DOM.createElement("span");
            text.setInnerText(loggerName + " - " + rawMessage);
            text.setStyle("color", "#e2e8f0");

            entry.appendChild(badge);
            entry.appendChild(text);
            stream.appendChild(entry);
        });

        // Run initial simulation
        runSimulation();
    }

    private static Element createBtn(String label, String bgColor, String eventName) {
        Element btn = DOM.createElement("button");
        btn.setInnerText(label);
        btn.setStyle("background", bgColor);
        btn.setStyle("color", "#ffffff");
        btn.setStyle("border", "none");
        btn.setStyle("padding", "6px 12px");
        btn.setStyle("borderRadius", "6px");
        btn.setStyle("cursor", "pointer");
        btn.setStyle("fontSize", "12px");
        btn.setStyle("fontWeight", "500");
        btn.addEventListener("click", eventName);
        return btn;
    }

    public static void clearConsole() {
        Element stream = DOM.getElementById("log4j-console-stream");
        if (stream != null) {
            stream.setInnerText("");
        }
        SimpleLogger.clearHistory();
    }

    public static void main(String[] args) {
        runSimulation();
    }
}
