/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2014 All Rights Reserved.
 *
 * Author: wuya
 * Create Date: 2014年6月26日 下午4:35:16
 */
package com.github.stuxuhai.hdata.core;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.github.stuxuhai.hdata.api.Metric;
import com.github.stuxuhai.hdata.api.Record;
import com.github.stuxuhai.hdata.api.RecordCollector;
import com.github.stuxuhai.hdata.util.Utils;
import com.google.common.base.Stopwatch;

public class DefaultRecordCollector implements RecordCollector {

	private static final Logger LOGGER = LogManager.getLogger(DefaultRecordCollector.class);

	private static final long SLEEP_MILL_SECONDS = 1000;

	private final DefaultStorage storage;
	private final Metric metric;
	private final long flowLimit;
	private Stopwatch stopwatch = Stopwatch.createStarted();

	public DefaultRecordCollector(DefaultStorage storage, Metric metric, long flowLimit) {
		this.storage = storage;
		this.metric = metric;
		this.flowLimit = flowLimit;
		LOGGER.info("The flow limit is {} bytes/s.", this.flowLimit);
	}

	@Override
	public void send(Record record) {
		// 限速
		if (flowLimit > 0) {
			while (true) {
				long currentSpeed = metric.getSpeed();
				if (currentSpeed > flowLimit) {
					if (stopwatch.elapsed(TimeUnit.SECONDS) >= 5) {
						LOGGER.info("Current Speed is {} MB/s, sleeping...",
								String.format("%.2f", (double) currentSpeed / 1024 / 1024));
						stopwatch.reset();
					}
					Utils.sleep(SLEEP_MILL_SECONDS);
				} else {
					break;
				}
			}
		}

		storage.put(record);
		metric.getReadCount().incrementAndGet();

		if (flowLimit > 0) {
			metric.getReadBytes().addAndGet(RamUsageEstimator.sizeOf(record));
		}

	}

	@Override
	public void send(Record[] records) {
		storage.put(records);
	}
}
