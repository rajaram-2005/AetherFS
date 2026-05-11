⚙️ Aether-FS: Anti-Gravity Edge Storage Middleware

Aether-FS is a conceptual "Anti-Gravity" middleware designed for local storage systems, particularly suited for high-frequency data logging in Industrial IoT (IIoT) and Edge computing environments (e.g., Wind Turbine telemetry).

Traditional file systems suffer from the "gravity" of mechanical or solid-state disk seek times. Aether-FS mitigates this by utilizing a Heuristic Markov Model to analyze user/system access patterns in real-time, predicting and "levitating" high-probability files into Off-Heap RAM before they are requested.

🚀 Key Features

Heuristic Markov Prediction: An embedded AI engine that builds a state-transition matrix of sequential file accesses to predict future requests with high accuracy.

Off-Heap "Levitation": Uses Java's ByteBuffer.allocateDirect() to store cached data directly in native OS memory, completely bypassing the Java Garbage Collector for ultra-low latency (0-1ms access times).

Zero-Dependency Architecture: The entire simulation, including the AI engine, REST API, and a futuristic web dashboard, is deployed from a single core Java file (AetherFSServer.java) using the native com.sun.net.httpserver.

Real-Time Telemetry UI: An embedded HTML/CSS/JS dashboard provides live metrics on Cache Hits, "Gravity" Misses, Time Saved, and the current state of Off-Heap memory.

🛠️ How to Run

Because Aether-FS is built as a zero-dependency, single-file deployment, running it is incredibly simple.

Ensure you have Java 11 or higher installed.

Open your terminal in the directory containing AetherFSServer.java.

Run the following command:

java AetherFSServer.java


Access the telemetry dashboard at: http://localhost:8080

🧠 Research Application

Author: Rajaram
This simulator serves as the foundational proof-of-concept for integrating predictive caching mechanisms into resource-constrained edge devices within the renewable energy sector, specifically targeting predictive maintenance logs for wind turbine gearboxes and generators.
