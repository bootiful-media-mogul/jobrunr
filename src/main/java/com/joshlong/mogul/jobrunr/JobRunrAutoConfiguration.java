package com.joshlong.mogul.jobrunr;

import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.List;

@AutoConfiguration
class JobRunrAutoConfiguration {

	@ConditionalOnMissingBean
	@DependsOnDatabaseInitialization
	@Bean(name = "storageProvider", destroyMethod = "close")
	StorageProvider storageProvider(List<JobRequestHandler<?>> handlers, DataSource dataSource, JobMapper jobMapper,
			JobRunrProperties properties) {
		var database = properties.getDatabase();
		// var options = database.isSkipCreate() ? DatabaseOptions.SKIP_CREATE :
		// DatabaseOptions.CREATE;
		var storageProvider = new SignatureFilteringStorageProvider(dataSource, database.getTablePrefix(),
				DatabaseOptions.SKIP_CREATE, handlers);
		storageProvider.setJobMapper(jobMapper);
		return storageProvider;
	}

}
