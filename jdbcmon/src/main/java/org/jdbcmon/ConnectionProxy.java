package org.jdbcmon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;

class ConnectionProxy {
    private static final Class[] CONNECTION_PROXY_INTERFACES = {Connection.class};

    static Connection proxy(Connection delegate, SqlStat sqlStat) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            Throwable exception = null;
            String sql = null;

            if (PreparedStatement.class.isAssignableFrom(method.getReturnType()) && "prepareStatement".equals(methodName)
                    && args.length > 0 && args[0] instanceof String) {
                sql = (String) args[0];
                try {
                    PreparedStatement delegatePS = (PreparedStatement) method.invoke(delegate, args);
                    return PreparedStatementProxy.proxy(sql, delegatePS, sqlStat);
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    sqlStat.registerPrepare(sql, exception);
                }
            }

            return method.invoke(delegate, args);
        };

        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), CONNECTION_PROXY_INTERFACES, handler);
    }
}
