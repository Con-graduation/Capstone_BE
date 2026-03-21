package dailyGuitar.capstone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chords")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String chordName;
    
    @Column(nullable = false)
    private String chordSymbol;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Builder.Default
    private String difficultyLevel = "BEGINNER";
    
    @Column(columnDefinition = "TEXT")
    private String fingeringPositions; // JSON string
    
    private String imageUrl;
    
    private String audioUrl;
    
    @Column(columnDefinition = "TEXT")
    private String chordNotes; // JSON string
    
    @Column(nullable = false)
    private String rootNote;
    
    @Enumerated(EnumType.STRING)
    private ChordType chordType;
    
    @Column(columnDefinition = "TEXT")
    private String tags; // JSON string
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum ChordType {
        MAJOR, MINOR, DIMINISHED, AUGMENTED, SUSPENDED, ADDED, EXTENDED
    }
}

