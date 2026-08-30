package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.IssueNote;
import com.riskregister.riskregisterapp.repositories.IssueNoteRepository;

@Service
public class IssueNoteService {

    /** Long enough for a real discussion, short enough to stop anyone pasting a document in. */
    private static final int MAX_LENGTH = 5000;

    @Autowired
    private IssueNoteRepository issueNoteRepository;

    public List<IssueNote> findByIssue(Long organizationId, Long issueId) {
        return issueNoteRepository.findByOrganizationIdAndIssueIdOrderByCreatedAtAsc(organizationId, issueId);
    }

    public long countByIssue(Long organizationId, Long issueId) {
        return issueNoteRepository.countByOrganizationIdAndIssueId(organizationId, issueId);
    }

    public IssueNote add(Long organizationId, Long issueId, String content,
                         String authorId, String authorName) {
        String clean = content == null ? "" : content.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Write something before posting.");
        }
        if (clean.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Comment is too long (" + clean.length() + " characters, limit " + MAX_LENGTH + ").");
        }

        IssueNote note = new IssueNote();
        note.setOrganizationId(organizationId);
        note.setIssueId(issueId);
        note.setContent(clean);
        note.setAuthorId(authorId);
        note.setAuthorName(authorName);
        note.setCreatedAt(Instant.now());
        return issueNoteRepository.save(note);
    }

    /**
     * Remove a comment. Only its author may do so — on a findings register the thread is part
     * of the record, and letting anyone delete anyone else's remarks would make it worthless
     * as evidence of what was discussed.
     */
    public void delete(Long organizationId, Long noteId, String requesterId) {
        IssueNote note = issueNoteRepository.findByOrganizationIdAndId(organizationId, noteId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found."));

        if (note.getAuthorId() == null || !note.getAuthorId().equalsIgnoreCase(requesterId)) {
            throw new IllegalStateException("You can only delete your own comments.");
        }
        issueNoteRepository.delete(note);
    }
}
