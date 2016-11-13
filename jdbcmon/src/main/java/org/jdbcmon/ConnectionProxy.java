package org.jdbcmon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;

class ConnectionProxy {
    private static final Class[] CONNECTION_PROXY_INTERFACES = {Connection.class};

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection delegate;
        private final SqlStat sqlStat;

        private ConnectionInvocationHandler(Connection delegate, SqlStat sqlStat) {
            this.delegate = delegate;
            this.sqlStat = sqlStat;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            Throwable exception = null;

            if (PreparedStatement.class.isAssignableFrom(method.getReturnType()) && "prepareStatement".equals(methodName)
                    && args.length > 0 && args[0] instanceof String) {
                String sql = (String) args[0];
                try {
                    PreparedStatement delegatePS = (PreparedStatement) method.invoke(delegate, args);
                    return PreparedStatementProxy.proxy(sql, delegatePS, sqlStat);
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    sqlStat.registerPrepare(sql, exception);
                }
            } else if (CallableStatement.class.isAssignableFrom(method.getReturnType()) && "prepareCall".equals(methodName)
                    && args.length > 0 && args[0] instanceof String) {
                String sql = (String) args[0];
                try {
                    CallableStatement delegateCS = (CallableStatement) method.invoke(delegate, args);
                    return CallableStatementProxy.proxy(sql, delegateCS, sqlStat);
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    sqlStat.registerPrepare(sql, exception);
                }
            }

            return method.invoke(delegate, args);
        }
    }

    static Connection proxy(Connection delegate, SqlStat sqlStat) {
        InvocationHandler handler = new ConnectionInvocationHandler(delegate, sqlStat);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), CONNECTION_PROXY_INTERFACES, handler);
    }
}
