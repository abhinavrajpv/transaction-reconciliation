package com.example.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.reconciliation.entity.BatchAudit;

public interface BatchAuditRepository extends JpaRepository<BatchAudit, Long> {

	boolean existsByJobInstanceId(Long jobInstanceId);

	Optional<BatchAudit> findByJobInstanceId(Long jobInstanceId);

	@Modifying
	@Query("""
			UPDATE BatchAudit a
			SET a.totalFirstFileRecords =
			        a.totalFirstFileRecords + :total,
			    a.matchedRecords =
			        a.matchedRecords + :matched,
			    a.mismatchedRecords =
			        a.mismatchedRecords + :mismatched,
			    a.invalidRecords =
			        a.invalidRecords + :invalid,
			    a.duplicateRecords =
			        a.duplicateRecords + :duplicate,
			    a.missingFromFirstFileRecords =
			        a.missingFromFirstFileRecords + :missing
			WHERE a.jobInstanceId = :jobInstanceId
			""")
	int incrementCounts(@Param("jobInstanceId") Long jobInstanceId, @Param("total") long total,
			@Param("matched") long matched, @Param("mismatched") long mismatched, @Param("invalid") long invalid,
			@Param("duplicate") long duplicate, @Param("missing") long missing);
}