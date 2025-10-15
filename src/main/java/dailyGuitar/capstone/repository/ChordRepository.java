package dailyGuitar.capstone.repository;

import dailyGuitar.capstone.entity.Chord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChordRepository extends JpaRepository<Chord, Long> {
    
    Optional<Chord> findByChordName(String chordName);
    
    List<Chord> findByIsActiveTrueOrderByChordName();
    
    @Query("SELECT c FROM Chord c WHERE c.isActive = true AND " +
           "(LOWER(c.chordName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.chordSymbol) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Chord> findByQuery(@Param("query") String query);
    
    List<Chord> findByRootNoteAndIsActiveTrueOrderByChordName(String rootNote);
    
    List<Chord> findByChordTypeAndIsActiveTrueOrderByChordName(Chord.ChordType chordType);
    
    List<Chord> findByDifficultyLevelAndIsActiveTrueOrderByChordName(String difficultyLevel);
    
    @Query("SELECT c FROM Chord c WHERE c.isActive = true ORDER BY c.chordName")
    Page<Chord> findActiveChordsOrderByName(Pageable pageable);
}

