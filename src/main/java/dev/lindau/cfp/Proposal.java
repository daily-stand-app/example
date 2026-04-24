package dev.lindau.cfp;

import java.time.Instant;
import java.util.List;

public record Proposal(
        long id,
        String title,
        String speakerName,
        String speakerEmail,
        String abstractText,
        List<String> tags,
        Instant submittedAt
) {
}
