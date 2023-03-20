
## vpn连接
```bash
vi /etc/ppp/options
```
```
// 添加配置
plugin L2TP.ppp
l2tpnoipsec
```
```bash
sudo sysctl net.link.generic.system.hwcksum_tx=0
sudo sysctl net.link.generic.system.hwcksum_rx=0
```