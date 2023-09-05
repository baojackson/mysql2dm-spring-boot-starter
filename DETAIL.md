# 详细文档

## 已支持功能

### \`替换为\"

> 说明
> * MySQL用\`包裹关键字，而达梦中用的是\"
> * MySQL和达梦关键字集并不一样，遇到的时候加上\`即可
> * 目前支持
>> 1. 数据库名、表名和列名
>> 2. 表别名和列别名
>> 3. 属性名, 形如 tab1.col1、schema1.tab1, col1、tab1即属性名

### GROUP_CONCAT转LISTAGG

> 说明
>
> 仅支持一个参数的情况, 例如GROUP_CONCAT(col1, col2)是不支持的, 但是可以修改sql改成GROUP_CONCAT(CONCAT(col1, '_', col2))

### IF转CASE WHEN

> ** 说明
>
> 达梦IF中类型校验严格, 需要转换写法

### CONVERT转CAST

> 说明
>
> 达梦不支持前者

### json_unquote转TRIM

> 说明
>
> 达梦不支持前者

### st_contains转dmgeo.ST_CONTAINS

> 说明
>
> 达梦不支持前者

### st_distance_sphere转dmgeo.ST_Distance

> 说明
>
> 达梦不支持前者

### POINT转dmgeo.ST_GeomFromText(CONCAT('POINT(',arg0,arg1,)'))

> 说明
>
> 达梦不支持前者

### geometry_from_text转dmgeo.ST_GeomFromText

> 说明
>
> 达梦不支持前者

### boolean转0/1

> 说明
>
> 达梦类型校验严格, 并且不支持boolean类型

### 除法分母为0校验

> 说明
>
> 需要在数据库中添加函数

```
CREATE FUNCTION FUNC_OP_MY_DIVIDE( C1 IN NUMBER,
                                   C2 IN NUMBER )
    RETURN NUMBER AS
    BEGIN
        RETURN
        CASE
        WHEN C1 is null then
            null
        when C2 is null or C2 = 0 then
            null
        else
            C1/C2
        END;
    END;
/
```

### bit/int等去除长度

> 说明
>
> DDL

### JSON操作符->替换为JSON_EXTRACT

> 说明

### JSON操作符->>替换为JSON_VALUE

> 说明

### b'0/1'替换为'0/1'

> 说明

### INTERVAL expr unit中expr转字符

> 说明
> 
> expr若是非数字或字符串等特殊表达式, 将会得到错误的形式
