# rust基础语法

---

## 泛型

```rust
#![allow(unused)]

fn main() {
// 卧龙
enum Option<T> {
    Some(T),
    None,
}
// 雏凤
enum Result<T, E> {
    Ok(T),
    Err(E),
}
}
```

方法中使用泛型

```rust
struct Point<T> {
    x: T,
    y: T,
}

impl<T> Point<T> {
    fn x(&self) -> &T {
        &self.x
    }
}

fn main() {
    let p = Point { x: 5, y: 10 };

    println!("p.x = {}", p.x());
}
```

特征对象的限制

不是所有特征都能拥有特征对象，只有对象安全的特征才行。当一个特征的所有方法都有如下属性时，它的对象才是安全的：

>方法的返回类型不能是 Self
>
>方法没有任何泛型参数

## 返回值Result和?

```rust
// 处理返回结果成功返回Ok(T) ,出错返回Err(E) ,返回Result必须要处理
struct Result<T,E>{
    Ok(T), 
    Err(E)
}
```

?宏

``` rust
#![allow(unused)]
fn main() {
use std::fs::File;
use std::io;
use std::io::Read;

fn read_username_from_file() -> Result<String, io::Error> {
    let mut f:Result<File,io:Error>= File::open("hello.txt")?;
    let mut s = String::new();
    f.read_to_string(&mut s)?;
    Ok(s)
}
}

// ?类似
let mut f = match f {
    // 打开文件成功，将file句柄赋值给f
    Ok(file) => file,
    // 打开文件失败，将错误返回(向上传播)
    Err(e) => return Err(e),
};
```

?可以自动进行类型提升（转换）

```rust
// 将std::io::Error转换为std::error:Error 
#![allow(unused)]
fn main() {
fn open_file() -> Result<File, Box<dyn std::error::Error>> {
    let mut f = File::open("hello.txt")?;
    Ok(f)
}
}
```

原因是在于标准库中定义的 From 特征，该特征有一个方法 from，用于把一个类型转成另外一个类型，? 可以自动调用该方法，然后进行隐式类型转换。因此只要函数返回的错误 ReturnError 实现了 From<OtherError> 特征，那么 ? 就会自动把 OtherError 转换为 ReturnError。

```rust
// std::io:Error源码
#[stable(feature = "rust1", since = "1.0.0")]
impl From<alloc::ffi::NulError> for Error {
    /// Converts a [`alloc::ffi::NulError`] into a [`Error`].
    fn from(_: alloc::ffi::NulError) -> Error {
        const_io_error!(ErrorKind::InvalidInput, "data provided contains a nul byte")
    }
}
```

## ? 用于 Option 的返回

```rust
// 通过 ? 返回 None
#![allow(unused)]
fn main() {
fn first(arr: &[i32]) -> Option<&i32> {
   let v:&i32 = arr.get(0)?;
   Some(v)
}
}
```

## 闭包定义

```
|param1, param2,...| {
    语句1;
    语句2;
    返回表达式
}
```

三种 Fn 特征

闭包捕获变量有三种途径，恰好对应函数参数的三种传入方式：转移所有权、可变借用、不可变借用，因此相应的 Fn 特征也有三种：

1. FnOnce，该类型的闭包会拿走被捕获变量的所有权。
    仅实现 FnOnce 特征的闭包在调用时会转移所有权

2. FnMut，它以可变借用的方式**捕获了环境中的值**，因此可以修改该值：
3. Fn 特征，它以不可变借用的方式捕获环境中的值

**一个闭包实现了哪种 Fn 特征取决于该闭包如何使用被捕获的变量，而不是取决于闭包如何捕获它们。**

三种 Fn 的关系

实际上，一个闭包并不仅仅实现某一种 Fn 特征，规则如下：

- 所有的闭包都自动实现了 FnOnce 特征，因此任何一个闭包都至少可以被调用一次
- 没有移出所捕获变量的所有权的闭包自动实现了 FnMut 特征
- 不需要对捕获变量进行改变的闭包自动实现了 Fn 特征

    如果你想强制闭包取得捕获变量的所有权，可以在参数列表前添加 move 关键字，这种用法通常用于闭包的生命周期大于捕获变量的生命周期时

```rust
use std::thread;
let v = vec![1, 2, 3];
let handle = thread::spawn(move || {
    println!("Here's a vector: {:?}", v);
});
handle.join().unwrap();
```

结构体中闭包定义

```rust
struct Cacher<T>
where
    T: Fn(u32) -> u32,  // Fn(u32) ->u32  闭包定义
{
    query: T,
    value: Option<u32>,
}
```

## 元组结构体

定义元组结构体，以 struct 关键字和结构体名开头并后跟元组中的类型。

```rust
struct Color(i32, i32, i32);
struct Point(i32, i32, i32);
```

类单元结构体（没有任何字段的结构体）

```rust
struct Test;
```

***结构体拥有字段的所有权***

```rust
#[derive(Debug)]
struct A{
    a:B,
}
struct B{
    b:i32
}
fn main(){
    let b = B{b:1};
    let a = A{a:b};
    // println!("{:?}",b) 报错，b变量所有权已移动到a中
}
```
