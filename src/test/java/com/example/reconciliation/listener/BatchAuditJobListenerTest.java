
package com.example.reconciliation.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.reconciliation.entity.BatchAudit;
import com.example.reconciliation.repository.BatchAuditRepository;

@ExtendWith(MockitoExtension.class)
class BatchAuditJobListenerTest {

	@Mock
	private BatchAuditRepository auditRepository;

	@Mock
	private JobExecution jobExecution;

	@Mock
	private JobInstance jobInstance;

	private BatchAuditJobListener listener;

	@BeforeEach
	void setUp() {
		listener = new BatchAuditJobListener(auditRepository);

		ReflectionTestUtils.setField(listener, "firstFile", "C:/batch/input/first.csv");
		ReflectionTestUtils.setField(listener, "secondFile", "C:/batch/input/second.csv");
	}

	@Test
	void shouldCreateAuditForNewJobInstance() {
		LocalDateTime started = LocalDateTime.of(2026, 10, 16, 10, 0);

		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);
		when(auditRepository.existsByJobInstanceId(42L)).thenReturn(false);
		when(jobExecution.getStartTime()).thenReturn(started);

		listener.beforeJob(jobExecution);

		ArgumentCaptor<BatchAudit> captor = ArgumentCaptor.forClass(BatchAudit.class);

		verify(auditRepository).save(captor.capture());

		BatchAudit audit = captor.getValue();

		assertEquals(42L, audit.getJobInstanceId());
		assertEquals("first.csv", audit.getFirstFileName());
		assertEquals("second.csv", audit.getSecondFileName());
		assertEquals(0L, audit.getTotalFirstFileRecords());
		assertEquals(0L, audit.getMatchedRecords());
		assertEquals(0L, audit.getMismatchedRecords());
		assertEquals(0L, audit.getInvalidRecords());
		assertEquals(0L, audit.getDuplicateRecords());
		assertEquals(0L, audit.getMissingFromFirstFileRecords());
		assertEquals(started, audit.getStartedAt());
		assertEquals("STARTED", audit.getStatus());
		assertNull(audit.getCompletedAt());
	}

	@Test
	void shouldUseCurrentTimeWhenJobStartTimeIsNull() {
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);
		when(auditRepository.existsByJobInstanceId(42L)).thenReturn(false);
		when(jobExecution.getStartTime()).thenReturn(null);

		LocalDateTime before = LocalDateTime.now();

		listener.beforeJob(jobExecution);

		LocalDateTime after = LocalDateTime.now();

		ArgumentCaptor<BatchAudit> captor = ArgumentCaptor.forClass(BatchAudit.class);

		verify(auditRepository).save(captor.capture());

		LocalDateTime actual = captor.getValue().getStartedAt();

		assertNotNull(actual);
		assertFalse(actual.isBefore(before));
		assertFalse(actual.isAfter(after));
	}

	@Test
	void shouldPreserveAuditWhenRestartingExistingInstance() {
		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);
		when(auditRepository.existsByJobInstanceId(42L)).thenReturn(true);

		listener.beforeJob(jobExecution);

		verify(auditRepository).existsByJobInstanceId(42L);
		verify(auditRepository, never()).save(any(BatchAudit.class));
	}

	@Test
	void shouldUpdateAuditStatusAndCompletionTimeAfterJob() {
		LocalDateTime completed = LocalDateTime.of(2026, 10, 16, 11, 0);

		BatchAudit audit = new BatchAudit();
		audit.setJobInstanceId(42L);
		audit.setStatus("STARTED");

		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
		when(jobExecution.getEndTime()).thenReturn(completed);
		when(auditRepository.findByJobInstanceId(42L)).thenReturn(Optional.of(audit));

		listener.afterJob(jobExecution);

		assertEquals("COMPLETED", audit.getStatus());
		assertEquals(completed, audit.getCompletedAt());

		verify(auditRepository).save(audit);
	}

	@Test
	void shouldLeaveCompletionTimeUnchangedWhenEndTimeIsNull() {
		LocalDateTime previous = LocalDateTime.of(2026, 10, 15, 9, 0);

		BatchAudit audit = new BatchAudit();
		audit.setCompletedAt(previous);

		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);
		when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
		when(jobExecution.getEndTime()).thenReturn(null);
		when(auditRepository.findByJobInstanceId(42L)).thenReturn(Optional.of(audit));

		listener.afterJob(jobExecution);

		assertEquals("FAILED", audit.getStatus());
		assertEquals(previous, audit.getCompletedAt());

		verify(auditRepository).save(audit);
	}

	@Test
	void shouldNotSaveAnythingAfterJobWhenAuditDoesNotExist() {

		when(jobExecution.getJobInstance()).thenReturn(jobInstance);
		when(jobInstance.getInstanceId()).thenReturn(42L);

		when(auditRepository.findByJobInstanceId(42L)).thenReturn(Optional.empty());

		listener.afterJob(jobExecution);

		verify(auditRepository).findByJobInstanceId(42L);
		verify(auditRepository, never()).save(any(BatchAudit.class));
	}

}
