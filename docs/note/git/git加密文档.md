## 安装 git-crypt

1. 使用 mac 可以直接使用 homebrew 安装
2. 运行命令

```bash
brew install git-crypt
brew install gpg
```
## 生成密钥

### 在使用 git-crypt 之前我们还需要使用 gpg 生成密钥

```bash
brew install gpg #安装gpg
gpg --gen-key # 生成密钥（公钥和私钥），按照流程提示进行
gpg --list-keys # 会列出当前所有的密钥
```

## 配置 git 项目

1. 进入到你的 git 项目中  
`cd yourRepo`
2. 生成对称主密钥并将其提交到自动创建的 .git-crypt 文件夹  
`git-crypt init`
3. 添加一个主密钥副本，该副本已使用您的公共 GPG 密钥加密 (只有这样才能解密)  
`git-crypt add-gpg-user --trusted your.email@domain.com`
4. 确保你要加密的文件在项目中并且未被忽略
5. 准备配置文件，在项目根目录创建 .gitattributes 文件
6. 以下为配置示例：  

> 需要被加密的文件，配置方式和 .gitignore 类似
```
config/*.yml filter=git-crypt diff=git-crypt
*.config filter=git-crypt diff=git-crypt
```
> Making sure that .gitattributes is never encrypted. DON'T TOUCH THAT LINE AND ONE BELOW
> .gitattributes !filter !diff

7. 将修改的文件上传到暂存区
git add .
8. 查看要加密到文件
git-crypt status -e
如果在未成功加密之前就进行了提交，需要运行 git-crypt status -f
9. 提交并 push 远端

```bash
git commit
git push
```

## 更换机器

如果你碰巧需要更换电脑而又不添加新的 user，则可以导出密钥，然后将其导入新计算机。 以下是导出密钥的方法：
```bash
gpg --export *your key-ID* > path/to/public/key/backup/file
gpg --export-secret-keys *your key-ID* > path/to/secret/key/backup/file
```
之后导入到新计算机中
```bash
gpg --import path/to/public/key/backup/file
gpg --import path/to/secret/key/backup/file
```