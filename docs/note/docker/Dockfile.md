全量命令参考地址：
https://docs.docker.com/engine/reference/builder/

DockFile基础语法
RUN 容器创建时执行的指令
eg: RUN apt-get update

CMD 容器被启用时执行的命令(只能有一个，配置多个只有最后一个生效)
eg: CMD ["/bin/bash"]
docker run <镜像>  <shell命令> 命令会覆盖CMD指令  
eg:docker run <镜像相关> /bin/ps
ENTRYPOINT 容器启动后执行命令，不容易被docker run 命令覆盖
docker run 可传参数给ENTRYPOINT
数组形式默认以 /bin/sh -c 执行

WORKDIR 容器工作目录

ENV  设置环境变量

USER 镜像运行用户信息 不写默认root

VOLUMN 容器添加卷,用户宿主机和容器共享

ADD 复制构建环境下文件到容器，源文件是压缩文件会自动解压添加到容器路径下

COPY 复制构建环境下文件到容器, 不解压缩文件

ONBUILD 为镜像添加触发器，紧跟在FROM之后执行，可以执行任何命令
