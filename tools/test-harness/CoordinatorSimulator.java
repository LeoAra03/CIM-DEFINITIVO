import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class CoordinatorSimulator {
    private final int port;
    private final ServerSocket server;
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private final ConcurrentMap<String, Socket> map = new ConcurrentHashMap<>();

    public CoordinatorSimulator(int port) throws IOException {
        this.port = port;
        this.server = new ServerSocket(port);
    }

    public void start() {
        System.out.println("CoordinatorSimulator listening on port " + port);
        exec.submit(() -> {
            while (!server.isClosed()) {
                try {
                    Socket s = server.accept();
                    exec.submit(() -> handleClient(s));
                } catch (IOException e) {
                    break;
                }
            }
        });
    }

    private void handleClient(Socket s) {
        String remote = s.getRemoteSocketAddress().toString();
        System.out.println("Client connected: " + remote);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true)) {

            String line;
            while ((line = r.readLine()) != null) {
                System.out.println("< " + remote + " : " + line);
                if (line.startsWith("CIM_MASTER_HUB_V1")) {
                    // Format: CIM_MASTER_HUB_V1;NOMBRE_ESTACION;PASSWORD;MAC_DISPOSITIVO;UUID_STATION
                    String[] toks = line.split(";");
                    String station = toks.length > 1 ? toks[1] : "UNKNOWN";
                    String mac = toks.length > 3 ? toks[3] : "00:00:00:00:00:00";
                    map.put(mac, s);
                    // Simple policy: accept if password matches or always accept for tests
                    w.println("VALIDADO");
                    System.out.println("> Sent VALIDADO to " + mac);
                } else if (line.startsWith("HEARTBEAT|")) {
                    // ignore for now
                    w.println("OK");
                } else {
                    // generic echo
                    w.println("ACK");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + remote);
        }
    }

    public void stop() throws IOException {
        server.close();
        exec.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        int port = 8888;
        CoordinatorSimulator sim = new CoordinatorSimulator(port);
        sim.start();

        System.out.println("Coordinator simulator running. Press Enter to stop...");
        System.in.read();
        sim.stop();
        System.out.println("Stopped.");
    }
}
