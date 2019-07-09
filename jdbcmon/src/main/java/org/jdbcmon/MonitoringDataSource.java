package org.jdbcmon;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MonitoringDataSource implements DataSource {

    private final DataSource delegate;
    private final SqlStat sqlStat;

    public MonitoringDataSource(DataSource delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.sqlStat = new SqlStat();
    }

    public List<Map<String, ?>> report() {
        return sqlStat.report(null, false);
    }

    public void reset() {
        sqlStat.reset();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection delegateConnection = delegate.getConnection();
        return ConnectionProxy.proxy(delegateConnection, sqlStat);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection delegateConnection = delegate.getConnection(username, password);
        return ConnectionProxy.proxy(delegateConnection, sqlStat);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
