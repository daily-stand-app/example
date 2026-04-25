package dev.lindau.cfp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
                Instant.now(),
                ProposalStatus.SUBMITTED
        );
        proposals.add(proposal);
        return proposal;
    }

    public synchronized List<Proposal> findAll() {
        return List.copyOf(proposals);
    }

    public synchronized List<Proposal> search(String speakerEmail, String tag) {
        return proposals.stream()
                .filter(proposal -> speakerEmail == null || speakerEmail.isBlank() || proposal.speakerEmail().equalsIgnoreCase(speakerEmail))
                .filter(proposal -> tag == null || tag.isBlank() || proposal.tags().stream().anyMatch(entry -> entry.equalsIgnoreCase(tag)))
                .collect(Collectors.toList());
    }

    public synchronized Optional<Proposal> findById(long id) {
        return proposals.stream()
                .filter(proposal -> proposal.id() == id)
                .findFirst();
    }

    public synchronized Proposal updateStatus(long id, ProposalStatus status) {
        for (int index = 0; index < proposals.size(); index++) {
            Proposal current = proposals.get(index);
            if (current.id() == id) {
                Proposal updated = new Proposal(
                        current.id(),
                        current.title(),
                        current.speakerName(),
                        current.speakerEmail(),
                        current.abstractText(),
                        current.tags(),
                        current.submittedAt(),
                        status
                );
                proposals.set(index, updated);
                return updated;
            }
        }
        throw new IllegalArgumentException("Unknown proposal id: " + id);
    }

    public synchronized long count() {
        return proposals.size();
    }
}
