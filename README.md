# Sakko
Sakko (German, translated: "Jacket");

A simple Server/Client API based on channels

### Usage:
```Java
SakkoHost host = new SakkoHost(); // uses port 28080 by default
SakkoClient client = new SakkoClient(); // uses host "localhost" and the above port by default

// While instantiating, the host automatically creates a ServerSocket and listens for clients.
// The SakkoClient automatically tries to connect to the SakkoHost

// Therefore, BOTH instantiations need to be tried/caught

```

Now both of these objects can simply communicate


```Java
// Client sends the host a message that he'd like to react to "InfoChannel" messages
// The client will simply print out the string
client.subscribe("InfoChannel", System.out::println);

// Now, everytime any client or the host publish a message to "InfoChannel", this client will be notified
```

What happens under the hood:
```Java

client.subscribe("InfoChannel");

// Adds a handler if a messages comes in
client.addHandler("InfoChannel", new SakkoMessageHandler() {
            @Override
            public void handleInput(String message) {
                System.out.println(message);
            }
        });
```

Now, either the client or the host can send a message via this channel.
```
// Sending a message to "InfoChannel"
host.publish("InfoChannel", "Sakko is awesome");
```
The client will print out `"Sakko is awesome"`


Yes, that's it! You can add as many clients as you want.
If you have 5 clients, each listening to the same channel, and send a message to this channel,
`every client gets notificated with the sent message!`
