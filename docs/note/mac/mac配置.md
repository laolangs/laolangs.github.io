- [vpn连接](#vpn连接)
- [软件安装问题](#软件安装问题)


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

## 软件安装问题

Mac安装软件的“已损坏，无法打开。 您应该将它移到废纸篓”问题
**一、允许“任何来源”开启**

苹果从macOS Sierra 10.12 开始，已经**去除了允许“任何来源”的选项**，如果不开启“任何来源”的选项，会直接影响到无法运行的第三方应用。

所以开启“任何来源”的方法如下：

打开【启动台】，选择【终端】，输入：

```bash
sudo spctl  --master-disable
```

然后回车，继续输入密码（密码输入时是不可见的），然后回车。

接着打开【系统偏好设置】，选择【安全性与隐私】，选择【通用】，可以看到【任何来源】已经选定。

![](https://pic1.zhimg.com/v2-5b81946b01c8285aab0296f6ac040718_b.jpg)

接着打开文件进行安装。

**二、发现还是显示“已损坏，无法打开。 您应该将它移到废纸篓”，不急，接下来用这种方法：**

在终端粘贴复制输入命令（注意最后有一个空格）：

```bash
sudo xattr -r -d com.apple.quarantine 
```

**先不要按回车！先不要按回车！先不要按回车！先不要按回车！**

然后打开 **“访达”（Finder）**进入 **“应用程序”** 目录，找到该软件图标，将图标拖到刚才的终端窗口里面，会得到如下组合(如图所示)：

```bash
sudo xattr -r -d com.apple.quarantine /Applications/WebStrom.app
```

回到终端窗口按回车，输入系统密码回车即可。

![](https://pic2.zhimg.com/v2-0b0558d34b0fe1c615be753302cf5d75_b.jpg)

接着重新打开安装软件，就可以正常安装了。

**_注：如果试了还是不行，那就只能下载以前的版本了。_**