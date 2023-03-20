Docker基础命令
容器操作
# 列出所有容器
docker ps -a 

# 初次启动容器
docker run    <镜像源>
--name <容器自定义名称> 
-p <本地端口>:<容器端口>  
-e <环境变量>   //eg:MYSQL_ROOT_PASSWORD=123456
-v <挂载路径> //本地路径:容器路径:权限  eg:/var/lib/xxx:/var/lib/yyy:ro 只读
-d #后台运行
--restart=<always(自动重启容器)| on-failure:3(容器退出代码非0重启次数)> 
--link <通信容器名>:<通信容器别名>
-it /bin/bash #启动后进入bash


# 查看容器日志
docker logs --tail 0 -f <容器id/容器名>  // 读取最新日志
docker logs -f <容器id/容器名> // 读取全量日志

# docker查看容器配置
docker inspect  <容器id/容器名>  // 返回配置信息，网络信息名称 网络
--format '{{.State.Running}}'   // 格式化具体指标项


# 查看容器进程
docker top <容器id/容器名>

# 进入容器shell
docker exec -it <容器id/容器名> /bin/bash

# 启动/停止/重启
docker start/stop/restart <容器id/容器名>

# 删除容器
docker rm <容器id/容器名>

# 查询已启用容器启动命令
pip install runlike 
runlike -p <容器id/容器名>

# 查看容器端口映射
docker port <容器id> 
镜像操作
# 查询镜像
docker search <镜像名称>

# 拉取镜像
docker pull <镜像名称:版本>

# 列出所有镜像
docker image ls 
docker images 
# 查看某个镜像
docker images <镜像名称>

# 查看构建历史
docker history <镜像id>

# 构建新镜像
在dockerfile目录下
docker build -t="<镜像仓库>/<镜像名称:版本>"
# 删除镜像
docker rmi <镜像id/镜像名称> <镜像id/镜像名称>

# 打标签
docker tag <镜像id/镜像名称>  <标签名>
docker tag IMAGE[:TAG] [REGISTRY_HOST[:REGISTRY_PORT]/]REPOSITORY[:TAG]