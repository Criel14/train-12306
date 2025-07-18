# SpringCloud 高铁购票系统 后端部分

前端部分：https://github.com/Criel14/train-web

## 启动项目

本项目的组件都安装在windows下，以下说明全部基于windows，下面的配置是安装完这些组件后需要做的，如果提前配置过，可跳过。

### 运行代码

运行4个模块中的`xxxxApplication`类：gateway, member, business, batch

### MySQL

本项目使用的版本是 `8.0.36`

打开每个微服务模块的 `.properties` 配置文件，找到数据库配置信息，这里以`member`微服务模块为例：

```properties
# 配置数据库连接
spring.datasource.url=jdbc:mysql://localhost:3306/train_member?characterEncoding=utf8&autoReconnect=true&serverTimezone=Asia/Shanghai
spring.datasource.username=train_member_user
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

在数据库中创建对应的`train_member`数据库，并创建对应的用户`train_member_user`，并设置密码为`123456`，给予该用户操作`train_member`数据库的权限；其他微服务模块同理；
> 如果嫌麻烦，直接配置成你的root用户即可

完成后，执行项目根目录下 `sql/` 路径下的3个sql文件，注意先用 `use xxx` 指定数据库；

### Redis

本项目使用的版本是 `5.0.14.1`（很久以前下载的，有点老了，使用新的版本应该也没问题）

打开redis安装目录下的 `redis.windows.conf` 文件，配置密码：

```properties
requirepass 123456
```

执行命令启动redis:

```cmd
redis-server.exe redis.windows.conf
```

### Nacos

本项目使用的版本是 `2.5.1`

打开cmd，cd到nacos的安装目录下的`bin/`路径下，启动命令（standalone代表着单机模式运行，非集群模式）：

```powershell
startup.cmd -m standalone
```

在nacos中创建命名空间namespace，自动生成命名空间ID，修改配置文件中的配置项（在IDEA中 `ctrl + shift + F` 全局搜索配置即可）：
```properties
spring.cloud.nacos.config.namespace=命名空间ID
```

### Seata

本项目使用的版本是 `2.0.0`

#### 启动Seata：

双击或在cmd中运行seata安装目录下的`/bin/seata-server.bat`文件即可

#### 配置seata连接nacos

在nacos的配置列表下，项目的命名空间内，创建`seataServer.properties`配置文件

修改seata配置文件，如下，分别是**配置中心**和**注册中心**：

```yaml
seata:
  config:
    # support: nacos, consul, apollo, zk, etcd3
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: 02521b8e-52fa-430e-abe7-b73a7d68574e
      dataId: seataServer.properties
      # nacos没开启鉴权，下面的2个就不用加
      #username: nacos
      #password: nacos
  registry:
    # support: nacos, eureka, redis, zk, consul, etcd3, sofa
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: 02521b8e-52fa-430e-abe7-b73a7d68574e
      # nacos没开启鉴权，下面的2个就不用加
      #username: nacos
      #password: nacos
```

#### 配置数据库

项目中使用Seata的**AT模式**，需要在用到Seata的微服务模块中创建`undo_log`表（建表sql已在项目中的sql文件中和其他表一同给出，直接执行即可），表结构如下：

```sql
CREATE TABLE `undo_log`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint(20)   NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int(11)      NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    `ext`           varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8;
```

修改配置文件`/conf/application.yml`，这里需要手动新建数据库`seata`，新建用户`seata_user`并设置密码为`123456`，给予该用户操作`seata`数据库的权限
> 如果嫌麻烦，直接配置成你的root用户即可

```properties
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.jdbc.Driver
store.db.url=jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true&rewriteBatchedStatements=true
store.db.user=seata_user
store.db.password=123456
```

在配置的数据库中，执行seata提供的语句，路径是`/script/server/db/mysql.sql`
> 执行上述sql前，记得先选中数据库
> ```sql
> use seata;
> ```

> 如果运行出现错误，请在nacos的`seataServer.properties`配置文件里加上以下内容：
> 
> ```properties
> store.db.globalTable=global_table
> store.db.branchTable=branch_table
> store.db.distributedLockTable=distributed_lock
> store.db.lockTable=lock_table
> ```
> 
> 如果还是有问题，再加上以下配置：
> 
> ```properties
> server.enableParallelRequestHandle=false
> ```


### Sentinel控台（不做也可以运行项目）

本项目使用的版本是 `1.8.8`

下载控台的jar包，在同个目录下，执行命令（其中，`18080`是自定义端口，和项目中的配置文件中的信息需要保持一致）：

```cmd
java -Dserver.port=18080 -Dcsp.sentinel.dashboard.server=localhost:18080 -Dproject.name=sentinel-dashboard -jar sentinel-dashboard-1.8.8.jar
```

### RocketMQ

本项目使用的版本是 `5.3.2`

配置环境变量（系统变量）

```
NAMESRV_ADDR = localhost:9876
ROCKETMQ_HOME = xxx\xxx\rocketmq-all-5.3.2-bin-release
```

按顺序启动`bin/`下的2个服务（双击或在cmd运行），第一个是`mqnamesrv.cmd`，第二个是`mqbroker.cmd`，可以编辑里面的内容，修改所需的内存大小等信息；




