package study.shop;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

// 현재 테스트 스레드의 Hibernate SQL만 기록합니다. SQL 자체는 바꾸지 않습니다.
public class SqlCapture implements StatementInspector {
    private static final ThreadLocal<List<String>> STATEMENTS =
            ThreadLocal.withInitial(ArrayList::new);

    @Override
    public String inspect(String sql) {
        STATEMENTS.get().add(sql);
        return sql;
    }

    static void clear() { STATEMENTS.get().clear(); }
    static List<String> selects() {
        return STATEMENTS.get().stream()
                .filter(sql -> sql.stripLeading().startsWith("select ")).toList();
    }
    static void finish() { STATEMENTS.remove(); }
}
