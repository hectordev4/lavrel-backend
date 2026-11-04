package com.lavrel.library.application.service;

import com.lavrel.library.application.port.in.ManageCollectionsUseCase;
import com.lavrel.library.application.port.out.CollectionRepositoryPort;
import com.lavrel.library.application.port.out.GameOwnershipPort;
import com.lavrel.library.domain.Collection;
import com.lavrel.library.domain.CollectionLimitReachedException;
import com.lavrel.library.domain.CollectionNameTakenException;
import com.lavrel.library.domain.CollectionNames;
import com.lavrel.library.domain.CollectionNotFoundException;
import com.lavrel.library.domain.GameNotOwnedException;
import com.lavrel.library.domain.InvalidCollectionOrderException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectionService implements ManageCollectionsUseCase {

    private final CollectionRepositoryPort collections;
    private final GameOwnershipPort ownership;
    private final Clock clock;

    public CollectionService(CollectionRepositoryPort collections, GameOwnershipPort ownership, Clock clock) {
        this.collections = collections;
        this.ownership = ownership;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionView> list(long userId) {
        return views(userId, collections.findAll(userId));
    }

    @Override
    @Transactional
    public CollectionView create(long userId, String rawName) {
        String name = CollectionNames.validate(rawName);
        List<Collection> existing = collections.findAll(userId);
        if (existing.size() >= MAX_COLLECTIONS) {
            throw new CollectionLimitReachedException(MAX_COLLECTIONS);
        }
        if (collections.nameTaken(userId, name, null)) {
            throw new CollectionNameTakenException(name);
        }
        Collection created = collections.save(Collection.create(userId, name, existing.size() + 1, clock.instant()));
        return new CollectionView(created.publicId(), created.name(), created.position(), 0);
    }

    @Override
    @Transactional
    public CollectionView rename(long userId, UUID collectionId, String rawName) {
        Collection collection = owned(userId, collectionId);
        String name = CollectionNames.validate(rawName);
        if (!collection.name().equalsIgnoreCase(name) && collections.nameTaken(userId, name, collectionId)) {
            throw new CollectionNameTakenException(name);
        }
        if (collection.rename(name)) {
            collections.save(collection);
        }
        return new CollectionView(collection.publicId(), collection.name(), collection.position(),
                collections.visibleGameCounts(userId).getOrDefault(collection.id(), 0));
    }

    @Override
    @Transactional
    public void delete(long userId, UUID collectionId) {
        Collection collection = owned(userId, collectionId);
        collections.delete(collection.id());
        List<Collection> remaining = collections.findAll(userId);
        List<Collection> moved = new ArrayList<>();
        for (int i = 0; i < remaining.size(); i++) {
            if (remaining.get(i).moveTo(i + 1)) {
                moved.add(remaining.get(i));
            }
        }
        collections.saveAll(moved);
    }

    @Override
    @Transactional
    public List<CollectionView> reorder(long userId, List<UUID> collectionIds) {
        List<Collection> all = collections.findAll(userId);
        if (collectionIds.size() != new HashSet<>(collectionIds).size()) {
            throw new InvalidCollectionOrderException("A collection appears more than once");
        }
        Map<UUID, Collection> byId = all.stream().collect(Collectors.toMap(Collection::publicId, Function.identity()));
        if (!byId.keySet().equals(new HashSet<>(collectionIds))) {
            throw new InvalidCollectionOrderException("The order must list every collection exactly once");
        }
        List<Collection> ordered = collectionIds.stream().map(byId::get).toList();
        List<Collection> moved = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).moveTo(i + 1)) {
                moved.add(ordered.get(i));
            }
        }
        collections.saveAll(moved);
        return views(userId, ordered);
    }

    @Override
    @Transactional
    public void addGame(long userId, UUID collectionId, long gameId) {
        Collection collection = owned(userId, collectionId);
        if (!ownership.owns(userId, gameId)) {
            throw new GameNotOwnedException(gameId);
        }
        collections.addGame(collection.id(), gameId, clock.instant());
    }

    @Override
    @Transactional
    public void removeGame(long userId, UUID collectionId, long gameId) {
        collections.removeGame(owned(userId, collectionId).id(), gameId);
    }

    private Collection owned(long userId, UUID collectionId) {
        return collections.find(userId, collectionId).orElseThrow(CollectionNotFoundException::new);
    }

    private List<CollectionView> views(long userId, List<Collection> list) {
        Map<Long, Integer> counts = collections.visibleGameCounts(userId);
        return list.stream()
                .map(c -> new CollectionView(c.publicId(), c.name(), c.position(), counts.getOrDefault(c.id(), 0)))
                .toList();
    }
}
