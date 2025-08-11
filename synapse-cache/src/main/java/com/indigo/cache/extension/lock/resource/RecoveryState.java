package com.indigo.cache.extension.lock.resource;

/**
 * 恢复状态类
 * 管理资源的恢复过程
 */
public class RecoveryState {
    
    private volatile boolean isRecovering = false;
    private volatile long lastRecoveryTime = 0;
    private volatile long recoveryDuration = 0;
    private volatile int recoveryCount = 0;
    private volatile long totalRecoveryTime = 0;
    
    public boolean isRecovering() {
        return isRecovering;
    }
    
    public void setRecovering(boolean recovering) {
        this.isRecovering = recovering;
        if (recovering) {
            this.lastRecoveryTime = System.currentTimeMillis();
        } else {
            // 计算恢复耗时
            if (this.lastRecoveryTime > 0) {
                this.recoveryDuration = System.currentTimeMillis() - this.lastRecoveryTime;
                this.totalRecoveryTime += this.recoveryDuration;
                this.recoveryCount++;
            }
        }
    }
    
    public long getLastRecoveryTime() {
        return lastRecoveryTime;
    }
    
    public void setLastRecoveryTime(long time) {
        this.lastRecoveryTime = time;
    }
    
    public long getRecoveryDuration() {
        return recoveryDuration;
    }
    
    public void setRecoveryDuration(long duration) {
        this.recoveryDuration = duration;
    }
    
    public int getRecoveryCount() {
        return recoveryCount;
    }
    
    public long getTotalRecoveryTime() {
        return totalRecoveryTime;
    }
    
    public double getAverageRecoveryTime() {
        return recoveryCount > 0 ? (double) totalRecoveryTime / recoveryCount : 0.0;
    }
    
    public void reset() {
        this.isRecovering = false;
        this.lastRecoveryTime = 0;
        this.recoveryDuration = 0;
        this.recoveryCount = 0;
        this.totalRecoveryTime = 0;
    }
} 