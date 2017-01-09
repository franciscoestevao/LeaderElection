/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leaderelection;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Random;


/**
 *
 * @author Francisco
 */
public class LeaderElection {

    
    public static int                   id          = 0;
    public static Random                DELAY       = new Random();
    public static int                   n_msg       = 0;
    public static SimpleDateFormat      sdf         = new SimpleDateFormat("HH:mm:ss.SSS");
    public static Timestamp             timestamp   ;

    
    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        // TODO code application logic here
        
        
        if (args.length==0){
            System.out.println("Usage: java -jar path ID");
            System.exit(1);
        }
        
        else{
            
            
            if (args[0].matches("[0-9]+")){
                id = Integer.parseInt(args[0]);
                
                if (id==0){
                    System.out.println("ID must be higher than 0");
                    System.out.println("Usage: java -jar path ID");
                    System.exit(1);
                }
         
                System.out.println("Process ID: " + id);
            }
            
            else{
                System.out.println("ID must be integer");
                System.out.println("Usage: java -jar path ID");
                System.exit(1);
            }
        }
        
        // Descomentar para imprimir para ficheiro (executar como admin)
//        PrintStream out = new PrintStream(new FileOutputStream("node" + id + ".txt"));
//        System.setOut(out);


        

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                
                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Numero total de mensagens: " + n_msg);
                
            }
        });
        
        Thread.sleep((DELAY.nextInt(3000) + 1));
        
        
        new TimerNode().start();
        
        Node node = new Node(id);
        
        node.init();
    }
    
}
