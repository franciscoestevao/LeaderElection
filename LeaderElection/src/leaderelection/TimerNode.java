/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leaderelection;

/**
 *
 * @author Diogo Ribeiro Sousa
 */
public class TimerNode extends Thread {
    public static int[] timer;
    int MAX_USERS = Node.MAX_USERS;

    public TimerNode(){
        timer = new int[MAX_USERS];
    }

    @Override
    public void run(){

        try {
            while(true){
                for(int i=0; i<MAX_USERS; i++){ //colocar max_users
                    timer[i]--;
                }
                Thread.sleep(100);
            }
        } catch(InterruptedException e) {
            System.out.println("sleep interrupted");      
        }
    }
}
