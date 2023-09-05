package com.myth.mysql2dm;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.myth.mysql2dm.filters.DmSupportVisitor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Myth
 * @date 2023/9/5 13:49
 */
public class Mysql2dmTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(Mysql2dmTest.class);

    @Test
    public void test01() {
        String originalSql = "SELECT DATE_ADD(now(),INTERVAL '2' DAY);";
        String modifiedSql = this.modifySql(originalSql);
        System.out.println();
    }

    private String modifySql(String originalSql) {
        try {
            SQLStatement sqlStatement = SQLUtils.parseSingleMysqlStatement(originalSql);
            DmSupportVisitor visitor = new DmSupportVisitor();
            sqlStatement.accept(visitor);
            String modifiedSql = sqlStatement.toString();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(StrUtil.format("dameng adapter modify success, origin sql: {}, \n" +
                        "actual sql: {}", originalSql, modifiedSql));
            } else {
                LOGGER.error(StrUtil.format("dameng adapter modify success, actual sql: {}", modifiedSql));
            }
            return modifiedSql;
        } catch (Exception e) {
            LOGGER.error(StrUtil.format("dameng adapter modify failed, sql: {}", originalSql), e);
            return originalSql;
        }
    }
}
