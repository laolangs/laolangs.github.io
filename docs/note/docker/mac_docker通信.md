# 解决mac宿主机和容器ip不通问题

安装docker-connector `brew install  wenjunxiao/brew/docker-connector`

将现有docker容器网络写入配置文件
`
docker network ls --filter driver=bridge --format "{{.ID}}" | xargs docker network inspect --format "route {{range .IPAM.Config}}{{.Subnet}}{{end}}" >> /opt/homebrew/etc/docker-connector.conf`

docker-connector配置文件地址：/opt/homebrew/etc/docker-connector.conf

启动docker-connector
  brew services restart wenjunxiao/brew/docker-connector

`sudo docker-connector -config /opt/homebrew/etc/docker-connector.conf`

启动docker容器

`docker run -it -d --restart always --net host --cap-add NET_ADMIN --name connector wenjunxiao/mac-docker-connector`

[github地址](https://github.com/wenjunxiao/mac-docker-connector)



>
>For the first time, you can add all the bridge networks of docker to the routing table by the following command:
  docker network ls --filter driver=bridge --format "{{.ID}}" | xargs docker network inspect --format "route {{range .IPAM.Config}}{{.Subnet}}{{end}}" >> /opt/homebrew/etc/docker-connector.conf
Or add the route of network you want to access to following config file at any time:
  /opt/homebrew/etc/docker-connector.conf
Route format is `route subnet`, such as:
  route 172.17.0.0/16
The route modification will take effect immediately without restarting the service.
You can also expose you docker container to other by follow settings in /opt/homebrew/etc/docker-connector.conf:
  expose 0.0.0.0:2512
  route 172.17.0.0/16 expose
Let the two subnets access each other through iptables:
  iptables 172.17.0.0+172.18.0.0

>To restart wenjunxiao/brew/docker-connector after an upgrade:
  brew services restart wenjunxiao/brew/docker-connector
Or, if you don't want/need a background service you can just run:
  sudo /opt/homebrew/opt/docker-connector/bin/docker-connector -config etc/docker-connector.conf
  sudo /opt/homebrew/opt/docker-connector/bin/docker-connector -config /opt/homebrew/etc/docker-connector.conf
==> Summary
>