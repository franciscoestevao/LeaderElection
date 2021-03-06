/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package leaderelection;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static leaderelection.LeaderElection.n_msg;
import static leaderelection.LeaderElection.sdf;

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
                                MAX_USERS       = 50,
            
                                // time units to repeat while(leader() == id), em ms
                                TIME_UNITS      = 100,
                                BASE_UNIT       = 100;
    
        
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
    
    
    public static int lastLeader;
    
    public Node(int id) {
        
        suspLevel       = new int[MAX_USERS];
        suspLevel[id]   = id;
        contenders      = new ArrayList();
        members         = new ArrayList();
        lastStopLeader  = new int[MAX_USERS];
        

            
            timer       = new int[MAX_USERS];
            timeout     = new int[MAX_USERS];
            
            for(int i=0; i < MAX_USERS; i++)
                timer[i] = TIME_UNITS;
            
        
        
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
        
                lock.lock();
                try {
                    queue.add(msg);
                    
                    if (DEBUG)
                        System.out.println("Message added to the queue: " + queue.toString());                    
                    
                } finally {
                    lock.unlock();
                }

        
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
                if (DEBUG)
                    System.out.println("[Multicast Receiver] Received from " + id_src + ": " + toProcess[2]);
               
                return toProcess;
            }             
                            
        }
        
    
        return null;


    }
    
    /**
     * Segundo o artigo:
     * return (l such that (-,l) = lex_min({(susp_level_i[kID],kID)}_j pertence a contenders_i))
     * lex_min(X) returns the smallest pair in X according to lexicographical order
     * 
     * Por outras palavras, retorna o id do processo (pertencente aos contenders) que tem o susp_level menor
     * 
     * @return leader id
     */
    public static int leader(){
        
        int i;
        
       // Will contain the smallest suspLevel value. Starts with suspLevel of myself
        int leastSusp = 99999;
        
        // Will contain the the id of the process with the smallest susp_level value. Starts with my id
        int leastSuspId = 9999;
        
        if (!contenders.isEmpty()){

            for(i=0; i<contenders.size(); i++){
                int iID = 0;
                while(true){
                    try {
                        iID = contenders.get(i);
                    } catch (NullPointerException | IndexOutOfBoundsException ne){
                        continue;
                    }
                    break;
                }
                   

                    if(contenders.contains(iID) && iID != -1){
                        if (suspLevel[iID] < leastSusp){

                            //leastSusp = suspLevel[contenders[i]]
                            leastSusp = suspLevel[iID];

                            // leastSuspId = contenders[i]
                            leastSuspId = iID;
                            
                        }
                    }
            }

        }
        
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
             
            while(leader() == id){
                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Contenders: " + contenders);
                //System.out.println("LeastSusp:::: "+leastSusp+" -- "+leastSuspId);
                
                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] I am the leader");
                // System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Mensagens trocadas até agora: " + n_msg);
                
                
                if(!nextPeriod){
                    nextPeriod = true;
                    hbc++;
                }

                tagID = HEARTBEAT;
                silent = DONT_CARE;
                
                message = Integer.toString(tagID) + "," + Integer.toString(id)+ "," + 
                            Integer.toString(suspLevel[id])+ "," + Integer.toString(silent) + "," + 
                            Integer.toString(hbc);

                
                Node.link.broadcast(message);
                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Sent HEARTBEAT");
                n_msg++;
                
                // Este sleep é o equivalente a fazer esta parte periodicamente
                // como está numa thread nova, nao há problemas de bloqueio
                try {
                    Thread.sleep(TIME_UNITS * BASE_UNIT);                    
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
                
                link.broadcast(message);
                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Sent STOP LEADER");
                n_msg++;
                
                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] I am not the leader anymore");
                
            }
            
            
              
        }
    }
    
    public void checkTimeouts(){
        
        
      
        String message;
        
        
        for(int k=0; k < contenders.size(); k++){ /* CHECK FOR TIMEOUTS */
            
            int kID = 0;
            
            while(true){
                try {
                    kID = contenders.get(k);
                } catch (NullPointerException | IndexOutOfBoundsException ne){
                    continue;
                }
                
                break;
            }
            
            
            if(!contenders.contains(kID)){
                // The process is not a contender
                continue;
            }
           
            if(contenders.size()>k && k>=0)
                if(kID == id){
                    // My ID doesn't matter
                    continue;
                }
            
            if(TimerNode.timer[kID] <= 0 && contenders.contains(kID)){

                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Timeout of process " + kID + " expired!");
                
                Node.timeout[kID] = Node.timeout[kID]+1;
                
                message = Integer.toString(SUSPICION) + "," + Integer.toString(id) + "," + 
                        Integer.toString(suspLevel[id]) + "," + Integer.toString(kID) + "," + 
                        Integer.toString(0);
                
                link.broadcast(message);
                System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Sent SUSPICION to " + kID);
                n_msg++;
               
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
                    mSilent         = Integer.parseInt(parts[3]);
                    mHbc            = Integer.parseInt(parts[4]);
                    
                    if (mID !=  id_src){
                        System.out.println("ERROR: Not sure who sent this");
                        continue;
                    }
                        
                    n_msg++;
                  

                    /* Treating the message */
                    // Posição 0 de members contem o numero de membros
                    // id dos membros começa a partir de members[1]

                
                    if(!members.contains(mID)){

                        // adiciona k aos membros
                        members.add(mID);
                        System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Added process " + mID + " to members");
                        
                        suspLevel[mID] = mID;

                        lastStopLeader[mID] = 0;

                        timeout[mID] = TIME_UNITS;
                        
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
                        
                        System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Received HEARTBEAT from process " + mID);

                        // Set timer to timeout (linha 13)
                        TimerNode.timer[mID] = timeout[mID];
                        
                        
                        // adiciona mID ao fim da lista dos contenders
                        if(!contenders.contains(mID)){
                            contenders.add(mID);
                            
                            System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Process " + mID + " proclaims leadership and was added to contenders!");
                            
                            // suspLevel[mID] = mID;       // PORQUÊ? comentei
                        }
                        if (lastLeader != leader()){
                            System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Process " + leader() + " is the leader");
                            lastLeader = leader();
                            System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Mensagens trocadas até agora: " + n_msg);
                        }
                        
                        continue;
                    }

                    /*stop_leader*/
                    if((mTag == STOP_LEADER) && (lastStopLeader[mID] < mHbc)){

                        System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Received STOP_LEADER from process " + mID);
                        
                        lastStopLeader[mID] = mHbc;

                        if(contenders.indexOf(mID)>=0 && mID > 0){
                            System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Removed process " + contenders.get(contenders.indexOf(mID)) + " from contenders");
                                Node.contenders.remove(Node.contenders.get(Node.contenders.indexOf(mID)));
                            
                        }
                     
                        continue;

                    }

                    /*suspicion*/

                    if((mTag == SUSPICION) && (mSilent == id)){
                        
                        System.out.println("[Node " + id + " - " + sdf.format(new Timestamp(System.currentTimeMillis())) + "] Received SUSPICION from process " + mID);
                        
                        if (DEBUG)
                            System.out.println("My SuspLevel increased");
                        
                        suspLevel[id] = suspLevel[id] + 1;

                    }
                }
            }
        }.start();
    }    
}
