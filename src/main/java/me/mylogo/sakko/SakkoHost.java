package me.mylogo.sakko;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dennis Heckmann on 30.06.17
 * Copyright (c) 2017 Dennis Heckmann
 */
public class SakkoHost extends Sakko {

    private ServerSocket socket;
    private SakkoHostThread hostThread;
    private final Object clientGuard = new Object(), subscriptionGuard = new Object(), directSubscriptionGuard = new Object();
    private List<SakkoClientThread> connectedClients;
    private Map<String, List<IReactToIt>> subscriptions;

    public SakkoHost() throws SakkoException {
        this(DEFAULT_PORT);
    }

    public SakkoHost(int port) throws SakkoException {
        subscriptions = new HashMap<>();
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            throw new SakkoException("Could not open SakkoHost on defined port!");
        }
        connectedClients = new ArrayList<>();
        this.hostThread = new SakkoHostThread();
    }

    public void close() throws SakkoException {
        try {
            hostThread.thread.interrupt();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new SakkoException("Could not proberly close SackoHost!");
        }
    }

    @Override
    public void publish(String channel, String message) {
        synchronized (subscriptionGuard) {
            subscriptions.computeIfAbsent(channel, s -> new ArrayList<>()).forEach(reacters -> reacters.reactTo(channel, message));
        }
    }

    @Override
    public void subscribe(String channel) {
        synchronized (subscriptionGuard) {
            subscriptions.computeIfAbsent(channel, s -> new ArrayList<>()).add(this);
        }
    }

    private void handleSocket(SakkoClientThread thread, String channel, String message) {
        if (SUBSCRIBE_CHANNEL.equals(channel)) {
            synchronized (subscriptionGuard) {
                subscriptions.computeIfAbsent(message, s -> new ArrayList<>()).add(thread);
            }
        } else {
            synchronized (subscriptionGuard) {
                List<IReactToIt> sakkoClientThreads = subscriptions.get(channel);
                if (sakkoClientThreads != null) {
                    sakkoClientThreads.forEach(reacter -> {
                        reacter.reactTo(channel, message);
                    });
                }
            }
        }
    }

    private class SakkoHostThread {
        private Thread thread;

        public SakkoHostThread() {
            this.thread = new Thread(() -> {
                while (true) {
                    try {
//                        System.out.println("Waiting for accepting...");
                        Socket accepted = socket.accept();
//                        System.out.println("Having accepted!");
                        SakkoClientThread sakkoClientThread = null;

                        try {
                            sakkoClientThread = new SakkoClientThread(accepted);
                        } catch (SakkoException e) {
                            e.printStackTrace();
                        }

                        synchronized (clientGuard) {
                            connectedClients.add(sakkoClientThread);
                        }

                    } catch (IOException e) {
                        System.err.println("Could not accept incoming Socket!");
                        e.printStackTrace();
                    }
                }
            });
            this.thread.start();
        }
    }

    public class SakkoClientThread implements IReactToIt {

        private boolean running = true;
        private Socket acceptedSocket;
        private Thread thread;
        private InputStream input;
        private OutputStream output;
        private DataInputStream dataInput;
        private DataOutputStream dataOutput;

        public SakkoClientThread(Socket acceptedSocket) throws SakkoException {
            this.acceptedSocket = acceptedSocket;
            try {
                this.input = acceptedSocket.getInputStream();
                this.dataInput = new DataInputStream(input);
            } catch (IOException e) {
                e.printStackTrace();
                throw new SakkoException("Could not open input stream of incoming SakkoClient!");
            }
            try {
                this.output = acceptedSocket.getOutputStream();
                this.dataOutput = new DataOutputStream(output);
            } catch (IOException e) {
                e.printStackTrace();
                throw new SakkoException("Could not open output stream of incoming SakkoClient!");
            }
            this.thread = new Thread(() -> {
                while (running && acceptedSocket.isConnected() && !acceptedSocket.isClosed() && acceptedSocket.isBound()) {
//                    System.out.println("IsClosed:" + acceptedSocket.isClosed() + " isBoound:" + acceptedSocket.isBound() + " is:" + acceptedSocket.isInputShutdown());
                    try {
                        String input = dataInput.readUTF();
                        handle(input, (channel, message) -> {
                            handleSocket(this, channel, message);
                        });
                    } catch (EOFException e) {
                        // This means that the socket was closed because we could not receive any more data!
                        running = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                removeMe();
            });
            thread.start();
        }

        private void removeMe() {
            System.out.println("Removing me!");
            synchronized (clientGuard) {
                connectedClients.remove(this);
            }
            synchronized (subscriptionGuard) {
                subscriptions.forEach((channel, list) -> list.remove(this));
            }
        }

        @Override
        public void reactTo(String channel, String message) {
            try {
//                System.out.println("I am sending channel:" + channel + " With msg:" + message);
                dataOutput.writeUTF(makeMessage(channel, message));
                dataOutput.flush();
            } catch (IOException e) {
                System.err.println("Could not properly write from host to client!");
                e.printStackTrace();
            }
        }
    }

}
