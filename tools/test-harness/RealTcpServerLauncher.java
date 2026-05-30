public class RealTcpServerLauncher {
    public static void main(String[] args) {
        int port = 8888;
        String policy = args.length > 0 ? args[0] : "PREFER_NEW";
        try {
            // Intentar cargar la clase TcpServer del paquete real
            Class<?> cls = Class.forName("com.sistema.distribuido.network.TcpServer");
            System.out.println("Found real TcpServer class - attempting to instantiate via reflection");
            Class<?> policyEnum = Class.forName("com.sistema.distribuido.network.TcpServer$CollisionPolicy");
            Object enumVal = java.lang.Enum.valueOf((Class<Enum>)policyEnum, policy);
            java.lang.reflect.Constructor<?> ctor = cls.getConstructor(int.class, policyEnum);
            Object server = ctor.newInstance(port, enumVal);
            java.lang.reflect.Method start = cls.getMethod("start");
            start.invoke(server);
            System.out.println("Real TcpServer started via reflection on port " + port);
            System.out.println("Press Enter to stop...");
            System.in.read();
            java.lang.reflect.Method stop = cls.getMethod("stop");
            stop.invoke(server);
            System.out.println("Stopped.");
        } catch (ClassNotFoundException e) {
            System.out.println("Real TcpServer class not found in classpath. Build the project and run the coordinator module instead.");
        } catch (Exception e) {
            System.out.println("Failed to start real TcpServer via reflection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
