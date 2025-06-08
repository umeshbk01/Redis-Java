package com.java.redis.internal.network;

import com.java.redis.internal.command.CommandExecutor;
import com.java.redis.internal.protocol.Command;
import com.java.redis.internal.protocol.RedisReply;
import com.java.redis.internal.protocol.RespParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BasicSocketServer implements NetworkServer{
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    void handleClient(Socket socketClient, CommandExecutor executor) {
        try(InputStream in = socketClient.getInputStream();
            OutputStream out = socketClient.getOutputStream()) {
            RespParser parser = new RespParser(in);
            while (true){
                Command cmd = parser.parse();
                System.out.println("Command received: " + cmd);
                RedisReply reply = executor.execute(cmd);
                out.write(reply.toBytes());
            }
        } catch (IOException ioe) {
            System.err.println("Error handling client: " + ioe.getMessage());
        } finally {
            try {
                socketClient.close();
            } catch (IOException ioe) {
                System.err.println("Error closing client socket: " + ioe.getMessage());
            }
        }
    }

    @Override
    public void start(int port, CommandExecutor executor) throws Exception {
        serverSocket =new ServerSocket(port);
        running = true;
        System.out.println("Server started on port: " + port);
        while (running){
            final Socket socketClient = serverSocket.accept();
            System.out.println("Client connected: " + socketClient.getRemoteSocketAddress());

            // Handle each client in its own thread
            new Thread(() -> handleClient(socketClient, executor)).start();
        }
    }

    @Override
    public void stop() throws Exception {
        running = false;
        if(serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            System.out.println("Server stopped.");
        }
    }
}
