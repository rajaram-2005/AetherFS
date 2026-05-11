// IMPORTANT: Save this file EXACTLY as AetherFSServer.java
// To run: java AetherFSServer.java

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AetherFSServer {

    // --- CORE AETHER-FS ENGINE ---

    /**
     * The Virtual Disk represents traditional storage with high "Gravity" (seek times).
     */
    static class VirtualDisk {
        private final Map<String, byte[]> storage = new HashMap<>();

        public VirtualDisk() {
            // Simulating files typical in an industrial IoT / Turbine environment
            storage.put("sys_boot.bin", "BOOT_SEQUENCE_INITIALIZED...".getBytes());
            storage.put("sensor_cfg.xml", "<config><rate>100hz</rate></config>".getBytes());
            storage.put("vibration_log.dat", "VIB_DATA:0.4,0.5,0.4,0.6...".getBytes());
            storage.put("ml_model.onnx", "TENSOR_WEIGHTS_BINARY_DATA...".getBytes());
            storage.put("fault_report.txt", "MAINTENANCE_REQUIRED: GEARBOX_WEAR".getBytes());
        }

        public byte[] readWithGravity(String filename) {
            try {
                // Simulating heavy disk seek time (Gravity: 200ms)
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return storage.getOrDefault(filename, "FILE_NOT_FOUND".getBytes());
        }
        
        public Set<String> getAvailableFiles() {
            return storage.keySet();
        }
    }

    /**
     * Anti-Gravity Memory: Uses DirectByteBuffers to store data Off-Heap.
     * This completely bypasses the Java Garbage Collector for ultra-low latency.
     */
    static class OffHeapCache {
        private final Map<String, ByteBuffer> levitatedFiles = new ConcurrentHashMap<>();

        public void levitate(String filename, byte[] data) {
            // Allocate memory strictly outside the JVM heap
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
            buffer.put(data);
            buffer.flip(); // Prepare for reading
            levitatedFiles.put(filename, buffer);
            System.out.println("[AETHER] Levitated into Off-Heap RAM: " + filename);
        }

        public String readLevitated(String filename) {
            ByteBuffer buffer = levitatedFiles.get(filename);
            if (buffer == null) return null;
            
            // Simulating instant RAM access (Anti-Gravity: ~1ms)
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            buffer.rewind(); // Reset position for future reads
            return new String(data);
        }

        public void drop(String filename) {
            levitatedFiles.remove(filename);
        }
        
        public boolean isLevitating(String filename) {
            return levitatedFiles.containsKey(filename);
        }
    }

    /**
     * The Heuristic Markov Model analyzes sequential access to predict future reads.
     */
    static class MarkovPredictor {
        private final Map<String, Map<String, Integer>> transitionMatrix = new ConcurrentHashMap<>();
        private String lastAccessedFile = null;

        public void recordAccess(String currentFile) {
            if (lastAccessedFile != null) {
                transitionMatrix.putIfAbsent(lastAccessedFile, new ConcurrentHashMap<>());
                Map<String, Integer> edges = transitionMatrix.get(lastAccessedFile);
                edges.put(currentFile, edges.getOrDefault(currentFile, 0) + 1);
            }
            lastAccessedFile = currentFile;
        }

        public String predictNext() {
            if (lastAccessedFile == null || !transitionMatrix.containsKey(lastAccessedFile)) return null;

            Map<String, Integer> edges = transitionMatrix.get(lastAccessedFile);
            String bestPrediction = null;
            int maxFreq = -1;

            for (Map.Entry<String, Integer> entry : edges.entrySet()) {
                if (entry.getValue() > maxFreq) {
                    maxFreq = entry.getValue();
                    bestPrediction = entry.getKey();
                }
            }
            return bestPrediction;
        }
    }

    // --- SYSTEM INITIALIZATION ---
    
    private static final VirtualDisk disk = new VirtualDisk();
    private static final OffHeapCache aetherCache = new OffHeapCache();
    private static final MarkovPredictor aiCore = new MarkovPredictor();
    
    private static int totalRequests = 0;
    private static int cacheHits = 0;
    private static long timeSavedMs = 0;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/", new WebUIHandler());
        server.createContext("/api/read", new FileReadHandler());
        server.createContext("/api/stats", new StatsHandler());
        
        server.setExecutor(null); 
        server.start();
        
        System.out.println("=========================================");
        System.out.println("Aether-FS Middleware Online.");
        System.out.println("Anti-Gravity Engine: ACTIVE");
        System.out.println("Markov Predictor: LEARNING");
        System.out.println("Access UI: http://localhost:8080");
        System.out.println("=========================================");
    }

    // --- API HANDLERS ---

    static class WebUIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = getWebInterface();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class FileReadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String payload = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                String targetFile = extractJsonValue(payload, "filename");
                
                long startTime = System.currentTimeMillis();
                boolean wasLevitating = aetherCache.isLevitating(targetFile);
                String dataStr;
                long accessTime;

                totalRequests++;

                if (wasLevitating) {
                    dataStr = aetherCache.readLevitated(targetFile);
                    accessTime = System.currentTimeMillis() - startTime;
                    cacheHits++;
                    timeSavedMs += (200 - accessTime);
                    System.out.println("[AETHER] HIT: " + targetFile + " read in " + accessTime + "ms");
                } else {
                    byte[] data = disk.readWithGravity(targetFile);
                    dataStr = new String(data);
                    accessTime = System.currentTimeMillis() - startTime;
                    System.out.println("[DISK] MISS: " + targetFile + " suffered gravity. Read in " + accessTime + "ms");
                }

                aiCore.recordAccess(targetFile);

                String predictedNext = aiCore.predictNext();
                if (predictedNext != null && !aetherCache.isLevitating(predictedNext)) {
                    byte[] nextData = disk.readWithGravity(predictedNext);
                    aetherCache.levitate(predictedNext, nextData);
                }

                String response = String.format(
                    "{\"filename\":\"%s\", \"hit\":%b, \"accessTime\":%d, \"data\":\"%s\", \"predictedNext\":\"%s\"}",
                    targetFile, wasLevitating, accessTime, dataStr, (predictedNext == null ? "None" : predictedNext)
                );

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                StringBuilder levitatedFilesList = new StringBuilder("[");
                int count = 0;
                for(String f : disk.getAvailableFiles()) {
                    if(aetherCache.isLevitating(f)) {
                        if(count > 0) levitatedFilesList.append(",");
                        levitatedFilesList.append("\"").append(f).append("\"");
                        count++;
                    }
                }
                levitatedFilesList.append("]");

                String response = String.format(
                    "{\"totalRequests\":%d, \"cacheHits\":%d, \"timeSavedMs\":%d, \"levitating\":%s}",
                    totalRequests, cacheHits, timeSavedMs, levitatedFilesList.toString()
                );
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private static String extractJsonValue(String json, String key) {
        String searchStr = "\"" + key + "\":";
        int start = json.indexOf(searchStr);
        if (start == -1) return "";
        start += searchStr.length();
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return "";
        return json.substring(start + 1, end).trim();
    }

    // --- FUTURISTIC UI INJECTION ---
    private static String getWebInterface() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>Aether-FS Telemetry</title>");
        
        sb.append("<style>");
        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Courier New', Courier, monospace; } ");
        sb.append("body { background-color: #0d0d12; color: #a3a3c2; padding: 20px; } ");
        sb.append(".container { max-width: 1000px; margin: 0 auto; } ");
        sb.append("header { background: rgba(20, 20, 30, 0.8); border: 1px solid #4d4dff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 0 15px rgba(77, 77, 255, 0.2); } ");
        sb.append("h1 { color: #4d4dff; text-shadow: 0 0 10px #4d4dff; margin-bottom: 10px; font-size: 1.8rem; } ");
        sb.append("h2 { color: #8585e0; margin-bottom: 15px; border-bottom: 1px dashed #4d4dff; padding-bottom: 5px;} ");
        sb.append(".panel { background: #14141f; border: 1px solid #2a2a40; padding: 20px; border-radius: 8px; margin-bottom: 20px; } ");
        
        sb.append(".grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; } ");
        sb.append(".btn-group { display: flex; flex-direction: column; gap: 10px; } ");
        sb.append("button { background: #1a1a2e; border: 1px solid #4d4dff; color: #4d4dff; padding: 12px; cursor: pointer; transition: all 0.2s; font-weight: bold; border-radius: 4px; } ");
        sb.append("button:hover { background: #4d4dff; color: #fff; box-shadow: 0 0 10px #4d4dff; } ");
        
        sb.append(".stat-box { background: #0a0a0f; border: 1px solid #2a2a40; padding: 15px; border-radius: 5px; text-align: center; } ");
        sb.append(".stat-val { font-size: 2rem; color: #00ffcc; text-shadow: 0 0 5px #00ffcc; font-weight: bold; margin-top: 5px; } ");
        sb.append(".stat-val.gravity { color: #ff3366; text-shadow: 0 0 5px #ff3366; } ");
        
        sb.append(".log-terminal { background: #050508; border: 1px solid #333; padding: 15px; height: 250px; overflow-y: auto; border-radius: 4px; font-size: 0.9rem; } ");
        sb.append(".log-entry { margin-bottom: 5px; } ");
        sb.append(".log-hit { color: #00ffcc; } .log-miss { color: #ff3366; } .log-ai { color: #b366ff; } ");
        
        sb.append(".levitating-badge { display: inline-block; background: rgba(0, 255, 204, 0.1); border: 1px solid #00ffcc; color: #00ffcc; padding: 5px 10px; border-radius: 12px; margin: 5px; font-size: 0.8rem; box-shadow: 0 0 5px rgba(0,255,204,0.3); } ");
        sb.append("</style></head><body>");
        
        sb.append("<div class='container'><header>");
        sb.append("<h1>&#9881; Aether-FS Telemetry Console</h1>");
        sb.append("<p>Anti-Gravity Middleware for IoT Edge Storage. Bypassing disk seek times using Heuristic Markov Models and Off-Heap Buffer Allocation.</p>");
        sb.append("</header>");
        
        sb.append("<div class='grid-2'>");
        
        sb.append("<div class='panel'><h2>System Request Simulator</h2>");
        sb.append("<p style='margin-bottom:15px; font-size:0.9rem;'>Simulate an industrial system requesting files. Watch the Markov Model learn your patterns.</p>");
        sb.append("<div class='btn-group'>");
        sb.append("<button onclick=\"requestFile('sys_boot.bin')\">Request: sys_boot.bin</button>");
        sb.append("<button onclick=\"requestFile('sensor_cfg.xml')\">Request: sensor_cfg.xml</button>");
        sb.append("<button onclick=\"requestFile('vibration_log.dat')\">Request: vibration_log.dat</button>");
        sb.append("<button onclick=\"requestFile('ml_model.onnx')\">Request: ml_model.onnx</button>");
        sb.append("<button onclick=\"requestFile('fault_report.txt')\">Request: fault_report.txt</button>");
        sb.append("</div></div>");
        
        sb.append("<div class='panel'><h2>Core Performance Metrics</h2>");
        sb.append("<div class='grid-2' style='margin-bottom: 15px;'>");
        sb.append("<div class='stat-box'>Aether Hits<div class='stat-val' id='val-hits'>0</div></div>");
        sb.append("<div class='stat-box'>Gravity Misses<div class='stat-val gravity' id='val-misses'>0</div></div>");
        sb.append("</div>");
        sb.append("<div class='stat-box' style='margin-bottom: 15px;'>Gravity Time Saved<div class='stat-val' id='val-saved'>0 ms</div></div>");
        sb.append("<h3>Currently Levitating (Off-Heap):</h3>");
        sb.append("<div id='levitating-files' style='margin-top: 10px;'><i>None</i></div>");
        sb.append("</div>");
        
        sb.append("</div>");
        
        sb.append("<div class='panel'><h2>System Access Log</h2>");
        sb.append("<div class='log-terminal' id='terminal'></div>");
        sb.append("</div>");
        
        sb.append("</div>");
        
        sb.append("<script>");
        sb.append("function logTerminal(msg, typeClass) { ");
        sb.append("  const term = document.getElementById('terminal'); ");
        sb.append("  const time = new Date().toLocaleTimeString(); ");
        sb.append("  term.innerHTML += `<div class='log-entry'><span style='color:#555'>[${time}]</span> <span class='${typeClass}'>${msg}</span></div>`; ");
        sb.append("  term.scrollTop = term.scrollHeight; ");
        sb.append("}");
        
        sb.append("async function requestFile(filename) { ");
        sb.append("  logTerminal(`System requested file: ${filename}...`, ''); ");
        sb.append("  const res = await fetch('/api/read', { method: 'POST', body: JSON.stringify({filename: filename}) }); ");
        sb.append("  const data = await res.json(); ");
        
        sb.append("  if(data.hit) { logTerminal(`AETHER HIT: '${filename}' accessed instantly in ${data.accessTime}ms`, 'log-hit'); } ");
        sb.append("  else { logTerminal(`GRAVITY MISS: '${filename}' pulled from slow disk in ${data.accessTime}ms`, 'log-miss'); } ");
        
        sb.append("  if(data.predictedNext !== 'None') { logTerminal(`AI PREDICTION: Markov model predicts next file is '${data.predictedNext}'. Levitating to Off-Heap RAM...`, 'log-ai'); } ");
        
        sb.append("  updateStats(); ");
        sb.append("}");
        
        sb.append("async function updateStats() { ");
        sb.append("  const res = await fetch('/api/stats'); ");
        sb.append("  const stats = await res.json(); ");
        sb.append("  document.getElementById('val-hits').innerText = stats.cacheHits; ");
        sb.append("  document.getElementById('val-misses').innerText = (stats.totalRequests - stats.cacheHits); ");
        sb.append("  document.getElementById('val-saved').innerText = stats.timeSavedMs + ' ms'; ");
        
        sb.append("  const leviDiv = document.getElementById('levitating-files'); ");
        sb.append("  if(stats.levitating.length === 0) { leviDiv.innerHTML = '<i>None</i>'; } else { ");
        sb.append("    leviDiv.innerHTML = stats.levitating.map(f => `<span class='levitating-badge'>&#8593; ${f}</span>`).join(''); ");
        sb.append("  }");
        sb.append("}");
        sb.append("</script></body></html>");
        
        return sb.toString();
    }
}