package com.myth.mysql2dm.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceWrapper;
import com.myth.mysql2dm.filters.DmDruidFilter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(DruidDataSourceAutoConfigure.class)
@ConditionalOnClass(DruidDataSourceWrapper.class)
public class Mysql2dmAutoConfiguration {

    @Bean
    public DmDruidFilter dmDruidFilter() {
        return new DmDruidFilter();
    }
}
