package org.jdbcmon;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;

class CallableStatementProxy {

    private static final Class[] CALLABLE_STATEMENT_PROXY_INTERFACES = {CallableStatement.class};

    static class CallableStatementInvocationHandler<T extends CallableStatement> extends PreparedStatementProxy.PreparedStatementInvocationHandler<T> {
        CallableStatementInvocationHandler(String sql, T delegate, SqlStat sqlStat) {
            super(sql, delegate, sqlStat);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Throwable exception = null;

            return super.invoke(proxy, method, args);
        }
    }

    static CallableStatement proxy(String sql, CallableStatement delegate, SqlStat sqlStat) {
        InvocationHandler handler = new CallableStatementInvocationHandler<>(sql, delegate, sqlStat);

        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                CALLABLE_STATEMENT_PROXY_INTERFACES, handler);
    }
}
