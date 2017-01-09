/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leaderelection;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Diogo Ribeiro Sousa
 * Código da Figura 2 do artigo [1].
 * Está implementada a Task1 do pseudo-código que retorna a mensagem a enviar do
 * tipo String.
  */

public class Node {
    public static final int     HEARTBEAT       =    0,                                
                                STOP_LEADER     =    1,
                                SUSPICION       =    2,
            
                                DONT_CARE   =    0,
                                TIMER           =  200,
                                MAX_USERS       = 1000,
            
                                // time units to repeat while(leader() == id), em ms
                                TIME_UNITS      = 2000;
    
    
    private static final String FAILURE         = "ERROR";
    
        
    static boolean      DEBUG = false;
    
    
    static int id;
    static int suspLevel[];
    static int hbc;
    static List<Integer> contenders = new ArrayList();
    static List<Integer> members = new ArrayList();
    static int lastStopLeader[];
    static int tagID;
    static int silent;
    static int timer[];
    static int timeout[];
    static boolean nextPeriod;
    static int mTag, mID, mSuspLevel, mSilent, mHbc;
    
    public static ReentrantLock lock;
    private static final Queue queue = new LinkedList();
    public static Communication link;
    
    public Node(int id) {
        
        suspLevel       = new int[MAX_USERS];
        suspLevel[id]   = id;
        contenders      = new ArrayList();
        members         = new ArrayList();
        lastStopLeader  = new int[MAX_USERS];
        

            
            timer       = new int[MAX_USERS];
            timeout     = new int[MAX_USERS];
            
            for(int i=0; i < MAX_USERS; i++)
                timer[i] = TIMER;
            
        
        
        contenders.add(id);
        members.add(id);
        
        Node.id     = id;
        hbc         = 0;
        silent      = DONT_CARE;
        nextPeriod  = false;  
        Node.lock   = new ReentrantLock();
        link        = new Communication();
    }
    
    public void init(){
        
        Thread rec = new Thread(link);
        rec.start();
        
        if (DEBUG)
            System.out.println("Começou Thread Communication");
        
        //começa Thread em que espera por nova mensagem
        newMessage();
        
        // faz Task T1 para sempre
        taskT1();
    }
    
    /**
     * Quando a Thread Communication recebe uma mensagem, chama este método para processar o pacote UDP
     * @param msg
     */
    public static void processMessage(String msg){
        
        // Nao sei se é suposto criar nova thread aqui        
        new Thread()
        {
            @Override
            public void run() {
                
                Thread.currentThread().setName("Process Message Thread " + Thread.currentThread().getId());

                lock.lock();
                try {
                    queue.add(msg);
                    
                    if (DEBUG)
                        System.out.println("Message added to the queue: " + queue.toString());                    
                    
                } finally {
                    lock.unlock();
                }
            
            }
            
            
        }.start();
        
    }
    
    /**
     * Método chamado pela thread newMessage receber e retirar a ultima mensagem da queue
     * @return String[] depois do split
     */
    public static String[] processQueue(){

        String[] toProcess ;
        int id_src;
        int id_dst;
        String msg;
        
        while(true) {
            
            while( true )
            {
                lock.lock();
                try { 
                    if(queue != null && !queue.isEmpty()) {
                        
                        if (DEBUG)
                            System.out.println("Mensagem removida da queue");
                        
                        msg = queue.remove().toString();
                        break;
                    }
                } finally {
                    lock.unlock();
                }

               
            }              
                
            toProcess = msg.split("::");
            id_src = Integer.parseInt(toProcess[0]);
            id_dst = Integer.parseInt(toProcess[1]);
            
            // se packet estiver vazio ou se tiver sido eu a mandar a mensagem, ignorar
            if(toProcess == null || id_src == id)
                break;

            
            // Se for para o processo ou broadcast
            if(id_dst == 0 || id_dst == Node.id){
                System.out.println("[Multicast Receiver] Received from " + id_src + ": " + toProcess[2]);
               // System.out.println("Returning" + msg); 
                //System.out.println("Queue actualizada: " + queue.toString());
                return toProcess;
            }             
                            
        }
        
    
        return null;


    }
    
    /**
     * Segundo o artigo:
 return (l such that (-,l) = lex_min({(susp_level_i[kID],kID)}_j pertence a contenders_i))
 lex_min(X) returns the smallest pair in X according to lexicographical order
 
 Por outras palavras, retorna o id do processo (pertencente aos contenders) que tem o susp_level menor
     * 
     * @return leader id
     */
    public int leader(){
        
        int i=0;
        
       // Will contain the smallest suspLevel value. Starts with suspLevel of myself
        int leastSusp = 99999;//suspLevel[contenders.get(i)];
        
        // Will contain the the id of the process with the smallest susp_level value. Starts with my id
        int leastSuspId = 9999;//contenders.get(i);
        
        if (!contenders.isEmpty()){

            for(i=0; i<contenders.size(); i++){
                
                int iID = contenders.get(i);
                
                // if contenders[i] == leastSusp
                //System.out.println("Retrieving contenders"+i+" = "+k);
                if(contenders.contains(iID) && iID != -1){
                    if (suspLevel[iID] < leastSusp){

                        //leastSusp = suspLevel[contenders[i]]
                        leastSusp = suspLevel[iID];

                        // leastSuspId = contenders[i]
                        leastSuspId = iID;

                        //System.out.println("suspLevel "+contenders.get(i)+":"+suspLevel[contenders.get(i)]);

                    }
                }
            }

        }
        //System.out.println("Leader should be: "+leastSuspId);
        return leastSuspId;
        
    }
    
    /**
     * Task T1 a repetir para sempre
     */
    public void taskT1() {
        
        String message;
        
        // repeat forever
        while(true){
            
            nextPeriod = false;
                
            checkTimeouts();
            
            /*for(int u=0; u<contenders.size(); u++){
                System.out.println("suspLevel "+contenders.get(u)+":"+suspLevel[contenders.get(u)]);
            }*/
            
            // System.out.println("leader is: " + leader());
            // System.out.println("time1: "+TimerNode.timer[1]);
             
            while(leader() == id){
            System.out.println("Contenders:::: " + contenders);
            //System.out.println("LeastSusp:::: "+leastSusp+" -- "+leastSuspId);
                System.out.println("[Node " + id + "] I am the leader");
                
                if(!nextPeriod){
                    nextPeriod = true;
                    hbc++;
                }

                tagID = HEARTBEAT;
                silent = DONT_CARE;
                
                message = Integer.toString(tagID) + "," + Integer.toString(id)+ "," + 
                            Integer.toString(suspLevel[id])+ "," + Integer.toString(silent) + "," + 
                            Integer.toString(hbc);

                System.out.println("[Node " + id + "] Sent HEARTBEAT");
                Node.link.broadcast(message);
                

                // Este sleep é o equivalente a fazer esta parte periodicamente
                // como está numa thread nova, nao há problemas de bloqueio
                try {
                    Thread.sleep(TIME_UNITS);                    
                } catch (InterruptedException ex) {
                    Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if(nextPeriod){
                tagID = STOP_LEADER;
                silent = DONT_CARE;
                message = Integer.toString(tagID) + "," + Integer.toString(id)+ "," + 
                      Integer.toString(suspLevel[id]) + "," + Integer.toString(silent) +
                      "," + Integer.toString(hbc);
                System.out.println("[Node " + id + "] Sent STOP LEADER");
                link.broadcast(message);
                
            }
            
            
              
        }
    }
    
    public void checkTimeouts(){
        
        
      
        String message;
        
        
        for(int k=0; k < contenders.size(); k++){ /* CHECK FOR TIMEOUTS */
            
            int kID = contenders.get(k);
            
            if(!contenders.contains(kID)){
                // The process is not a contender
                continue;
            }
            
            //System.out.println("LeastSusp:: "+leader());
            if(contenders.size()>k && k>=0)
                if(kID == id){
                    // My ID doesn't matter
                    continue;
                }
            
            if(TimerNode.timer[kID] <= 0 && contenders.contains(kID)){

                System.out.println("[Node " + id + "] Timeout of process " + kID + " expired!");
                
                Node.timeout[kID] = Node.timeout[kID]+1;
                
                message = Integer.toString(SUSPICION) + "," + Integer.toString(id) + "," + 
                        Integer.toString(suspLevel[id]) + "," + Integer.toString(kID) + "," + 
                        Integer.toString(0);
                link.broadcast(message);
                System.out.println("[Node " + id + "] Sent SUSPICION");
                if(k>=0 && k<contenders.size())
                    contenders.remove(k);
            }
        }  
        
    }
    
    /**
     * Thread que está sempre a analisar a ultima mensagem recevida
     * Espécie de máquina de estados
     */
    public static void newMessage() {
        
        
        new Thread(){
                
            @Override
            public void run(){
                
                String[] received;
                
                
                Thread.currentThread().setName("New Message Thread " + Thread.currentThread().getId());
                
                if (DEBUG)
                    System.out.println("Começou Thread newMessage");
                
                while (true){
                
                    received = processQueue();

                    if (received == null || received.length == 0)
                       continue;

                    String message = received[2];
                    int id_src = Integer.parseInt(received[0]);

                    String[] parts = message.split(",");

                    mTag            = Integer.parseInt(parts[0]);
                    mID             = Integer.parseInt(parts[1]);
                    mSuspLevel      = Integer.parseInt(parts[2]);
                    mSilent       = Integer.parseInt(parts[3]);
                    mHbc            = Integer.parseInt(parts[4]);
                    
                    if (mID !=  id_src){
                        System.out.println("ERROR: Not sure who sent this");
                        continue;
                    }
                        


                    /* Treating the message */
                    // Posição 0 de members contem o numero de membros
                    // id dos membros começa a partir de members[1]

                
                    if(!members.contains(mID)){

                        // adiciona k aos membros
                        members.add(mID);
                        System.out.println(mID + " added to members");

                        // comentei esta linha porque acho que está mal
                        suspLevel[mID] = mID;

                        // e substituí-a por esta
                        // suspLevel[mID] = 0;

                        lastStopLeader[mID] = 0;

                        
                        // ???????????? timeout[mID] = TIME_UNITS;
                        TimerNode.timer[mID] = TIMER;
                        


                    }

                    if (DEBUG){
                        System.out.println("SuspLevel que já tinha dele: " + suspLevel[id_src]);
                        System.out.println("SuspLevel que me mandou: " + mSuspLevel);
                    }
                    
                    // Linha 11
                    if(suspLevel[id_src] < mSuspLevel){
                        suspLevel[id_src] = mSuspLevel;
                        
                        if (DEBUG)
                            System.out.println("SuspLevel of " + id_src + "increased");
                        
                    }

                    /*heartbeat*/
                    if((mTag == HEARTBEAT) && (lastStopLeader[mID] < mHbc)){
                        
                        System.out.println("Received HEARTBEAT from " + mID);

                        
                            TimerNode.timer[id_src] = TIMER;
                        
                        
                        // adiciona mID ao fim da lista dos contenders
                        if(!contenders.contains(mID)){
                            contenders.add(mID);
                            
                            System.out.println(mID + " added to the contenders");
                            
                            // suspLevel[mID] = mID;       // PORQUÊ? comentei
                        }

                        continue;
                    }

                    /*stop_leader*/
                    if((mTag == STOP_LEADER) && (lastStopLeader[mID] < mHbc)){

                        System.out.println("Received STOP_LEADER from: " + mID);
                        
                        lastStopLeader[mID] = mHbc;
                        
                        
                        TimerNode.timer[id_src] = 0;

                        if(contenders.indexOf(mID)>=0 && mID > 0){
                            System.out.println("REMOVED process " + contenders.get(contenders.indexOf(mID)) + " from contenders");
                            contenders.remove(contenders.indexOf(mID));
                        }
                     
                        continue;

                    }

                    /*suspicion*/

                    if((mTag == SUSPICION) && (mSilent == id)){
                        
                        System.out.println("Received SUSPICION from: " + mID);
                        
                        if (DEBUG)
                            System.out.println("My SuspLevel increased");
                        
                        suspLevel[id] = suspLevel[id] + 1;

                    }
                }
            }
        }.start();
    }    
}
