package com.riskregister.riskregisterapp.services;

import com.riskregister.riskregisterapp.entities.RiskNote;
import com.riskregister.riskregisterapp.repositories.RiskNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RiskNoteService {

    @Autowired
    private RiskNoteRepository riskNoteRepository;

    public List<RiskNote> findNotesByRisk(Long riskId) {
        return riskNoteRepository.findByRiskIdOrderByCreatedAtAsc(riskId);
    }

    public RiskNote addNote(Long riskId, String content, String authorId, String authorName) {
        RiskNote note = new RiskNote();
        note.setRiskId(riskId);
        note.setContent(content);
        note.setAuthorId(authorId);
        note.setAuthorName(authorName);
        note.setCreatedAt(Instant.now());
        return riskNoteRepository.save(note);
    }

    public void deleteNote(Long noteId) {
        if (noteId != null) {
            riskNoteRepository.deleteById(noteId);
        }
    }
}
