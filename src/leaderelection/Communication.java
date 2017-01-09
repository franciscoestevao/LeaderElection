/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leaderelection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;




/**
 *
 * @author Francisco
 *
 * Trata de tudo o que tem a ver com grupo multicast, envio e recepção de
 * mensagens
 *
 */
public final class Communication implements Runnable {

    private int mcPort;
    public boolean running;
    private String mcIPStr;
    private InetAddress mcIPAddress;
    private MulticastSocket mcSocket;
    int id;


    
    public Communication() {
        
        this.id = LeaderElection.id;
        this.running = false;

        // Porta a escutar para a comunicação
        this.mcPort = 12345;
        
        // IP do grupo multicast ao qual o processo se junta
        this.mcIPStr = "225.4.5.6";
        
        Thread.currentThread().setName("Communication Thread");

        
        start();

        
    }
    
    public void start(){
        try {
            this.mcSocket = new MulticastSocket(mcPort);
        } catch (IOException ex) {
            Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Erro ao criar Socket Multicast");
            System.exit(1);
        }

        
        try {
            // Transforma string com o IP para variavel do tipo InetAddress
            this.mcIPAddress = InetAddress.getByName(mcIPStr);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Erro ao obter objecto InetAddress");
            System.exit(1);
        }

        // System.out.println("Running on: " + mcSocket.getLocalSocketAddress());

        try {
            // Faz join ao grupo multicast
            mcSocket.joinGroup(mcIPAddress);
            running = true;
        } catch (IOException ex) {
            Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Erro ao fazer join ao grupo Multicast");
            System.exit(1);
        }
        
    }
    
    /**
     * Fecha todas as sockets
     * @throws IOException 
     */
    void close() throws IOException {
        if (running){
            mcSocket.leaveGroup(mcIPAddress);
            mcSocket.close();
            running = false;
        }

    }
    
    
    @Override
    public void run() {
    
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

        if(!running){
            start();
        }
        
        /**
        * Fica à escuta até receber mensagem multicast, devolve a mensagem recebida
        * 
        */
        while (running){
            try {
                mcSocket.receive(packet);
            } catch (IOException ex) {
                Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println("Erro ao receber pacote UDP");
            }
            String msg = new String(packet.getData(), packet.getOffset(),
                    packet.getLength());            
            
            Node.processMessage(msg);

        }
    }

    /**
     * Cria Thread e Envia pacote UDP contendo mensagem
     * @param mensagem 
     * @param id_dst s
     */
    public void send(String mensagem, int id_dst) {
        
        new Thread() {
            
            @Override
            public void run() {

                String toSend = id + "::" + id_dst + "::" + mensagem;

                byte[] msg = toSend.getBytes();
                DatagramPacket packet = new DatagramPacket(msg, msg.length);
                packet.setAddress(mcIPAddress);
                packet.setPort(mcPort);
                try {
                    mcSocket.send(packet);
                    Thread.currentThread().interrupt();
                } catch (IOException ex) {
                    Logger.getLogger(Communication.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Erro ao enviar pacote UDP");
                }

                // System.out.println("Sent: " + mensagem);
            }
        }.start();

    }
    
    
    public void broadcast (String mensagem){
        this.send(mensagem, 0);
    }
   
}
