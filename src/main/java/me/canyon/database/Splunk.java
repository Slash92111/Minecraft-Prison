package me.canyon.database;


import com.splunk.*;
import me.canyon.Main;

import java.util.HashMap;
import java.util.Map;

public class Splunk {
    private Main plugin;
    private Service connection;

    public Splunk(Main plugin) {
        this.plugin = plugin;
        String host = plugin.getDatabaseConfig().getString("splunk.host");
        String username = plugin.getDatabaseConfig().getString("splunk.username");
        String password = plugin.getDatabaseConfig().getString("splunk.password");
        String scheme = plugin.getDatabaseConfig().getString("splunk.scheme");
        int port = plugin.getDatabaseConfig().getInt("splunk.port");

        HttpService.setSslSecurityProtocol( SSLSecurityProtocol.TLSv1_2 );

        Map<String, Object> connectionArgs = new HashMap<String, Object>();
        connectionArgs.put("host", host);
        connectionArgs.put("username", username);
        connectionArgs.put("password", password);
        connectionArgs.put("port", port);
        connectionArgs.put("scheme", scheme);

        this.connection = Service.connect(connectionArgs);
    }

    public void log(String index, String source, String data) {
        Args logArgs = new Args();
        logArgs.put("sourcetype", source);

        Receiver receiver = connection.getReceiver();

        receiver.log(index, logArgs, data);

        plugin.debug("Splunk   " +  "Index=" + index + " | Source=" + source + " | Data=" + data);
    }
}
