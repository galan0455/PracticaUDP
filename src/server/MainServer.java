package server;

import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainServer {
    private static final Map<String, InformacionCliente> clientesConectados = new ConcurrentHashMap<>();
    private static final List<String> mensajesGuardados = new CopyOnWriteArrayList<>();
    private static final int PUERTO = 6001;
    private static JTextArea textArea;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Servidor UDP Chat");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textArea = new JTextArea();
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.setVisible(true);

        new Thread(MainServer::iniciarServidor).start();
    }

    private static void iniciarServidor() {
        try (DatagramSocket socketServidor = new DatagramSocket(PUERTO)) {
            textArea.append("Servidor UDP escuchando en el puerto " + PUERTO + "\n");
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                socketServidor.receive(paqueteRecibido);
                new Thread(() -> procesarMensaje(socketServidor, paqueteRecibido)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void procesarMensaje(DatagramSocket socketServidor, DatagramPacket paqueteRecibido) {
        try {
            String mensajeRecibido = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            String[] partes = mensajeRecibido.split(":", 2);
            if (partes.length < 2) return;

            String nombreUsuario = partes[0].trim();
            String contenidoMensaje = partes[1].trim();

            if (contenidoMensaje.equals("CONEXION")) {
                if (clientesConectados.containsKey(nombreUsuario)) {
                    enviarMensaje(socketServidor, "El nombre ya está en uso.", direccionCliente, puertoCliente);
                } else {
                    clientesConectados.put(nombreUsuario, new InformacionCliente(direccionCliente, puertoCliente));
                    enviarMensaje(socketServidor, "Conectado con éxito. Bienvenido, " + nombreUsuario, direccionCliente, puertoCliente);
                    textArea.append(nombreUsuario + " se ha conectado.\n");
                }
            } else if (contenidoMensaje.equals("DESCONECTAR")) {
                clientesConectados.remove(nombreUsuario);
                textArea.append(nombreUsuario + " se ha desconectado.\n");
            } else if (contenidoMensaje.equals("HISTORIAL")) {
                String historial = mensajesGuardados.isEmpty() ? "No hay mensajes guardados." : String.join("\n", mensajesGuardados);
                enviarMensaje(socketServidor, historial, direccionCliente, puertoCliente);
            } else {
                mensajesGuardados.add(contenidoMensaje);
                textArea.append(nombreUsuario + ": " + contenidoMensaje + "\n");

                for (Map.Entry<String, InformacionCliente> entry : clientesConectados.entrySet()) {
                    if (!entry.getKey().equals(nombreUsuario)) {
                        enviarMensaje(socketServidor, nombreUsuario + ": " + contenidoMensaje, entry.getValue().getDireccion(), entry.getValue().getPuerto());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enviarMensaje(DatagramSocket socket, String mensaje, InetAddress address, int port) {
        try {
            DatagramPacket packet = new DatagramPacket(mensaje.getBytes(), mensaje.length(), address, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class InformacionCliente {
        private final InetAddress direccion;
        private final int puerto;

        public InformacionCliente(InetAddress direccion, int puerto) {
            this.direccion = direccion;
            this.puerto = puerto;
        }

        public InetAddress getDireccion() {
            return direccion;
        }

        public int getPuerto() {
            return puerto;
        }
    }
}
