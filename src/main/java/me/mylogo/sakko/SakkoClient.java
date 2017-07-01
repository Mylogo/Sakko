package me.mylogo.sakko;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by Dennis Heckmann on 30.06.17
 * Copyright (c) 2017 Dennis Heckmann
 */
public class SakkoClient extends Sakko {

    private Socket socket;
    private SakkoClientThread clientThread;

    public SakkoClient() throws SakkoException {
        this(DEFAULT_PORT);
    }

    public SakkoClient(int port) throws SakkoException {
        this(DEFAULT_HOST, port);
    }

    public SakkoClient(String host, int port) throws SakkoException {
        super(port);
        try {
            socket = new Socket(host, port);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SakkoException("Could not connect to host!");
        }
        clientThread = new SakkoClientThread();
    }

    public boolean isConnectionOpened() {
        return !socket.isClosed() && socket.isBound();
    }

    public void close() throws SakkoException {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new SakkoException("Could not proberly close SackoClient!");
        }
    }

    @Override
    public void publish(String channel, String message) {
        try {
            clientThread.dataOutput.writeUTF(makeMessage(channel, message));
            clientThread.dataOutput.flush();
        } catch (IOException e) {
            System.err.println("Failed to write to the output!");
            e.printStackTrace();
        }
    }

    @Override
    public void subscribe(String channel) {
        publish(SUBSCRIBE_CHANNEL, channel);
    }

    private class SakkoClientThread {
        private Thread thread;
        private DataInputStream dataInput;
        private DataOutputStream dataOutput;

        public SakkoClientThread() throws SakkoException {
            try {
                dataInput = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                throw new SakkoException("Could not open input stream!");
            }
            try {
                dataOutput = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                throw new SakkoException("Could not open output stream!");
            }
            thread = new Thread(() -> {
                while (!socket.isClosed() && socket.isBound()) {
                    try {
                        String received = dataInput.readUTF();
                        handle(received, SakkoClient.this::reactTo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

}
