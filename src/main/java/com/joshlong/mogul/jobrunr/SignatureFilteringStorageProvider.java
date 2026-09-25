package com.joshlong.mogul.jobrunr;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.JobTable;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;

class SignatureFilteringStorageProvider extends PostgresStorageProvider {

	private final String selectJobsToProcessStatement;

	private JobMapper jobMapper;

	SignatureFilteringStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions,
			Collection<JobRequestHandler<?>> handlers) {
		super(dataSource, tablePrefix, databaseOptions);
		Assert.notEmpty(handlers, "there must be at least one job request handler: a node that is allowed to run "
				+ "nothing would be an expensive way to poll a database");
		var jobSignatures = jobSignatures(handlers).stream().map(s -> "'" + s + "'").collect(joining(" , "));

		this.selectJobsToProcessStatement = " jobAsJson from jobrunr_jobs where state = :state and jobSignature in ("
				+ jobSignatures + ") ";
	}

	private String jobSignature(Class<?> handler, Class<?> request) {
		return handler.getName() + ".run(" + request.getName() + ")";
	}

	private Set<String> jobSignatures(Collection<JobRequestHandler<?>> handlers) {
		var signatures = new HashSet<String>();
		for (var handler : handlers) {
			var handlerClass = AopProxyUtils.ultimateTargetClass(handler);
			var requestClass = ResolvableType.forClass(JobRequestHandler.class, handlerClass).getGeneric(0).resolve();
			Assert.state(requestClass != null && JobRequest.class.isAssignableFrom(requestClass),
					() -> "could not work out which " + JobRequest.class.getName() + " the handler ["
							+ handlerClass.getName() + "] takes. it must bind the type variable on "
							+ JobRequestHandler.class.getName() + " to a concrete type");
			signatures.add(jobSignature(handlerClass, requestClass));
		}
		return signatures;
	}

	@Override
	public void setJobMapper(JobMapper jobMapper) {
		super.setJobMapper(jobMapper);
		this.jobMapper = jobMapper;
	}

	@Override
	protected JobTable jobTable(Connection connection) {
		return new SignatureFilteringJobTable(connection, this.dialect, this.tablePrefix, this.jobMapper,
				this.selectJobsToProcessStatement);
	}

}
