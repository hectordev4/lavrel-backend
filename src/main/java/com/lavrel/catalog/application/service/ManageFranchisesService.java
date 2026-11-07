package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ManageFranchisesUseCase;
import com.lavrel.catalog.application.port.out.FranchiseBlocklistPort;
import com.lavrel.catalog.application.port.out.FranchiseBlocklistPort.BlockedFranchise;
import com.lavrel.catalog.application.port.out.FranchiseLinksPort;
import com.lavrel.catalog.application.port.out.FranchiseLinksPort.FranchiseLink;
import com.lavrel.catalog.domain.FranchiseRules;
import com.lavrel.catalog.domain.CreatorNotFoundException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageFranchisesService implements ManageFranchisesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageFranchisesService.class);

    private final FranchiseLinksPort links;
    private final FranchiseBlocklistPort blocklist;
    private final Clock clock;

    public ManageFranchisesService(FranchiseLinksPort links, FranchiseBlocklistPort blocklist, Clock clock) {
        this.links = links;
        this.blocklist = blocklist;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CleanupReport cleanup(boolean apply) {
        Set<String> blocked = blocklist.keys();
        List<FranchiseLink> rejected = new ArrayList<>();
        Map<Long, String> reasons = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, Integer> games = new LinkedHashMap<>();
        for (FranchiseLink link : links.findAll()) {
            Optional<String> reason = FranchiseRules.rejectionReason(link.franchise(), link.developers(),
                    link.publishers(), blocked);
            if (reason.isPresent()) {
                rejected.add(link);
                reasons.putIfAbsent(link.creatorId(), reason.get());
                names.putIfAbsent(link.creatorId(), link.franchise());
                games.merge(link.creatorId(), 1, Integer::sum);
            }
        }
        List<Finding> findings = names.keySet().stream()
                .map(id -> new Finding(id, names.get(id), games.get(id), reasons.get(id)))
                .sorted(Comparator.comparing(Finding::games).reversed().thenComparing(Finding::name))
                .toList();
        int removed = apply ? links.unlink(rejected) : 0;
        if (apply) {
            log.info("Franchise clean-up removed {} links ({} franchises)", removed, findings.size());
        }
        return new CleanupReport(apply, removed, findings);
    }

    @Override
    @Transactional
    public BlockedFranchise block(long creatorId) {
        String name = links.creatorName(creatorId).orElseThrow(() -> new CreatorNotFoundException(creatorId));
        String key = FranchiseRules.key(name);
        blocklist.add(key, name, clock.instant());
        int removed = links.unlinkCreator(creatorId);
        log.info("Franchise '{}' blocked; {} links removed", name, removed);
        return new BlockedFranchise(key, name, clock.instant());
    }

    @Override
    @Transactional
    public void unblock(long creatorId) {
        links.creatorName(creatorId).ifPresent(name -> {
            if (blocklist.remove(FranchiseRules.key(name))) {
                log.info("Franchise '{}' unblocked", name);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedFranchise> blocked() {
        return blocklist.list();
    }
}
