package com.securecare.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private record Window(int attempts, Instant started) { }
    private final ConcurrentHashMap<String,Window> attempts=new ConcurrentHashMap<>();
    public synchronized boolean allow(String ip) {
        Instant now=Instant.now(); Window w=attempts.get(ip);
        if(w==null || now.isAfter(w.started().plusSeconds(60))) { attempts.put(ip,new Window(0,now)); return true; }
        return w.attempts()<5;
    }
    public synchronized void recordFailure(String ip) {
        Instant now=Instant.now(); Window w=attempts.get(ip);
        if(w==null || now.isAfter(w.started().plusSeconds(60))) attempts.put(ip,new Window(1,now));
        else attempts.put(ip,new Window(w.attempts()+1,w.started()));
    }
    public synchronized void reset(String ip) { attempts.remove(ip); }
}
