package dailyGuitar.capstone.dto;

import dailyGuitar.capstone.entity.Chord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChordResponseDto {

    private Long id;
    private String chordName;
    private String chordSymbol;
    private String description;
    private String difficultyLevel;
    private String fingeringPositions;
    private String imageUrl;
    private String audioUrl;
    private String chordNotes;
    private String rootNote;
    private Chord.ChordType chordType;
    private String tags;
    private LocalDateTime createdAt;

    public static ChordResponseDto from(Chord chord) {
        return ChordResponseDto.builder()
                .id(chord.getId())
                .chordName(chord.getChordName())
                .chordSymbol(chord.getChordSymbol())
                .description(chord.getDescription())
                .difficultyLevel(chord.getDifficultyLevel())
                .fingeringPositions(chord.getFingeringPositions())
                .imageUrl(chord.getImageUrl())
                .audioUrl(chord.getAudioUrl())
                .chordNotes(chord.getChordNotes())
                .rootNote(chord.getRootNote())
                .chordType(chord.getChordType())
                .tags(chord.getTags())
                .createdAt(chord.getCreatedAt())
                .build();
    }
}

