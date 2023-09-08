package com.myth.mysql2dm.filters;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLAlterTableAddColumn;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author myth
 */
public class DmSupportVisitor extends MySqlASTVisitorAdapter {

    private final static Logger LOGGER = LoggerFactory.getLogger(DmDruidFilter.class);

    private final static String LOG_PREFIX = "-----达梦语句适配-----, ";

    /**
     * select列表项
     * 处理别名
     *
     * @param x
     * @return
     */
    @Override
    public boolean visit(SQLSelectItem x) {
        x.setAlias(this.replace(x.getAlias()));
        return true;
    }

    /**
     * 属性, tab1.col1、schema1.tab1
     *
     * @param x
     * @return
     */
    @Override
    public boolean visit(SQLPropertyExpr x) {
        x.setName(this.replace(x.getName()));
        return true;
    }

    /**
     * 替换属性符号, 数据库名、表名、列名等
     *
     * @param x
     * @return
     */
    @Override
    public boolean visit(SQLIdentifierExpr x) {
        x.setName(this.replace(x.getName()));
        return true;
    }

    /**
     * 表项
     *
     * @param x
     * @return
     */
    @Override
    public boolean visit(SQLExprTableSource x) {
        x.setAlias(this.replace(x.getAlias()));
        return true;
    }

    private String replace(String str) {
        String searchChar = "`";
        String replacementChar = "\"";
        if (StrUtil.contains(str, searchChar)) {
            LOGGER.debug(LOG_PREFIX + "转换特殊符号, `->\"");
            return StrUtil.replace(str, searchChar, replacementChar);
        }
        return str;
    }

    /**
     * 表达式处理
     *
     * @param x
     * @return
     */
    @Override
    public boolean visit(SQLAggregateExpr x) {
        String methodName = x.getMethodName();
        String groupConcatMysqlName = "GROUP_CONCAT";
        String groupConcatDmName = "LISTAGG";
        if (StrUtil.equalsIgnoreCase(methodName, groupConcatMysqlName)) {
            String message = StrUtil.format("原函数名: {}, 新函数名: {}", methodName, groupConcatDmName);
            if (CollUtil.size(x.getArguments()) == 1) {
                x.setMethodName(groupConcatDmName);
                if (x.getOrderBy() != null) {
                    x.setWithinGroup(true);
                }
                x.addArgument(new SQLCharExpr(StrPool.COMMA));
                if (CollUtil.isNotEmpty(x.getAttributes())) {
                    ReflectUtil.setFieldValue(x, "attributes", null);
                }
                LOGGER.debug(LOG_PREFIX + "替换函数名成功, " + message);
            } else {
                LOGGER.debug(LOG_PREFIX + "替换函数名失败, 参数只支持一个, " + message);
            }
        }
        return true;
    }

    /**
     * 方法执行处理
     *
     * @param x
     * @return
     */
    @Override
    public boolean visit(SQLMethodInvokeExpr x) {
        // 将IF转case when
        String methodName = x.getMethodName();
        methodName = methodName.toLowerCase();
        SQLObject parent = x.getParent();
        List<SQLExpr> arguments = x.getArguments();
        switch (methodName) {
            case "if":
                if (parent instanceof SQLReplaceable) {
                    SQLReplaceable sqlReplaceable = (SQLReplaceable) parent;
                    SQLExpr sqlExpr0 = arguments.get(0);
                    SQLExpr sqlExpr1 = arguments.get(1);
                    SQLExpr sqlExpr2 = arguments.get(2);
                    SQLCaseExpr sqlCaseExpr = new SQLCaseExpr();
                    sqlCaseExpr.addItem(new SQLCaseExpr.Item(sqlExpr0, sqlExpr1));
                    sqlCaseExpr.setElseExpr(sqlExpr2);
                    sqlReplaceable.replace(x, sqlCaseExpr);
                    LOGGER.debug(LOG_PREFIX + "IF转case when");
                    sqlExpr0.accept(this);
                    sqlExpr1.accept(this);
                    sqlExpr2.accept(this);
                    return false;
                }
                break;
            case "convert":
                if (parent instanceof SQLReplaceable) {
                    SQLReplaceable sqlReplaceable = (SQLReplaceable) parent;
                    SQLExpr sqlExpr0 = arguments.get(0);
                    SQLExpr sqlExpr1 = arguments.get(1);
                    if (sqlExpr1 instanceof SQLDataTypeRefExpr) {
                        SQLCastExpr sqlCastExpr = new SQLCastExpr(sqlExpr0, ((SQLDataTypeRefExpr) sqlExpr1).getDataType());
                        sqlReplaceable.replace(x, sqlCastExpr);
                        LOGGER.debug(LOG_PREFIX + "CONVERT转CAST");
                        sqlExpr0.accept(this);
                        sqlExpr1.accept(this);
                        return false;
                    }
                }
                break;
            case "date_format":
                SQLExpr sqlExpr = arguments.get(1);
                SQLCharExpr sqlCharExpr = (SQLCharExpr) sqlExpr;
                sqlCharExpr.setText(this.resolveForDateFormat(sqlCharExpr.getText()));
                LOGGER.debug(LOG_PREFIX + "date_format格式处理");
                return true;
            case "json_unquote":
                SQLExpr arg0 = arguments.get(0);
                x.setArgument(0, new SQLCharExpr("\""));
                x.setMethodName("TRIM");
                x.setFrom(arg0);
                LOGGER.debug(LOG_PREFIX + "JSON_UNQUOTE转TRIM");
                arg0.accept(this);
                return false;
            case "st_contains":
                x.setMethodName(StrUtil.concat(true, "dmgeo.", methodName));
                LOGGER.debug(LOG_PREFIX + "ST_CONTAINS转dmgeo.ST_CONTAINS");
                return true;
            case "st_distance_sphere":
                x.setMethodName(StrUtil.concat(true, "dmgeo.", "ST_Distance"));
                LOGGER.debug(LOG_PREFIX + "ST_DISTANCE_SPHERE转dmgeo.ST_Distance");
                return true;
            case "point":
                if (parent instanceof SQLReplaceable) {
                    SQLMethodInvokeExpr sqlMethodInvokeExpr = new SQLMethodInvokeExpr("dmgeo.ST_GeomFromText");
                    SQLMethodInvokeExpr concatMethod = new SQLMethodInvokeExpr("CONCAT");
                    concatMethod.addArgument(new SQLCharExpr("POINT("));
                    concatMethod.addArgument(arguments.get(0));
                    concatMethod.addArgument(new SQLCharExpr(String.valueOf(StrPool.C_SPACE)));
                    concatMethod.addArgument(arguments.get(1));
                    concatMethod.addArgument(new SQLCharExpr(")"));
                    sqlMethodInvokeExpr.addArgument(concatMethod);
                    sqlMethodInvokeExpr.addArgument(new SQLNumberExpr(0));
                    ((SQLReplaceable) parent).replace(x, sqlMethodInvokeExpr);
                    LOGGER.debug(LOG_PREFIX + "POINT转dmgeo.ST_GeomFromText(CONCAT('POINT(',arg0,arg1,)'))");
                    for (SQLExpr argument : arguments) {
                        argument.accept(this);
                    }
                    return false;
                }
                break;
            case "geometryfromtext":
                if (parent instanceof SQLReplaceable) {
                    SQLMethodInvokeExpr sqlMethodInvokeExpr = new SQLMethodInvokeExpr("dmgeo.ST_GeomFromText");
                    sqlMethodInvokeExpr.addArgument(arguments.get(0));
                    sqlMethodInvokeExpr.addArgument(new SQLNumberExpr(0));
                    ((SQLReplaceable) parent).replace(x, sqlMethodInvokeExpr);
                    LOGGER.debug(LOG_PREFIX + "geometry_from_text转dmgeo.ST_GeomFromText");
                    for (SQLExpr argument : sqlMethodInvokeExpr.getArguments()) {
                        argument.accept(this);
                    }
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    private String resolveForDateFormat(String content) {
        boolean flag = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char thisChar = content.charAt(i);
            if (thisChar == '%') {
                if (i == 0) {
                    sb.append(thisChar);
                } else {
                    sb.append("\"").append(thisChar);
                }
                flag = true;
            } else {
                if (flag) {
                    if (i == content.length() - 1) {
                        sb.append(thisChar);
                    } else {
                        sb.append(thisChar).append("\"");
                    }
                } else {
                    if (i == content.length() - 1) {
                        sb.append(thisChar).append("\"");
                    } else {
                        sb.append(thisChar);
                    }

                }
                flag = false;
            }
        }
        return sb.toString();
    }

    /**
     * 二元运算表达式处理
     *
     * @param x
     * @return
     */
    @Override
    public boolean visit(SQLBinaryOpExpr x) {
        if (x.getRight() instanceof SQLBooleanExpr) {
            boolean booleanValue = ((SQLBooleanExpr) x.getRight()).getBooleanValue();
            x.setRight(new SQLNumberExpr(booleanValue ? 1 : 0));
            LOGGER.debug(LOG_PREFIX + "boolean转0/1");
        }

        SQLObject parent = x.getParent();
        if (x.getOperator() == SQLBinaryOperator.Divide) {
            if (parent instanceof SQLReplaceable) {
                SQLMethodInvokeExpr sqlMethodInvokeExpr = new SQLMethodInvokeExpr("FUNC_OP_MY_DIVIDE");
                SQLExpr left = x.getLeft();
                SQLExpr right = x.getRight();
                sqlMethodInvokeExpr.addArgument(left);
                sqlMethodInvokeExpr.addArgument(right);
                ((SQLReplaceable) parent).replace(x, sqlMethodInvokeExpr);
                LOGGER.debug(LOG_PREFIX + "除法分母为0校验");
                left.accept(this);
                right.accept(this);
                return false;
            }
        } else if (x.getOperator() == SQLBinaryOperator.SubGt) {
            if (parent instanceof SQLReplaceable) {
                SQLMethodInvokeExpr sqlMethodInvokeExpr = new SQLMethodInvokeExpr("JSON_EXTRACT");
                SQLExpr left = x.getLeft();
                SQLExpr right = x.getRight();
                sqlMethodInvokeExpr.addArgument(left);
                sqlMethodInvokeExpr.addArgument(right);
                ((SQLReplaceable) parent).replace(x, sqlMethodInvokeExpr);
                LOGGER.debug(LOG_PREFIX + "JSON操作符->替换为JSON_EXTRACT");
                left.accept(this);
                right.accept(this);
                return false;
            }
        } else if (x.getOperator() == SQLBinaryOperator.SubGtGt) {
            if (parent instanceof SQLReplaceable) {
                SQLMethodInvokeExpr sqlMethodInvokeExpr = new SQLMethodInvokeExpr("JSON_VALUE");
                SQLExpr left = x.getLeft();
                SQLExpr right = x.getRight();
                sqlMethodInvokeExpr.addArgument(left);
                sqlMethodInvokeExpr.addArgument(right);
                ((SQLReplaceable) parent).replace(x, sqlMethodInvokeExpr);
                LOGGER.debug(LOG_PREFIX + "JSON操作符->>替换为JSON_VALUE");
                left.accept(this);
                right.accept(this);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean visit(SQLAlterTableAddColumn x) {
        // 兼容Mysql有afterColumn
        x.getColumns().forEach(col -> col.accept(this));
        if (x.getAfterColumn() != null) {
            x.getAfterColumn().accept(this);
        }
        return false;
    }

    @Override
    public boolean visit(SQLColumnDefinition x) {
        SQLDataType dataType = x.getDataType();
        if (dataType != null) {
            String name = dataType.getName();
            if (StrUtil.equalsAny(name, true, "bit", "int", "bigint", "smallint", "double") && CollUtil.isNotEmpty(dataType.getArguments())) {
                ReflectUtil.setFieldValue(dataType, "arguments", new ArrayList<>(0));
                LOGGER.debug(LOG_PREFIX + "bit/int等去除长度");
            }
        }
        return true;
    }

    @Override
    public boolean visit(SQLBinaryExpr x) {
        SQLObject parent = x.getParent();
        if (parent instanceof SQLReplaceable) {
            ((SQLReplaceable) parent).replace(x, new SQLCharExpr(x.getText()));
            LOGGER.debug(LOG_PREFIX + "b'0/1'转'0/1'");
        }
        return true;
    }

    @Override
    public boolean visit(SQLIntervalExpr x) {
        SQLExpr valueSqlExpr = x.getValue();
        if (valueSqlExpr instanceof SQLValuableExpr) {
            Object value = ((SQLValuableExpr) valueSqlExpr).getValue();
            x.setValue(new SQLCharExpr(Objects.toString(value)));
            LOGGER.debug(LOG_PREFIX + "INTERVAL expr unit中expr转字符");
            // todo expr若是非数字或字符串等特殊表达式, 将会得到错误的形式
            return false;
        }
        return true;
    }
}
