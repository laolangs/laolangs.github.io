## tokio用法

## tokio::select!

> 当有一个异步表达式执行完成就返回,其他异步表达式将被丢弃,分支最多限制64个
>
> select宏的分支都是在一个任务中执行,不会同时执行,  tokio::spawn!任务会创建一个新的task进行调度,可以同时运行.

### 语法

```rust
<pattern> = <async expression> => <handler>,
```

### 返回值

``` rust
async fn computation1() -> String {
    // .. computation
}

async fn computation2() -> String {
    // .. computation
}

#[tokio::main]
async fn main() {
    let out = tokio::select! {
        res1 = computation1() => res1,
        res2 = computation2() => res2,
    };

    println!("Got = {}", out);
}
```

需要返回值时,每个分支都需要返回同样的返回类型.

### 错误处理

``` rust
use tokio::net::TcpListener;
use tokio::sync::oneshot;
use std::io;

#[tokio::main]
async fn main() -> io::Result<()> {
    // [setup `rx` oneshot channel]

    let listener = TcpListener::bind("localhost:3465").await?;

    tokio::select! {
        res = async {
            loop {
                let (socket, _) = listener.accept().await?;
                tokio::spawn(async move { process(socket) });
            }

            // Help the rust type inferencer out
            Ok::<_, io::Error>(())
        } => {
            res?;
        }
        _ = rx => {
            println!("terminating accept loop");
        }
    }

    Ok(())
}
```

当?用于`<async expression>`,错误传导至`<pattern>`,
当?用于`<handler>`,错误传导至 select!线程.

在引用上调用.await时,引用必须固定(pinned)或者实现Unpin.
.await a reference, the value being referenced must be pinned or implement Unpin.

```rust
async fn action() {
    // Some asynchronous logic
}

#[tokio::main]
async fn main() {
    let (mut tx, mut rx) = tokio::sync::mpsc::channel(128);    
    
    let operation = action();
    tokio::pin!(operation);
    
    loop {
        tokio::select! {
            _ = &mut operation => break,
            Some(v) = rx.recv() => {
                if v % 2 == 0 {
                    break;
                }
            }
        }
    }
}
```
