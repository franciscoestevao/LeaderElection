/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leaderelection;

import java.util.Random;


/**
 *
 * @author Francisco
 */
public class LeaderElection {

    
    public static int   id      = 0;
    static Random       DELAY   = new Random();
    
    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here
        
        
        if (args.length==0){
            System.out.println("Usage: java -jar path ID");
            System.exit(1);
        }
        
        else{
            
            new TimerNode().start();
            
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
        
        Thread.sleep((DELAY.nextInt(3000) + 1));
        
        Node node = new Node(id);
        
        node.init();
    }
    
}
