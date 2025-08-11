package com.indigo.cache.extension.lock.resource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 资源状态类
 * 管理资源的可用性和访问信息
 */
public class ResourceState {
    
    private volatile boolean available = false;
    private volatile long lastAccessTime = 0;
    private volatile long releaseTime = 0;
    private final AtomicInteger accessCount = new AtomicInteger(0);
    private volatile long lastRecoveryTime = 0;
    private volatile long recoveryDuration = 0;
    
    public ResourceState() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
        if (available) {
            this.lastAccessTime = System.currentTimeMillis();
            this.lastRecoveryTime = System.currentTimeMillis();
        }
    }
    
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
    
    public long getReleaseTime() {
        return releaseTime;
    }
    
    public void setReleaseTime(long releaseTime) {
        this.releaseTime = releaseTime;
    }
    
    public int getAccessCount() {
        return accessCount.get();
    }
    
    public void incrementAccessCount() {
        this.accessCount.incrementAndGet();
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    public long getTimeSinceRelease() {
        return releaseTime > 0 ? System.currentTimeMillis() - releaseTime : 0;
    }
    
    public long getTimeSinceLastAccess() {
        return System.currentTimeMillis() - lastAccessTime;
    }
    
    public long getLastRecoveryTime() {
        return lastRecoveryTime;
    }
    
    public void setLastRecoveryTime(long lastRecoveryTime) {
        this.lastRecoveryTime = lastRecoveryTime;
    }
    
    public long getRecoveryDuration() {
        return recoveryDuration;
    }
    
    public void setRecoveryDuration(long recoveryDuration) {
        this.recoveryDuration = recoveryDuration;
    }
    
    public void reset() {
        this.available = false;
        this.releaseTime = System.currentTimeMillis();
        this.accessCount.set(0);
    }
} 