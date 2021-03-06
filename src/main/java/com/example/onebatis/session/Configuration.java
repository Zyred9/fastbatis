package com.example.onebatis.session;

import com.example.onebatis.DataSource;
import com.example.onebatis.binding.MapperRegistry;
import com.example.onebatis.builder.SqlBuilder;
import com.example.onebatis.executor.Executor;
import com.example.onebatis.executor.impl.CachingExecutor;
import com.example.onebatis.executor.impl.SimpleExecutor;
import com.example.onebatis.logging.Log;
import com.example.onebatis.plugin.Interceptor;
import com.example.onebatis.plugin.InterceptorChain;
import com.example.onebatis.pool.ConnectionPool;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * <p>
 * 总配置文件
 * </p>
 *
 * @author zyred
 * @createTime 2020/9/23 13:54
 **/
public final class Configuration {

    /** 全局缓存开关 **/
    private String cacheEnabled;
    /** 日志实现类  com.example.onebatis.logging.stdout.StdOutImpl **/
    private Log logger;
    /** 懒加载 **/
    private String lazyLoadingEnabled;
    /** 连接信息 **/
    private DataSource dataSource;
    /** 其实这里面就包含了连接信息 **/
    private ResourceBundle properties;
    /** 数据库连接池 **/
    private ConnectionPool connectionPool;
    /** 插件 **/
    private final InterceptorChain interceptorChain = new InterceptorChain();

    /** statementId -> sql **/
    private final HashMap<String, SqlBuilder> sqlMapping = new HashMap<>();
    /** Mapper注册 **/
    private final MapperRegistry mapperRegistry = new MapperRegistry(this);

    /** sql拦截器，主要是插件才使用 **/
    public void addInterceptor(Interceptor i) {
        interceptorChain.addInterceptor(i);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setProperties(ResourceBundle properties) {
        this.properties = properties;
    }

    public ResourceBundle getProperties() {
        return this.properties;
    }

    public void addSqlMapping(String statementId, SqlBuilder sql) {
        this.sqlMapping.put(statementId, sql);
    }

    public void addMapper(Class<?> clazz) {
        this.mapperRegistry.addMapper(clazz);
    }

    public boolean hasMapper(Class<?> clazz) {
        return this.mapperRegistry.hasMapper(clazz);
    }

    public boolean hasStatement(String statementId) {
        return this.sqlMapping.containsKey(statementId);
    }

    public SqlBuilder getMappedStatement(String statementId) {
        return sqlMapping.get(statementId);
    }

    /**
     * 初始化执行器和插件注入
     *
     * @param autoCommit 自动注入
     * @return sql执行器
     */
    public Executor newExecutor(boolean autoCommit) {
        // 简单sql执行器
        Executor ex = new SimpleExecutor(this, autoCommit);
        if (Objects.deepEquals(cacheEnabled, "true")) {
            // 这里使用了委托模式，来增强执行器对象
            ex = new CachingExecutor(ex);
        }

        // 通过动态代理，重新生成 executor
        ex = (Executor) this.interceptorChain.pluginAll(ex);
        return ex;
    }

    public void setConnectionPool(ConnectionPool conn) {
        this.connectionPool = conn;
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public MapperRegistry getMapperRegistry() {
        return mapperRegistry;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }


    @SuppressWarnings("unchecked")
    public void setLogImpl(String logImpl) {
        if (logImpl == null || logImpl.equals("")) {
            return;
        }
        try {
            Class<Log> implClazz = (Class<Log>) Class.forName(logImpl);
            Constructor<Log> implConstructor = implClazz.getConstructor(String.class);
            this.logger = implConstructor.newInstance("LogFactory");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
