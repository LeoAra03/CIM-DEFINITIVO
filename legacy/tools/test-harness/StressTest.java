import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class StressTest {
    static class Result {
        int success = 0;
        int failed = 0;
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8888;
        int count = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int burstMs = args.length > 3 ? Integer.parseInt(args[3]) : 50; // delay between starts

        System.out.println("Starting stress test: host=" + host + " port=" + port + " count=" + count);

        ExecutorService exec = Executors.newFixedThreadPool(Math.min(count, 200));
        Result res = new Result();
        CountDownLatch latch = new CountDownLatch(count);
        long start = System.currentTimeMillis();
        List<String> csvLines = Collections.synchronizedList(new ArrayList<>());
        csvLines.add("idx,mac,success,latency_ms,error");

        for (int i = 0; i < count; i++) {
            final int idx = i;
            exec.submit(() -> {
                String mac = String.format("AA:AA:AA:AA:AA:%02d", (idx % 100));
                String name = (idx % 4 == 0) ? "ALMACEN" : (idx % 4 == 1) ? "CALIDAD" : (idx % 4 == 2) ? "MANUFACTURA" : "PLC";
                String uuid = "CIM-ST-TEST-" + idx;
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, port), 3000);
                    s.setSoTimeout(5000);
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
                         PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true)) {

                        String handshake = String.join(";", "CIM_MASTER_HUB_V1", name, "UBB_CIM_PRO_SECURE_2024", mac, uuid);
                        long t0 = System.currentTimeMillis();
                        w.println(handshake);
                        String resp = r.readLine();
                        long t1 = System.currentTimeMillis();
                        long latency = t1 - t0;
                        boolean ok = (resp != null && resp.contains("VALIDADO"));
                        if (ok) {
                            res.success++;
                        } else {
                            res.failed++;
                        }
                        String err = ok ? "" : (resp == null ? "null_response" : "not_validado:" + resp);
                        csvLines.add(idx + "," + mac + "," + (ok ? 1 : 0) + "," + latency + "," + err);
                        if (!ok) res.errors.add(mac + " handshake issue: " + err);

                        // Send a few heartbeats
                        for (int h = 0; h < 3; h++) {
                            Thread.sleep(200 + (idx % 5) * 20);
                            w.println("HEARTBEAT|" + System.currentTimeMillis());
                            try {
                                String ack = r.readLine();
                                if (ack == null) {
                                    res.errors.add(mac + " heartbeat ack null");
                                }
                            } catch (Exception e) {
                                res.errors.add(mac + " heartbeat read error: " + e.getMessage());
                            }
                        }

                    }
                } catch (Exception e) {
                    res.failed++;
                    res.errors.add("Error for idx=" + idx + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            Thread.sleep(burstMs);
        }

        latch.await(300, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        exec.shutdownNow();

        System.out.println("--- Stress Test Summary ---");
        System.out.println("Requested: " + count);
        System.out.println("Success: " + res.success);
        System.out.println("Failed: " + res.failed);
        System.out.println("Elapsed ms: " + elapsed);
        if (!res.errors.isEmpty()) {
            System.out.println("Errors sample (up to 20):");
            res.errors.stream().limit(20).forEach(System.out::println);
        }
        // Escribir CSV de resultados
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.File("stress_results.csv"))) {
            csvLines.forEach(pw::println);
            System.out.println("Wrote stress_results.csv (latencies and results)");
        } catch (Exception e) {
            System.out.println("Failed writing CSV: " + e.getMessage());
        }
    }
}
