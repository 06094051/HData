/*
 * Author: wuya
 * Create Date: 2014年6月26日 下午4:35:16
 */
package com.github.stuxuhai.hdata.core;

import com.github.stuxuhai.hdata.plugin.Record;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class Storage {

    private final Disruptor<RecordEvent> disruptor;
    private final RingBuffer<RecordEvent> ringBuffer;

    private static final EventTranslatorOneArg<RecordEvent, Record> TRANSLATOR = new EventTranslatorOneArg<RecordEvent, Record>() {

        @Override
        public void translateTo(RecordEvent event, long sequence, Record record) {
            event.setRecord(record);
        }
    };

    public Storage(Disruptor<RecordEvent> disruptor, RecordWorkHandler[] handlers, JobContext context) {
        this.disruptor = disruptor;
        disruptor.handleExceptionsWith(new RecordEventExceptionHandler(disruptor, context));
        disruptor.handleEventsWithWorkerPool(handlers);
        ringBuffer = disruptor.start();
    }

    public void put(Record record) {
        disruptor.publishEvent(TRANSLATOR, record);
    }

    public void put(Record[] records) {
        for (Record record : records) {
            put(record);
        }
    }

    public boolean isEmpty() {
        return ringBuffer.remainingCapacity() == ringBuffer.getBufferSize();
    }

    public int size() {
        return ringBuffer.getBufferSize();
    }

    public void close() {
        disruptor.shutdown();
    }
}
