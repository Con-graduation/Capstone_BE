package dailyGuitar.capstone.service;

import dailyGuitar.capstone.dto.ChordResponseDto;
import dailyGuitar.capstone.entity.Chord;
import dailyGuitar.capstone.exception.ChordNotFoundException;
import dailyGuitar.capstone.repository.ChordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChordService {

    private final ChordRepository chordRepository;

    public Page<ChordResponseDto> getAllChords(Pageable pageable) {
        Page<Chord> chords = chordRepository.findActiveChordsOrderByName(pageable);
        return chords.map(ChordResponseDto::from);
    }

    public ChordResponseDto getChordById(Long id) {
        Chord chord = chordRepository.findById(id)
                .orElseThrow(() -> new ChordNotFoundException("코드를 찾을 수 없습니다: " + id));
        return ChordResponseDto.from(chord);
    }

    public ChordResponseDto getChordByName(String chordName) {
        Chord chord = chordRepository.findByChordName(chordName)
                .orElseThrow(() -> new ChordNotFoundException("코드를 찾을 수 없습니다: " + chordName));
        return ChordResponseDto.from(chord);
    }

    public List<ChordResponseDto> searchChords(String query) {
        List<Chord> chords = chordRepository.findByQuery(query);
        return chords.stream().map(ChordResponseDto::from).toList();
    }

    public List<ChordResponseDto> getChordsByRootNote(String rootNote) {
        List<Chord> chords = chordRepository.findByRootNoteAndIsActiveTrueOrderByChordName(rootNote);
        return chords.stream().map(ChordResponseDto::from).toList();
    }

    public List<ChordResponseDto> getChordsByType(Chord.ChordType chordType) {
        List<Chord> chords = chordRepository.findByChordTypeAndIsActiveTrueOrderByChordName(chordType);
        return chords.stream().map(ChordResponseDto::from).toList();
    }

    public List<ChordResponseDto> getChordsByDifficulty(String difficultyLevel) {
        List<Chord> chords = chordRepository.findByDifficultyLevelAndIsActiveTrueOrderByChordName(difficultyLevel);
        return chords.stream().map(ChordResponseDto::from).toList();
    }

    public List<ChordResponseDto> getAllActiveChords() {
        List<Chord> chords = chordRepository.findByIsActiveTrueOrderByChordName();
        return chords.stream().map(ChordResponseDto::from).toList();
    }
}

