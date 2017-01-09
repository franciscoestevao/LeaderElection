/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leaderelection;


/**
 *
 * @author Francisco
 */
public class LeaderElection {

    
    public static int id = 0;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
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
        
        Node node = new Node(id);
        
        node.init();
    }
    
}
