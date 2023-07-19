## 使用panic!宏处理错误
>
> ### 对应 panic 时的栈展开或终止
>
>当出现 panic 时，程序默认会开始 展开（unwinding），这意味着 Rust 会回溯栈>并清理它遇到的每一个函数的数据，不过这个回溯并清理的过程有很多工作。另一种选择>是直接 终止（abort），这会不清理数据就退出程序。
>
>那么程序所使用的内存需要由操作系统来清理。如果你需要项目的最终二进制文件越小越>好，panic 时通过在 Cargo.toml 的 [profile] 部分增加 panic = 'abort'，>可以由展开切换为终止。例如，如果你想要在 release 模式中 panic 时直接终止：
>
> ```toml
> [profile.release]
> panic = 'abort'
> ```

### 展开panic调用堆栈信息

`RUST_BACKTRACE=1 cargo run`

## 传播错误的简写：? 运算符

> 向上层调用抛出异常

```rust
#![allow(unused)]
fn main() {
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username_file = File::open("hello.txt")?;
    let mut username = String::new();
    username_file.read_to_string(&mut username)?;
    Ok(username)
}
}
```
? 运算符所使用的错误值被传递给了 `from` 函数，它定义于标准库的 `From trait` 中，其用来将错误从一种类型转换为另一种类型。当 ? 运算符调用 from 函数时，收到的错误类型被转换为由当前函数返回类型所指定的错误类型。这在当函数返回单个错误类型来代表所有可能失败的方式时很有用，即使其可能会因很多种原因失败。

`?`运算符只能被用于返回值与`?`作用的值相兼容的函数

>the `?` operator can only be used in a function that returns `Result` or `Option` (or another type that implements `FromResidual`)
