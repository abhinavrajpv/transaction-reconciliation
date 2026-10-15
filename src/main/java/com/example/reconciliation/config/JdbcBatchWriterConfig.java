package com.example.reconciliation.config;

import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.reconciliation.entity.TransactionRecord;

@Configuration
public class JdbcBatchWriterConfig {

	@Bean
	public JdbcBatchItemWriter<TransactionRecord> transactionJdbcBatchWriter(DataSource dataSource) {

		return new JdbcBatchItemWriterBuilder<TransactionRecord>().dataSource(dataSource).sql("""
				INSERT INTO transaction_record
				(transaction_id, account_number, transaction_date,
				 amount, currency, transaction_type, description)
				VALUES
				(:transactionId, :accountNumber, :transactionDate,
				 :amount, :currency, :transactionType, :description)
				""").beanMapped().build();
	}
}