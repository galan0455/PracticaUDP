package client;

import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainClient2 {
    private static DatagramSocket socketCliente;
    private static InetAddress direccionServidor;
    private static final int PUERTO = 6001;
    private static String nombre;
    private static JTextArea textArea;
    private static JTextField textField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainClient2::crearInterfaz);
    }

    private static void crearInterfaz() {
        JFrame frame = new JFrame("Cliente Chat");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        textField = new JTextField();
        frame.add(textField, BorderLayout.SOUTH);

        textField.addActionListener(e -> enviarMensaje(textField.getText()));

        frame.setVisible(true);

        conectarServidor();
    }

    private static void conectarServidor() {
        try {
            socketCliente = new DatagramSocket();
            direccionServidor = InetAddress.getByName("localhost");

            while (true) {
                nombre = JOptionPane.showInputDialog("Introduce tu nombre:");
                if (nombre == null || nombre.trim().isEmpty()) {
                    continue;
                }
                String mensajeEnvio = nombre + ":CONEXION";
                DatagramPacket packet = new DatagramPacket(mensajeEnvio.getBytes(), mensajeEnvio.length(), direccionServidor, PUERTO);
                socketCliente.send(packet);

                byte[] buffer = new byte[1024];
                DatagramPacket packetRespuesta = new DatagramPacket(buffer, buffer.length);
                socketCliente.receive(packetRespuesta);
                String respuesta = new String(packetRespuesta.getData(), 0, packetRespuesta.getLength());
                textArea.append(respuesta + "\n");

                if (respuesta.startsWith("Conectado")) {
                    new Thread(MainClient2::recibirMensajes).start();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recibirMensajes() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socketCliente.receive(packet);
                String mensajeRecibido = new String(packet.getData(), 0, packet.getLength());
                textArea.append(mensajeRecibido + "\n");
            }
        } catch (Exception e) {
            textArea.append("Desconectado del servidor.\n");
        }
    }

    private static void enviarMensaje(String texto) {
        try {
            if (texto.equalsIgnoreCase("salir")) {
                String mensajeSalida = nombre + ":DESCONECTAR";
                DatagramPacket packet = new DatagramPacket(mensajeSalida.getBytes(), mensajeSalida.length(), direccionServidor, PUERTO);
                socketCliente.send(packet);
                System.exit(0);
            } else {
                String mensajeEnvio = nombre + ": " + texto;
                DatagramPacket packet = new DatagramPacket(mensajeEnvio.getBytes(), mensajeEnvio.length(), direccionServidor, PUERTO);
                socketCliente.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
