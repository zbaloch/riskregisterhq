package com.riskregister.riskregisterapp.services;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.riskregister.riskregisterapp.entities.RiskNote;
import com.riskregister.riskregisterapp.repositories.RiskNoteRepository;

@Service
public class RiskNoteService {

    /** Long enough for a real discussion, short enough to stop anyone pasting a document in. */
    private static final int MAX_LENGTH = 5000;

    @Autowired
    private RiskNoteRepository riskNoteRepository;

    public List<RiskNote> findNotesByRisk(Long riskId) {
        return riskNoteRepository.findByRiskIdOrderByCreatedAtAsc(riskId);
    }

    public RiskNote addNote(Long riskId, String content, String authorId, String authorName) {
        String clean = content == null ? "" : content.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Write something before posting.");
        }
        if (clean.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                "Note is too long (" + clean.length() + " characters, limit " + MAX_LENGTH + ").");
        }

        RiskNote note = new RiskNote();
        note.setRiskId(riskId);
        note.setContent(clean);
        note.setAuthorId(authorId);
        note.setAuthorName(authorName);
        note.setCreatedAt(Instant.now());
        return riskNoteRepository.save(note);
    }

    /**
     * Remove a note. Only its author may do so, and only from the risk it actually belongs to.
     *
     * <p>risk_notes carries no organisation column, so the caller must pass the id of a risk it
     * has already resolved within the current organisation; checking the note against it is what
     * stops a note being deleted from another tenant by guessing its id.</p>
     */
    public void deleteNote(Long noteId, Long riskId, String requesterId) {
        RiskNote note = riskNoteRepository.findById(noteId)
            .orElseThrow(() -> new IllegalArgumentException("Note not found."));

        if (!note.getRiskId().equals(riskId)) {
            throw new IllegalArgumentException("Note not found.");
        }
        if (note.getAuthorId() == null || !note.getAuthorId().equalsIgnoreCase(requesterId)) {
            throw new IllegalStateException("You can only delete your own notes.");
        }
        riskNoteRepository.delete(note);
    }
}
