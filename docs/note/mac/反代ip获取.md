获取asn:

```shell
curl -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.99 Safari/537.36" -L https://bgp.he.net/search\?search%5Bsearch%5D\=CLOUDFLARE\&commit\=Search | grep -o '<td><a href="[^"]*">AS[0-9]*</a></td>' | sed -E 's/.*>(AS[0-9]*)<\/a>.*/\1/' > as.txt
```


```shell
curl -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.99 Safari/537.36"  https://bgp.he.net/AS395747#_prefixes | grep -o '<a href="[^"]*">[0-9./]*</a>' | sed -E 's/.*>([0-9./]*)<\/a>.*/\1/' > at.txt
```

查asn 
echo "cloudflare" | metabigor netd --org -o /tmp/result.txt
查ip地址
echo '172.67.90.213' | metabigor ipc --json