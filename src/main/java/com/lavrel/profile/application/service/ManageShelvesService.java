package com.lavrel.profile.application.service;

import com.lavrel.library.application.port.in.ManageCollectionsUseCase;
import com.lavrel.library.application.port.in.ManageCollectionsUseCase.CollectionView;
import com.lavrel.profile.application.port.in.ManageShelvesUseCase;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.AchievementItem;
import com.lavrel.profile.application.port.out.ShelfContentQueryPort;
import com.lavrel.profile.application.port.out.ShelfRepositoryPort;
import com.lavrel.profile.domain.InvalidShelfException;
import com.lavrel.profile.domain.Shelf;
import com.lavrel.profile.domain.ShelfLimitReachedException;
import com.lavrel.profile.domain.ShelfNotFoundException;
import com.lavrel.profile.domain.ShelfTitles;
import com.lavrel.profile.domain.ShelfType;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageShelvesService implements ManageShelvesUseCase {

    private final ShelfRepositoryPort shelves;
    private final ShelfContentQueryPort content;
    private final ManageCollectionsUseCase collections;
    private final Clock clock;

    public ManageShelvesService(ShelfRepositoryPort shelves, ShelfContentQueryPort content,
                                ManageCollectionsUseCase collections, Clock clock) {
        this.shelves = shelves;
        this.content = content;
        this.collections = collections;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelfView> list(long userId) {
        return shelves.findAll(userId).stream().map(this::view).toList();
    }

    @Override
    @Transactional
    public ShelfView add(long userId, ShelfType type, String rawTitle, UUID collectionId) {
        String title = ShelfTitles.validate(rawTitle);
        List<Shelf> existing = shelves.findAll(userId);
        if (existing.size() >= MAX_SHELVES) {
            throw new ShelfLimitReachedException(MAX_SHELVES);
        }
        if (type.onlyOnce() && existing.stream().anyMatch(s -> s.type() == type)) {
            throw new InvalidShelfException("Your profile already has a " + type + " shelf");
        }
        if (type == ShelfType.COLLECTION) {
            if (collectionId == null) {
                throw new InvalidShelfException("A collection shelf needs collectionId");
            }
            boolean mine = collections.list(userId).stream().map(CollectionView::id).anyMatch(collectionId::equals);
            if (!mine) {
                throw new InvalidShelfException("That collection is not yours");
            }
            if (existing.stream().anyMatch(s -> collectionId.equals(s.collectionId()))) {
                throw new InvalidShelfException("That collection is already on your profile");
            }
        } else if (collectionId != null) {
            throw new InvalidShelfException("Only a collection shelf takes collectionId");
        }
        return view(shelves.save(Shelf.create(userId, type, title, existing.size() + 1, collectionId,
                clock.instant())));
    }

    @Override
    @Transactional
    public ShelfView retitle(long userId, UUID shelfId, String rawTitle) {
        Shelf shelf = owned(userId, shelfId);
        shelf.retitle(ShelfTitles.validate(rawTitle));
        return view(shelves.save(shelf));
    }

    @Override
    @Transactional
    public void delete(long userId, UUID shelfId) {
        shelves.delete(owned(userId, shelfId).id());
        List<Shelf> remaining = shelves.findAll(userId);
        List<Shelf> moved = new ArrayList<>();
        for (int i = 0; i < remaining.size(); i++) {
            if (remaining.get(i).moveTo(i + 1)) {
                moved.add(remaining.get(i));
            }
        }
        shelves.saveOrder(moved);
    }

    @Override
    @Transactional
    public List<ShelfView> reorder(long userId, List<UUID> shelfIds) {
        List<Shelf> all = shelves.findAll(userId);
        if (shelfIds.size() != new HashSet<>(shelfIds).size()) {
            throw new InvalidShelfException("A shelf appears more than once");
        }
        Map<UUID, Shelf> byId = all.stream().collect(Collectors.toMap(Shelf::publicId, Function.identity()));
        if (!byId.keySet().equals(new HashSet<>(shelfIds))) {
            throw new InvalidShelfException("The order must list every shelf exactly once");
        }
        List<Shelf> ordered = shelfIds.stream().map(byId::get).toList();
        List<Shelf> moved = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).moveTo(i + 1)) {
                moved.add(ordered.get(i));
            }
        }
        shelves.saveOrder(moved);
        return ordered.stream().map(this::view).toList();
    }

    @Override
    @Transactional
    public ShelfView showAchievements(long userId, UUID shelfId, List<Long> achievementIds) {
        Shelf shelf = owned(userId, shelfId);
        shelf.showAchievements(achievementIds);
        Set<Long> unlocked = content.unlockedAmong(userId, achievementIds);
        if (!unlocked.containsAll(achievementIds)) {
            throw new InvalidShelfException("You can only show achievements you have unlocked");
        }
        return view(shelves.save(shelf));
    }

    private Shelf owned(long userId, UUID shelfId) {
        return shelves.find(userId, shelfId).orElseThrow(ShelfNotFoundException::new);
    }

    private ShelfView view(Shelf s) {
        List<AchievementItem> picked = s.type() == ShelfType.ACHIEVEMENTS && !s.achievementIds().isEmpty()
                ? content.achievements(s.userId(), s.achievementIds())
                : List.of();
        return new ShelfView(s.publicId(), s.type(), s.title(), s.position(), s.collectionId(), s.achievementIds(),
                picked);
    }
}
