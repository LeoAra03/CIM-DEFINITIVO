import java.io.*;
import java.net.*;
import java.util.*;

public class StationSimulator implements Runnable {
    private final String host;
    private final int port;
    private final String stationName;
    private final String password;
    private final String uuid;
    private final String mac;

    public StationSimulator(String host, int port, String stationName, String password, String uuid, String mac) {
        this.host = host;
        this.port = port;
        this.stationName = stationName;
        this.password = password;
        this.uuid = uuid;
        this.mac = mac;
    }

    @Override
    public void run() {
        try (Socket s = new Socket(host, port);
             BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true)) {

            String handshake = String.join(";", "CIM_MASTER_HUB_V1", stationName, password, mac, uuid);
            System.out.println("[" + mac + "] Connecting and sending handshake: " + handshake);
            w.println(handshake);

            String resp = r.readLine();
            System.out.println("[" + mac + "] Received: " + resp);

            // Keep connection alive and send a few heartbeats
            for (int i = 0; i < 5; i++) {
                Thread.sleep(2000);
                String hb = "HEARTBEAT|" + System.currentTimeMillis();
                w.println(hb);
                String ack = r.readLine();
                System.out.println("[" + mac + "] Heartbeat ack: " + ack);
            }

            System.out.println("[" + mac + "] Finished, closing socket.");
        } catch (Exception e) {
            System.out.println("[" + mac + "] Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8888;
        List<Thread> threads = new ArrayList<>();

        String[] macs = new String[] {"AA:AA:AA:AA:AA:01","AA:AA:AA:AA:AA:02","AA:AA:AA:AA:AA:03","AA:AA:AA:AA:AA:04"};
        String[] names = new String[] {"ALMACEN","CALIDAD","MANUFACTURA","PLC"};

        for (int i = 0; i < macs.length; i++) {
            StationSimulator sim = new StationSimulator(host, port, names[i], "UBB_CIM_PRO_SECURE_2024", "CIM-ST-TEST-"+i, macs[i]);
            Thread t = new Thread(sim);
            threads.add(t);
            t.start();
            Thread.sleep(300);
        }

        for (Thread t : threads) t.join();
        System.out.println("All stations finished.");
    }
}
