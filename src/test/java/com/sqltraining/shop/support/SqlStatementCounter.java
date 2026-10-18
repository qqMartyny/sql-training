package com.sqltraining.shop.support;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Считает реальные обращения к БД на уровне JDBC (через datasource-proxy).
 * <p>
 * roundTrips — сколько раз приложение «сходило» в БД (один execute или один executeBatch = 1).
 * selects/inserts/updates/deletes — количество выполненных команд с учётом всех строк batch'а.
 */
public class SqlStatementCounter implements QueryExecutionListener {

    private final AtomicLong roundTrips = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final AtomicLong selects = new AtomicLong();
    private final AtomicLong inserts = new AtomicLong();
    private final AtomicLong updates = new AtomicLong();
    private final AtomicLong deletes = new AtomicLong();
    private final List<String> executed = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        roundTrips.incrementAndGet();
        if (execInfo.isBatch()) {
            batches.incrementAndGet();
        }
        for (QueryInfo query : queryInfoList) {
            long executions = Math.max(1, query.getParametersList().size());
            if (executed.size() < 10_000) {
                executed.add(query.getQuery());
            }
            String sql = query.getQuery().stripLeading().toLowerCase(Locale.ROOT);
            if (sql.startsWith("select") || sql.startsWith("with")) {
                selects.addAndGet(executions);
            } else if (sql.startsWith("insert")) {
                inserts.addAndGet(executions);
            } else if (sql.startsWith("update")) {
                updates.addAndGet(executions);
            } else if (sql.startsWith("delete")) {
                deletes.addAndGet(executions);
            }
        }
    }

    public void reset() {
        roundTrips.set(0);
        batches.set(0);
        selects.set(0);
        inserts.set(0);
        updates.set(0);
        deletes.set(0);
        executed.clear();
    }

    /** Тексты выполненных SQL с момента последнего reset() — удобно печатать при отладке. */
    public List<String> executedQueries() {
        synchronized (executed) {
            return List.copyOf(executed);
        }
    }

    public SqlStats snapshot() {
        return new SqlStats(roundTrips.get(), batches.get(), selects.get(), inserts.get(), updates.get(), deletes.get());
    }

    public record SqlStats(long roundTrips, long batches, long selects, long inserts, long updates, long deletes) {
    }
}
