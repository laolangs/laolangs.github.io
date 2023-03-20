## python基础
### 1.函数相关
```
__name__ == 'main'
#当前类中执行为main,其他类调用时，显示其他类名称

```
---
### 2.集合类型
1. list (多可多类型数据)
```
a =[1,"cwl",3,True,NONE]
```
2. tuple (不可变)
```
  空列表 ()
  单个元素列表 a=(1,) 逗号隔开消除歧义]
```
3. dict (hash)
```
    {"name":"cwl","age":30}
```
4. set (自动去重)
```
set([1,2,3])
```
---
### 3.类初始化

生成全量依赖：pip freeze > requirements.txt

单个项目生成依赖：pipreqs ./ --encoding=utf8

安装依赖
pip install -r requirements.txt

conda install --yes --file requirements.txt



from(bucket: "mini_seed")
  |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
  |> filter(fn: (r) => r["_measurement"] == "sensor_data")
  |> filter(fn: (r) => r["_field"] == "channel" or r["_field"] == "network" or r["_field"] == "new_channel" or r["_field"] == "time_stamp" or r["_field"] == "v")
  |> filter(fn:(r) => r.channel=~ /${channels}/)
  |> filter(fn:(r) => r.station=~ /${station}/)

from(bucket: "mini_seed")
|> range(start: v.timeRangeStart, stop: v.timeRangeStop)
|> keyValues(keyColumns:["channel"])
|> group()
|> keep(columns:["channel"])
|> distinct(column:"channel")

from(bucket: "mini_seed")
|> range(start: v.timeRangeStart, stop: v.timeRangeStop)
|> filter(fn: (r) => r["_measurement"] == "sensor_data")
|> filter(fn: (r) => r["channel"] =~ /^${channels}$/)
|> keyValues(keyColumns:["station"])
|> group()
|> keep(columns:["station"])
|> distinct(column:"station")

