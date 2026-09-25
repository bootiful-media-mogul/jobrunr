package com.joshlong.mogul.jobrunr;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.sql.common.JobTable;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.mapper.SqlJobPageRequestMapper;

import java.sql.Connection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_JOB_AS_JSON;

class SignatureFilteringJobTable extends JobTable {

	private final JobMapper jobMapper;

	private final SqlJobPageRequestMapper pageRequestMapper;

	private final String selectJobsToProcessStatement;

	SignatureFilteringJobTable(Connection connection, Dialect dialect, String tablePrefix, JobMapper jobMapper,
			String selectJobsToProcessStatement) {
		super(connection, dialect, tablePrefix, jobMapper);
		this.jobMapper = jobMapper;
		this.pageRequestMapper = new SqlJobPageRequestMapper(this, dialect);
		this.selectJobsToProcessStatement = selectJobsToProcessStatement;
	}

	@Override
	public List<Job> selectJobsToProcess(AmountRequest amountRequest) {
		// the suffix is where the ORDER BY, the LIMIT and the `for update skip locked`
		// come from; mapping the amount request also binds the `limit` parameter, so it
		// has to happen before the statement is executed -- which it does, as an
		// argument.
		return this.withState(ENQUEUED)
			.select(this.selectJobsToProcessStatement,
					this.pageRequestMapper.map(amountRequest) + this.dialect.selectForUpdateSkipLocked())
			.map(rs -> this.jobMapper.deserializeJob(rs.asString(FIELD_JOB_AS_JSON)))
			.collect(toList());
	}

}
