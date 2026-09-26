package com.joshlong.mogul.jobrunr;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.spring.autoconfigure.storage.JobRunrSqlStorageAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.List;

/**
 * one set of JobRunr tables backs every service, so a node has to be told which jobs are
 * its own: see {@link SignatureFilteringStorageProvider}.
 */
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "jobrunr.database", name = "type", havingValue = "sql", matchIfMissing = true)
@AutoConfiguration(after = DataSourceAutoConfiguration.class, before = JobRunrSqlStorageAutoConfiguration.class)
class JobRunrAutoConfiguration {

	@ConditionalOnMissingBean
	@DependsOnDatabaseInitialization
	@Bean(name = "storageProvider", destroyMethod = "close")
	StorageProvider storageProvider(List<JobRequestHandler<?>> handlers, DataSource dataSource, JobMapper jobMapper,
			JobRunrProperties properties) {
		var database = properties.getDatabase();
		var options = database.isSkipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
		var storageProvider = new SignatureFilteringStorageProvider(dataSource, database.getTablePrefix(), options,
				handlers);
		storageProvider.setJobMapper(jobMapper);
		return storageProvider;
	}

}
