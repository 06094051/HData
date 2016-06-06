/*
 * 蘑菇街 Inc.
 * Copyright (c) 2010-2014 All Rights Reserved.
 *
 * Author: wuya
 * Create Date: 2014年6月27日 下午4:02:30
 */
package com.github.stuxuhai.hdata.plugin.writer.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringEscapeUtils;

import com.github.stuxuhai.hdata.api.Fields;
import com.github.stuxuhai.hdata.api.JobContext;
import com.github.stuxuhai.hdata.api.PluginConfig;
import com.github.stuxuhai.hdata.api.Record;
import com.github.stuxuhai.hdata.api.Writer;
import com.github.stuxuhai.hdata.exception.HDataException;
import com.google.common.base.Preconditions;

/**
 * @author wuya
 *
 */
public class CSVWriter extends Writer {

	private String path = null;
	private String encoding = null;
	private String separator = null;
	private java.io.Writer writer;
	private CSVPrinter csvPrinter;
	private Fields fields;
	private boolean showColumns;
	private boolean showTypesAndComments;
	private String[] types;
	private String[] comments;
	private List<Object> csvList = new ArrayList<Object>();
	private static AtomicInteger sequence = new AtomicInteger(0);
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final Pattern REG_FILE_PATH_WITHOUT_EXTENSION = Pattern.compile(".*?(?=\\.\\w+$)");
	private static final Pattern REG_FILE_EXTENSION = Pattern.compile("(\\.\\w+)$");

	@Override
	public void prepare(JobContext context, PluginConfig writerConfig) {
		path = writerConfig.getString(CSVWriterProperties.PATH);
		Preconditions.checkNotNull(path, "CSV writer required property: path");

		encoding = writerConfig.getString(CSVWriterProperties.ENCODING, "UTF-8");
		separator = StringEscapeUtils.unescapeJava(writerConfig.getString(CSVWriterProperties.SEPARATOR, ","));

		fields = context.getFields();
		showColumns = writerConfig.getBoolean(CSVWriterProperties.SHOW_COLUMNS, false);
		showTypesAndComments = writerConfig.getBoolean(CSVWriterProperties.SHOW_TYPES_AND_COMMENTS, false);
		if (showTypesAndComments) {
			types = context.getJobConfig().getString("types").split("\001");
			comments = context.getJobConfig().getString("comments").split("\001");
		}

		int parallelism = writerConfig.getParallelism();
		if (parallelism > 1) {
			String filePathWithoutExtension = "";
			String fileExtension = "";
			Matcher m1 = REG_FILE_PATH_WITHOUT_EXTENSION.matcher(path.trim());
			if (m1.find()) {
				filePathWithoutExtension = m1.group();
			}

			Matcher m2 = REG_FILE_EXTENSION.matcher(path.trim());
			if (m2.find()) {
				fileExtension = m2.group();
			}
			path = String.format("%s_%04d%s", filePathWithoutExtension, sequence.getAndIncrement(), fileExtension);
		}

		try {
			writer = new OutputStreamWriter(new FileOutputStream(path), encoding);
		} catch (Exception e) {
			throw new HDataException(e);
		}
	}

	@Override
	public void execute(Record record) {
		if (csvPrinter == null) {
			try {
				csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(separator.charAt(0)));
				if (showTypesAndComments) {
					for (String type : types) {
						csvList.add(type);
					}
					csvPrinter.printRecord(csvList);
					csvList.clear();

					for (String comment : comments) {
						csvList.add(comment);
					}
					csvPrinter.printRecord(csvList);
					csvList.clear();
				}

				if (showColumns) {
					for (Object object : fields) {
						csvList.add(object);
					}
					csvPrinter.printRecord(csvList);
					csvList.clear();
				}
			} catch (IOException e) {
				throw new HDataException(e);
			}
		}

		for (int i = 0, len = record.size(); i < len; i++) {
			Object obj = record.get(i);
			if (obj instanceof Timestamp) {
				csvList.add(dateFormat.format(obj));
			} else {
				csvList.add(obj);
			}
		}

		try {
			csvPrinter.printRecord(csvList);
		} catch (IOException e) {
			throw new HDataException(e);
		}
		csvList.clear();
	}

	@Override
	public void close() {
		if (csvPrinter != null) {
			try {
				csvPrinter.close();
			} catch (IOException e) {
				throw new HDataException(e);
			}
		}

		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				throw new HDataException(e);
			}
		}
	}

}
