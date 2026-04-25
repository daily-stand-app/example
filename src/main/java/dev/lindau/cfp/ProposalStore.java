package dev.lindau.cfp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class ProposalStore {
    private final AtomicLong ids = new AtomicLong(1);
    private final List<Proposal> proposals = new ArrayList<>();

    public synchronized Proposal save(String title,
                                      String speakerName,
                                      String speakerEmail,
                                      String abstractText,
                                      List<String> tags) {
        Proposal proposal = new Proposal(
                ids.getAndIncrement(),
                title,
                speakerName,
                speakerEmail,
                abstractText,
                List.copyOf(tags),
                Instant.now()
        );
        proposals.add(proposal);
        return proposal;
    }

    public synchronized List<Proposal> findAll() {
        return List.copyOf(proposals);
    }

    public synchronized Optional<Proposal> findById(long id) {
        return proposals.stream()
                .filter(proposal -> proposal.id() == id)
                .findFirst();
    }

    public synchronized long count() {
        return proposals.size();
    }
}
