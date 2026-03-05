package com.socialmedia.repository;

import com.socialmedia.entity.Tweet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
    List<Tweet> findByAuthorIdAndCreatedAtBetween(Long authorId, Instant startTime, Instant endTime);
    
    @Query("SELECT t FROM Tweet t WHERE t.author.id = :authorId AND t.createdAt >= :startTime AND t.createdAt <= :endTime ORDER BY t.createdAt DESC")
    List<Tweet> findTweetsByAuthorAndTimeRange(@Param("authorId") Long authorId, 
                                                @Param("startTime") Instant startTime, 
                                                @Param("endTime") Instant endTime);
}

