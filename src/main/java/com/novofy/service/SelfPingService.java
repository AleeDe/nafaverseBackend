// filepath: c:\Users\drogo\OneDrive\Desktop\nafaverse\nafaverseBackend\src\main\java\com\novofy\service\SelfPingService.java
package com.novofy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SelfPingService {

    private final RestTemplate rest = new RestTemplate();

    @Value("${server.port:8080}")
    private String port;

    // every 60s, 10s initial delay
    @Scheduled(initialDelay = 10000, fixedRateString = "${SELF_PING_INTERVAL_MS:60000}")
    public void pingSelf() {
        String url = "https://nafaversebackend.onrender.com"  + "/health/ping";
        System.out.println("Self-pinging: " + url);
        try {
            rest.getForObject(url, String.class);
            System.out.println("Self-ping successful");
        } catch (Exception ignored) {
            // optional: log exception
        }
    }
}
