## 环境优化
### 添加代理
```
conda config --set proxy_servers.https https://127.0.0.1:8118
conda config --set proxy_servers.http http://127.0.0.1:8118
```

### 移除配置
```
conda config --remove-key proxy_servers
```

## Aconda常用
### 创建一个名称为learn的虚拟环境并指定python版本为3(这里conda会自动找3中最新的版本下载)
```
conda create -n learn python=3
```

### 切换环境
```
conda activate learn
```

### 列出所有环境
```
conda env list
```
```
conda list // 列出当前环境的所有包
conda install requests 安装requests包
conda remove requests 卸载requets包
conda remove -n learn --all // 删除learn环境及下属所有包
conda update requests 更新requests包
conda env export > environment.yaml  // 导出当前环境的包信息
conda env create -f environment.yaml  // 用配置文件创建新的虚拟环境
conda install --yes --file requirements.txt  // conda安装依赖ß
```