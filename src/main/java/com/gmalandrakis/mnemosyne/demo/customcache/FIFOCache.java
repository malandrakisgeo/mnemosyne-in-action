package com.gmalandrakis.mnemosyne.demo.customcache;

import com.gmalandrakis.mnemosyne.cache.AbstractGenericCache;
import com.gmalandrakis.mnemosyne.cache.AbstractMnemosyneCache;
import com.gmalandrakis.mnemosyne.core.ValuePool;
import com.gmalandrakis.mnemosyne.exception.MnemosyneRetrievalException;
import com.gmalandrakis.mnemosyne.structures.CacheParameters;
import com.gmalandrakis.mnemosyne.structures.CollectionIdWrapper;
import com.gmalandrakis.mnemosyne.structures.IdWrapper;
import com.gmalandrakis.mnemosyne.structures.SingleIdWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

public class FIFOCache<K, ID, T> extends AbstractMnemosyneCache<K, ID, T> {
    final boolean returnsCollection;

    final ConcurrentLinkedQueue<K> concurrentFIFOQueue = new ConcurrentLinkedQueue<>();

    /*
        In collection caches, a key corresponds to multiple IDs. We need a way to know how many keys are using an ID without transversing through
        the whole map of keys and their ID-collections every time.
        In single-value caches, more often than not, there is a 1-1 correspondence between keys and IDs. But this is not *guaranteed*
        (fun exercise: come up with cases where different keys may point to the same object/ID) , and especially when we
        are dealing with cache updates: we may want to use a key with a brand new value and corresponding ID.
        By keeping track of how many keys are using an ID, we can accurately inform the ValuePool that it may be time to get rid of an ID and its' corresponding
        object.
     */
    final ConcurrentHashMap<ID, Integer> numberOfUsesById = new ConcurrentHashMap<ID, Integer>();
    final String name;
    final boolean countdownFromCreation;
    final long timeToLive;
    final long invalidationInterval;
    final int totalCapacity;
    final float actualCapacity;
    final short preemptiveEvictionPercentage;
    final short evictionStepPercentage;
    final boolean handleCollectionKeysSeparately;
    public FIFOCache(CacheParameters parameters, ValuePool poolService) {
        super(parameters, poolService, new ConcurrentHashMap<>());
        this.returnsCollection = parameters.isReturnsCollection();
        this.totalCapacity = (parameters.getCapacity() <= 0 ? Integer.MAX_VALUE : parameters.getCapacity());
        this.timeToLive = (parameters.getTimeToLive() <= 0 ? Long.MAX_VALUE : parameters.getTimeToLive());
        this.invalidationInterval = (parameters.getInvalidationInterval() < 0 ? Long.MAX_VALUE : parameters.getInvalidationInterval());
        this.name = parameters.getCacheName();
        this.countdownFromCreation = parameters.isCountdownFromCreation();
        this.preemptiveEvictionPercentage = (parameters.getPreemptiveEvictionPercentage() <= 0 || parameters.getPreemptiveEvictionPercentage() >= 100 ? 100 : parameters.getPreemptiveEvictionPercentage());
        this.evictionStepPercentage = (parameters.getEvictionStepPercentage() < 0 || parameters.getEvictionStepPercentage() > 100) ? 0 : parameters.getEvictionStepPercentage();
        this.actualCapacity = (totalCapacity * (preemptiveEvictionPercentage / 100f));
        this.handleCollectionKeysSeparately = parameters.isHandleCollectionKeysSeparately();
    }

    @Override
    public void putAll(K key, Map<ID, T> map) {
        if (key == null || map == null || !returnsCollection) {
            return;
        }

        if (concurrentFIFOQueue.size() >= this.actualCapacity) {
            this.evict();
        }
        //We avoid iterative calls to put(), to avoid checking the keyIdMapper and concurrentFIFOQueue multiple times. One time suffices.
        var possibleValue = (CollectionIdWrapper<ID>) this.getKeyIdMapper().computeIfAbsent(key, k -> new CollectionIdWrapper<>());
        possibleValue.addAllToCollectionOrUpdate(map.keySet());

        map.forEach(this::addOrUpdateIdAndValue);

        if (!concurrentFIFOQueue.contains(key)) {
            concurrentFIFOQueue.add(key);
        }
    }

    @Override
    public void put(K key, ID id, T value) {
        if (key == null || id == null) {
            return;
        }
        if (concurrentFIFOQueue.size() >= this.actualCapacity) {
            this.evict();
        }

        if (returnsCollection) {
            var idWrapper = (CollectionIdWrapper) getKeyIdMapper().computeIfAbsent(key, k -> new CollectionIdWrapper());
            idWrapper.addToCollectionOrUpdate(id); //Unlike single-value caches, removing an old ID from a collection cache is not as simple as just replacing it a newer one. Only a manual call to removeOneFromCollection() or expiration can remove it.
        } else {
            var idWrapper = getKeyIdMapper().get(key);
            if (idWrapper != null) {
                var oldId = (ID) ((SingleIdWrapper) idWrapper).getId();
                if (oldId.equals(id)) {
                    getValuePool().put(id, value, false); //just update the current value
                    return;
                }
                removeOrDecreaseIdUses(oldId);
            }
            getKeyIdMapper().put(key, new SingleIdWrapper<ID>(id)); //if we used putIfAbsent, we would prevent the key from being updated with a brand new ID/value
        }

        addOrUpdateIdAndValue(id, value);

        if (!concurrentFIFOQueue.contains(key)) {
            concurrentFIFOQueue.add(key); //reminder that updates are not synonymous to accesses, and this is why we do not change the position in the queue on updating.
        }
    }

    @Override
    public void putInAllCollections(ID id, T value) {
        if (!returnsCollection || handleCollectionKeysSeparately) {
            return;
        }
        var initialNumOfUses = numberOfUsesById.get(id);
        int i = initialNumOfUses;
        for (K k : getKeyIdMapper().keySet()) {
            var idWrapper = ((CollectionIdWrapper) getKeyIdMapper().get(k));
            if (idWrapper.addToCollectionOrUpdate(id)) {
                numberOfUsesById.put(id, ++i);
            }
        }
        getValuePool().put(id, value, initialNumOfUses == 0);
    }

    @Override
    public T get(K key) {
        if (!concurrentFIFOQueue.contains(key)) {
            return null;
        }
        var cachedIdData = getKeyIdMapper().get(key);
        if (cachedIdData == null) {
            concurrentFIFOQueue.remove(key);
            throw new MnemosyneRetrievalException("Key is present in concurrent FIFO queue, buy not in keyIdMap: " + key.toString());
        }
        //TODO: Perhaps a cacheIdData with single Id could be used when handleCollectionKeysSeparately.
        ID id = (ID) (handleCollectionKeysSeparately ? ((CollectionIdWrapper) cachedIdData).getIds().toArray()[0] : ((SingleIdWrapper) cachedIdData).getId());

        return getValuePool().getValue(id);
    }

    @Override
    public Collection<T> getAll(K key) {
        if (!returnsCollection || !concurrentFIFOQueue.contains(key)) {
            return Collections.emptyList();
        }
        var id = (CollectionIdWrapper) getKeyIdMapper().get(key);
        if (id == null) {
            concurrentFIFOQueue.remove(key);
            throw new MnemosyneRetrievalException("Key was present in concurrent FIFO queue, buy not in keyIdMap: " + key.toString());
        }
        return getValuePool().getAll(id.getIds());
    }

    @Override
    public Collection<T> getAll(Collection<K> key) {
        var all = new ArrayList<T>();
        for (K k : key) {
            all.add(this.get(k));
        }
        return all;
    }

    @Override
    public void remove(K key) {
        var cacheData = getKeyIdMapper().get(key);
        if (cacheData == null) {
            return;
        }
        concurrentFIFOQueue.remove(key);
        getKeyIdMapper().remove(key);

        if (returnsCollection) {
            Collection<ID> ids = ((CollectionIdWrapper) cacheData).getIds();
            ids.forEach(this::removeOrDecreaseIdUses);
        } else {
            var id = (ID) ((SingleIdWrapper) cacheData).getId();
            removeOrDecreaseIdUses(id);
        }
    }

    @Override
    public void removeOneFromCollection(K key, ID id) {
        if (!returnsCollection) {
            return;
        }
        if (key == null) {
            removeFromAllCollections(id);
        } else {
            var cacheData = (CollectionIdWrapper) getKeyIdMapper().get(key);
            if (cacheData == null) {
                return;
            }
            if(cacheData.getIds().remove(id)){
                removeOrDecreaseIdUses(id);
            };
        }
    }

    @Override
    public void removeFromAllCollections(ID id) {
        if (!returnsCollection) {
            return;
        }
        var relatedKeys = new HashSet<>();
        for (K k : getKeyIdMapper().keySet()) {
            var deleted = ((CollectionIdWrapper) getKeyIdMapper().get(k)).getIds().remove(id);
            if (deleted) {
                relatedKeys.add(k);
                removeOrDecreaseIdUses(id);
            }
        }
        if (handleCollectionKeysSeparately) { //on special collection handling, a key corresponds to at most one ID
            relatedKeys.forEach(k -> {
                getKeyIdMapper().remove(k);
                concurrentFIFOQueue.remove(k);
            });
        }
    }

    @Override
    public String getAlgorithmName() {
        return "FIFO";
    }

    @Override
    public K getTargetKey() {
        return concurrentFIFOQueue.poll();
    }

    @Override
    public void evict() {
        if (timeToLive != Long.MAX_VALUE && timeToLive > 0) {
            var expiredValues = getKeyIdMapper().entrySet().stream().filter(this::isExpired).map(Map.Entry::getKey);
            expiredValues.forEach(this::remove);
        }

        while (concurrentFIFOQueue.size() >= this.actualCapacity) {
            var oldestElement = concurrentFIFOQueue.poll();
            if (oldestElement != null) {
                remove(oldestElement);
            }
        }
    }

    @Override
    public void invalidateCache() {
        while (!concurrentFIFOQueue.isEmpty()) {
            var k = concurrentFIFOQueue.poll();
            remove(k);
        }
        /*
            When we preemptively added values in collection-caches without filtering out the ones already fetched,
            we ended up with a memory leak: the IDs were removed from concurrentFIFOQueue but were still referenced to
            by numberOfUsesById. We keep it as it is to help us find other mistakes in a while.
            TODO: Replace it manually after everything is tested thoroughly.
         */
        //numberOfUsesById = new ConcurrentHashMap<ID, Integer>();
    }

    @Override
    public boolean idUsedAlready(ID v) {
        var numberOfCollectionsUsingIt = numberOfUsesById.get(v);
        return numberOfCollectionsUsingIt != null && numberOfCollectionsUsingIt > 0;
    }


    private void removeOrDecreaseIdUses(ID id) {
        var numOfCollectionsUsingId = numberOfUsesById.getOrDefault(id, 0) - 1;
        if (numOfCollectionsUsingId <= 0) {
            numberOfUsesById.remove(id);
            getValuePool().removeOrDecreaseNumberOfUsesForId(id);
        } else {
            numberOfUsesById.put(id, numOfCollectionsUsingId);
        }
    }

    private void addOrUpdateIdAndValue(ID id, T value) {
        var usesOfIdInCache = numberOfUsesById.getOrDefault(id, 0); //In non-collection caches, a key corresponds to just one object, but one object may be referenced to by many keys.
        var idAlreadyInCache = usesOfIdInCache > 0;
        numberOfUsesById.put(id, ++usesOfIdInCache);
        getValuePool().put(id, value, !idAlreadyInCache);
    }
    protected boolean isExpired(Map.Entry<K, IdWrapper<ID>> entry) {
        long creationOrAccessTime = countdownFromCreation ? entry.getValue().getCreatedOn() : entry.getValue().getLastAccessed();
        return (System.currentTimeMillis() - creationOrAccessTime) > this.timeToLive;    //System.currentTimeMillis() is very slow on Linux though very fast on Windows, but System.nanoTime() the opposite.
    }

}
