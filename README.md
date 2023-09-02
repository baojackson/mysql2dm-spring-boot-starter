# mysql2dm-spring-boot-starter

#### 介绍
用于基于MySQL的项目适配达梦数据库。

#### 软件架构
基于Druid里提供的AST工具和Filter实现。在数据库连接层拦截，上层无感知，可以用mybatis、jpa甚至jdbcTemplate都可以拦截到。

Druid提供的AST开发简单、灵活、并且性能达到生产级别。


#### 安装教程

将本工程install到本地maven仓库后，再待改造的项目引入pom即可
```
<dependency>
      <groupId>com.myth</groupId>
      <artifactId>mysql2dm-spring-boot-starter</artifactId>
      <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 使用说明

1. 工具中会自动判断当前数据库环境是否是达梦数据库，是的时候才会拦截修改

#### <a href="./DETAIL.md" title="详细文档">详细文档</a>

