package com.zpzhou.trendingrestaurants.controllers.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

public class TumblingCache<K, V> {

    private static final Logger logger = LogManager.getLogger();

    private final ScheduledExecutorService scheduler;
    private final ReadWriteLock readWriteLock;
    private final Map<K, V> state;
    private long capacity;
    private long size;

    public TumblingCache(final ScheduledExecutorService scheduler,
                         final ReadWriteLock readWriteLock,
                         final Map<K, V> state,
                         final int windowSizeMinutes,
                         final long capacity) {

        this.scheduler = scheduler;
        this.readWriteLock = readWriteLock;
        this.state = state;
        this.capacity = capacity;
        this.size = 0;

        scheduler.scheduleAtFixedRate(
                getEmptyCacheTask(), windowSizeMinutes, windowSizeMinutes, TimeUnit.MINUTES);
    }

    public Optional<V> get(final K key) {
        readWriteLock.readLock().lock();
        final Optional<V> value = Optional.ofNullable(
                state.getOrDefault(key, null));

        readWriteLock.readLock().unlock();
        return value;
    }

    public void put(final K key, final V value) {
        if (!state.containsKey(key) && size <= capacity) {
            state.put(key, value);
            size++;
        }
    }

    private Runnable getEmptyCacheTask() {
        return () -> {
            readWriteLock.writeLock().lock();
            state.clear();
            readWriteLock.writeLock().unlock();
        };
    }
}
