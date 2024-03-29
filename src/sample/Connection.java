package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;

public class Connection implements Runnable{

    private Socket socket = null;
    private BufferedReader is;
    public PrintStream os = null;
    private boolean isEstablished = false;
    private Controller kawaii = null;
    private String status = "fresh";
    private String username;
    private boolean isDed = false;
    private CounterClient counter;
    private String tryout;


    public Connection(String ip, int port, String username, Controller kawaii){
        try {
            socket = new Socket(ip, port);
            this.username = username;
            this.kawaii = kawaii;
            System.out.println(socket.getLocalPort() + " " + socket.getPort());
        } catch (Exception e) {
            kawaii.outputField.appendText("host "+ip+":"+port+" not found\n");
            return;
        }
        this.isEstablished = true;
    }

    public void sendMessage(String msg) throws IOException
    {
        String[] lineAr = msg.split(" ");
        //JOIN rene, 172.16.31.127:4444
        if (kawaii != null){
            if (msg!= null && msg.length()>0){
                switch (status) {
                    case "fresh": {
                        if (lineAr.length>2 && lineAr[0].equals("JOIN") && lineAr[1].length()>0){
                            username = lineAr[1].replaceAll(",","");
                        }
                        os.println(msg);
                        kawaii.outputField.appendText("<"+username+"> " + msg + "\n");
                        if (msg.equals("QUIT")) isDed = true;
                        break;
                    }
                    case "connected": {
                        os.println("DATA " + username + ": " + msg);
                        kawaii.outputField.appendText("<"+username+"> " + msg + "\n");
                        if (msg.equals("QUIT")) isDed = true;
                        break;
                    }
                }
            }
        }
        else kawaii.outputField.appendText("you are not connected\n");
        kawaii.inputField.clear();
    }

    @Override
    public void run() {
        try {
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            os = new PrintStream(socket.getOutputStream());

            String line = "";

            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("initializing a receiver on port"+socket.getLocalPort() + " " + socket.getPort());

            counter = new CounterClient(this);
            Thread counterThread = new Thread(counter);
            counterThread.setDaemon(true);
            counterThread.start();

            while ( !line.trim().equalsIgnoreCase("QUIT") && !isDed && !socket.isClosed()) {
                line = is.readLine();

                if (line!=null && line.length()>0 && !line.equals("QUIT")){
                    protocol(line);
                }

            }
            disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }

    }

    public void protocol(String line) {
        String[] lineAr = line.split(" ");
        switch (lineAr[0]){
            case "J_OK":{
                kawaii.outputField.appendText("welcome to the server, "+username+"!\n");
                status = "connected";
                break;
            }
            case "LIST": {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                kawaii.names.clear();
                for (int i = 1; i < lineAr.length; i++) {
                    if (lineAr[i].equals(username)) lineAr[i] = "   " + lineAr[i];
                    kawaii.names.add(new Name(lineAr[i]));
                }
                break;
            }
            case "J_ER":{
                String code = lineAr[1].substring(0, lineAr[1].length() - 1);
                String message = "";
                for (int i = 2 ; i < lineAr.length ; i++) message = message + " " + lineAr[i];
                kawaii.outputField.appendText("<server> ERROR: " + message + ". (code " + code + ")\n");
                break;
            }
            case "DATA":{
                String source = lineAr[1].replaceAll(":","");
                String message = "";
                for (int i = 2 ; i < lineAr.length ; i++) message = message + " " + lineAr[i];
                if (!source.equals(username)) {
                    kawaii.outputField.appendText("<" + source + "> " + message + "\n");
                } else {
                    System.out.println("you received your own message: " + message);
                }
                break;
            }
            default:{
                kawaii.outputField.appendText("      you received an unrecognised command\n              " + line + "\n");
                break;
            }
        }
    }

    public void setDed(boolean ded) {
        isDed = ded;
    }

    public boolean isDed() {
        return isDed;
    }

    public String getStatus() {
        return status;
    }

    public boolean isEstablished() {
        return isEstablished;
    }

    public void disconnect(){
        System.out.println("disconnected");
        isDed = true;
        try {
            kawaii.outputField.appendText("   >disconnected or timed out<\n");
            os.println("QUIT");
            socket.close();
            is.close();
            os.close();
            kawaii.inputField.setVisible(false);
            kawaii.sendButton.setVisible(false);
        } catch (IOException e) {e.printStackTrace();}
    }


}
