package org.jdbcmon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.jdbcmon.Utils.matches;

class PreparedStatementProxy {
    private static final Class[] PREPARED_STATEMENT_PROXY_INTERFACES = {PreparedStatement.class};

    static class PreparedStatementInvocationHandler<T extends PreparedStatement> extends StatementProxy.StatementInvocationHandler<T> {
        final String sql;

        PreparedStatementInvocationHandler(String sql, T delegate, SqlStat sqlStat) {
            super(delegate, sqlStat);
            this.sql = sql;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Throwable exception = null;

            if (matches(method, boolean.class, "execute")) {
                long startNanos = System.nanoTime();
                try {
                    return delegate.execute();
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    sqlStat.registerExecute(sql, System.nanoTime() - startNanos, exception);
                }
            } else if (matches(method, ResultSet.class, "executeQuery")) {
                long startNanos = System.nanoTime();
                try {
                    return delegate.executeQuery();
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    sqlStat.registerExecute(sql, System.nanoTime() - startNanos, exception);
                }
            }

            return super.invoke(proxy, method, args);
        }
    }

    static PreparedStatement proxy(String sql, PreparedStatement delegate, SqlStat sqlStat) {
        InvocationHandler handler = new PreparedStatementInvocationHandler<>(sql, delegate, sqlStat);

        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                PREPARED_STATEMENT_PROXY_INTERFACES, handler);
    }
}
