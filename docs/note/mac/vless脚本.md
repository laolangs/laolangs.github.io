<iframe allowfullscreen="" height="333" src="https://www.youtube.com/embed/ojGojtF8sW8" width="479" youtube-src-id="ojGojtF8sW8"></iframe>

**备用机场（500G/月，12￥/年）：** [https://xn--4gq62f52gdss.art/#/register?code=t0FR9bX4](https://xn--4gq62f52gdss.art/#/register?code=t0FR9bX4)

**获得CF的CDN IP工具：**

1、[https://github.com/badafans/better-cloudflare-ip](https://github.com/badafans/better-cloudflare-ip)

2、[https://github.com/XIU2/CloudflareSpeedTest](https://github.com/XIU2/CloudflareSpeedTest)

**一、如果获取CF中转IP(即反代IP)**

1、[zip.baipiao.eu.org](http://zip.baipiao.eu.org/)

2、电报机器人：[@cf\_push](https://t.me/cf_push)

3、合并多个txt文档技巧：新建txt文件，输入type \*.txt>>all.txt，重命名格式为.bat后，运行。自动合并生成。

4、文本排重工具：[https://www.toolhelper.cn/Char/TextFilter](https://www.toolhelper.cn/Char/TextFilter)

5、在线批量IP地理位置查找工具：[https://reallyfreegeoip.org/bulk](https://reallyfreegeoip.org/bulk)

6、利用“CloudflareSpeedTest”工具来测速:

7、在该软件当时文件栏上输出：cmd，回车键

8.1、直接软件测：CloudflareST.exe -tll 40 -tl 200 -f <u>文件名.txt</u>（下载速度测不了）

8.2、有测速地址：CloudflareST.exe -url https://down.heleguo.top/download/300MB.zip -tl 200 -sl 5 -tlr 0.2 -f <u>文件名.txt</u>

\*表示启动cloudflare工具，利用https://down.heleguo.top/download/300MB.zip测速地址测速。tll延迟下限是40，tl延迟上限是200，sl最低速度5M，tlr丢包率小于0.2，-f表示针对文件名.txt文件内IP测速。

\*参数设置请看这里：[https://github.com/XIU2/CloudflareSpeedTest#-cmd-%E5%B8%A6%E5%8F%82%E6%95%B0%E8%BF%90%E8%A1%8C-cloudflarest](https://github.com/XIU2/CloudflareSpeedTest#-cmd-%E5%B8%A6%E5%8F%82%E6%95%B0%E8%BF%90%E8%A1%8C-cloudflarest)

\*查找更多测速地址：[https://github.com/XIU2/CloudflareSpeedTest/issues/168](https://github.com/XIU2/CloudflareSpeedTest/issues/168)

**二、如何获取CF的CDN IP**

1、[https://stock.hostmonit.com/CloudFlareYes](https://stock.hostmonit.com/CloudFlareYes)

2、[http://ip.flares.cloud/](http://ip.flares.cloud/)

3、[https://github.com/hello-earth/cloudflare-better-ip/tree/main/cloudflare](https://github.com/hello-earth/cloudflare-better-ip/tree/main/cloudflare)

4、better-cloudflare-ip（本地优选）

5、CloudflareSpeedTest（本地优选）

6、[https://fofa.info/](https://fofa.info/)

\*打开fofa网站搜索：server="cloudflare" && country="US" && city="Chicago" && port="443"

- 国内反代IP：server=="cloudflare" && port=="80" && header="Forbidden" && country=="CN"
- 剔除CF：asn!="13335" && asn!="209242"
- 阿里云：server=="cloudflare" && asn=="45102"
- 甲骨文韩国：server=="cloudflare" && asn=="31898" && country=="KR"
- 搬瓦工：server=="cloudflare" && asn=="25820"

\*意思是筛选CF的CDN IP，国家是美国，地区是芝加哥，端口443

7、利用“CloudflareSpeedTest”工具来测速:
108.170.53.157


省级/出国/国际骨干节点都以202.97开头AS434，全程没有59.43开头的CN2节点。
**三、查当前IP地址及测速**

1、查IP：[https://ip.gs/](https://ip.gs/)

2、查IP纯净度：[https://scamalytics.com/](https://scamalytics.com/)

3、测速度：[https://www.speedtest.net/](https://www.speedtest.net/)

\-------------------------------------------------------

C:\\>CloudflareST.exe -h

CloudflareSpeedTest vX.X.X

测试 Cloudflare CDN 所有 IP 的延迟和速度，获取最快 IP (IPv4+IPv6)！

https://github.com/XIU2/CloudflareSpeedTest

参数：

    -n 200

        延迟测速线程；越多延迟测速越快，性能弱的设备 (如路由器) 请勿太高；(默认 200 最多 1000)

    -t 4

        延迟测速次数；单个 IP 延迟测速的次数；(默认 4 次)

    -dn 10

        下载测速数量；延迟测速并排序后，从最低延迟起下载测速的数量；(默认 10 个)

    -dt 10

        下载测速时间；单个 IP 下载测速最长时间，不能太短；(默认 10 秒)

    -tp 443

        指定测速端口；延迟测速/下载测速时使用的端口；(默认 443 端口)

    -url https://cf.xiu2.xyz/url

        指定测速地址；延迟测速(HTTPing)/下载测速时使用的地址，默认地址不保证可用性，建议自建；

    -httping

        切换测速模式；延迟测速模式改为 HTTP 协议，所用测试地址为 \[-url\] 参数；(默认 TCPing)

        注意：HTTPing 本质上也算一种 网络扫描 行为，因此如果你在服务器上面运行，需要降低并发(-n)，否则可能会被一些严格的商家暂停服务。

        如果你遇到 HTTPing 首次测速可用 IP 数量正常，后续测速越来越少甚至直接为 0，但停一段时间后又恢复了的情况，那么也可能是被 运营商、Cloudflare CDN 认为你在网络扫描而 触发临时限制机制，因此才会过一会儿就恢复了，建议降低并发(-n)减少这种情况的发生。

    -httping-code 200

        有效状态代码；HTTPing 延迟测速时网页返回的有效 HTTP 状态码，仅限一个；(默认 200 301 302)

    -cfcolo HKG,KHH,NRT,LAX,SEA,SJC,FRA,MAD

        匹配指定地区；地区名为当地机场三字码，英文逗号分隔，支持小写，支持 Cloudflare、AWS CloudFront，仅 HTTPing 模式可用；(默认 所有地区)

    -tl 200

        平均延迟上限；只输出低于指定平均延迟的 IP，各上下限条件可搭配使用；(默认 9999 ms)

    -tll 40

        平均延迟下限；只输出高于指定平均延迟的 IP；(默认 0 ms)

    -tlr 0.2

        丢包几率上限；只输出低于/等于指定丢包率的 IP，范围 0.00~1.00，0 过滤掉任何丢包的 IP；(默认 1.00)

    -sl 5

        下载速度下限；只输出高于指定下载速度的 IP，凑够指定数量 \[-dn\] 才会停止测速；(默认 0.00 MB/s)

    -p 10

        显示结果数量；测速后直接显示指定数量的结果，为 0 时不显示结果直接退出；(默认 10 个)

    -f ip.txt

        IP段数据文件；如路径含有空格请加上引号；支持其他 CDN IP段；(默认 ip.txt)

    -ip 1.1.1.1,2.2.2.2/24,2606:4700::/32

        指定IP段数据；直接通过参数指定要测速的 IP 段数据，英文逗号分隔；(默认 空)

    -o result.csv

        写入结果文件；如路径含有空格请加上引号；值为空时不写入文件 \[-o ""\]；(默认 result.csv)

    -dd

        禁用下载测速；禁用后测速结果会按延迟排序 (默认按下载速度排序)；(默认 启用)

    -allip

        测速全部的IP；对 IP 段中的每个 IP (仅支持 IPv4) 进行测速；(默认 每个 /24 段随机测速一个 IP)

    -v

        打印程序版本 + 检查版本更新

    -h

        打印帮助说明

\-----------------------------------------

[vless脚本地址](https://github.com/yonggekkk/Cloudflare-workers-pages-vless)

支持tls 443 2053 2083 2087 2096 8443

非tls 80 2052 2082 2086 2095 8080 8880
