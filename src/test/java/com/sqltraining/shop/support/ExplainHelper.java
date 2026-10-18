package com.sqltraining.shop.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Получение плана выполнения запроса (EXPLAIN) в тестах.
 * <p>
 * Тестовая БД маленькая, и на ней планировщик честно выбирает Seq Scan даже при наличии индекса.
 * Поэтому план строится с {@code enable_seqscan = off}: так мы проверяем, что подходящий индекс
 * <b>существует и может быть использован</b>, а не то, что он выгоднее на текущем объёме данных.
 */
public class ExplainHelper {

    private static final Pattern NAMED_PARAM = Pattern.compile("(?<!:):(\\w+)");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public ExplainHelper(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    /** Текстовый план запроса с подставленными литералами вместо именованных параметров (:name). */
    public String explain(String sql, Map<String, String> literals, boolean disableSeqScan) {
        String prepared = substitute(sql, literals);
        return tx.execute(status -> {
            if (disableSeqScan) {
                jdbc.execute("set local enable_seqscan = off");
            }
            List<String> lines = jdbc.queryForList("explain " + prepared, String.class);
            return String.join("\n", lines);
        });
    }

    private static String substitute(String sql, Map<String, String> literals) {
        Matcher m = NAMED_PARAM.matcher(sql);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = literals.get(m.group(1));
            if (value == null) {
                throw new IllegalArgumentException("No literal for parameter :" + m.group(1));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
