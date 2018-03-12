package org.jdbcmon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Statement;

import static org.jdbcmon.Utils.invokeTarget;
import static org.jdbcmon.Utils.matches;

class StatementProxy {

    private static final Class[] STATEMENT_PROXY_INTERFACES = {Statement.class};

    static class StatementInvocationHandler<T extends Statement> implements InvocationHandler {
        final T delegate;
        final SqlStat sqlStat;

        StatementInvocationHandler(T delegate, SqlStat sqlStat) {
            this.delegate = delegate;
            this.sqlStat = sqlStat;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Throwable exception = null;

            if (matches(method, "executeQuery", String.class)) {
                String sql = (String) args[0];
                long startNanos = System.nanoTime();
                try {
                    return invokeTarget(method, delegate, args);
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    sqlStat.registerExecute(sql, System.nanoTime() - startNanos, exception);
                }
            }

            return invokeTarget(method, delegate, args);
        }
    }

    static Statement proxy(Statement delegate, SqlStat sqlStat) {
        InvocationHandler handler = new StatementInvocationHandler<>(delegate, sqlStat);

        return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
                STATEMENT_PROXY_INTERFACES, handler);
    }
}
