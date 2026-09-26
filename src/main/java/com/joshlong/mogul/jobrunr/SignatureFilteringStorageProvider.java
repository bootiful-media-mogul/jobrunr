package com.joshlong.mogul.jobrunr;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.sql.common.JobTable;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;

class SignatureFilteringStorageProvider extends PostgresStorageProvider {

	private final String selectJobsToProcessStatement;

	private JobMapper jobMapper;

	private final Logger log = LoggerFactory.getLogger(getClass());

	SignatureFilteringStorageProvider(DataSource dataSource, String tablePrefix, DatabaseOptions databaseOptions,
			Collection<JobRequestHandler<?>> handlers) {
		super(dataSource, tablePrefix, databaseOptions);
		Assert.notEmpty(handlers, "there must be at least one job request handler.");
		var jobSignatures = jobSignatures(handlers) //
			.stream() //
			.map(s -> "'" + s + "'") //
			.collect(joining(" , "));
		this.log.debug("jobSignatures: {}", jobSignatures);
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
			var requestClass = ResolvableType //
				.forClass(JobRequestHandler.class, handlerClass) //
				.getGeneric(0) //
				.resolve();
			// when there's no type argument left to read -- a lambda, or a raw
			// JobRequestHandler -- ResolvableType falls back to the type variable's bound
			// and hands back the JobRequest interface itself rather than null. left alone
			// that becomes a signature matching nothing: a node that starts, polls, and
			// quietly never works.
			Assert.state(this.isConcreteJobRequest(requestClass),
					() -> "could not work out which " + JobRequest.class.getSimpleName() + " the handler ["
							+ handlerClass.getName() + "] handles; it has to name one as a type argument, so no "
							+ "lambdas and no raw " + JobRequestHandler.class.getSimpleName());
			this.validate(JobRequestHandler.class, handlerClass);
			var jobSignature = this.jobSignature(handlerClass, requestClass);
			signatures.add(jobSignature);
		}
		return signatures;
	}

	private boolean isConcreteJobRequest(@Nullable Class<?> requestClass) {
		return requestClass != null && JobRequest.class.isAssignableFrom(requestClass) && !requestClass.isInterface()
				&& !Modifier.isAbstract(requestClass.getModifiers());
	}

	private void validate(@NonNull Class<?> target, @NonNull Class<?> assignableFrom) {
		Assert.state(target.isAssignableFrom(assignableFrom),
				() -> assignableFrom.getName() + " is not an instance of  " + target.getName());
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
