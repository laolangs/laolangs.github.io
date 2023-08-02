# Refrence Counted (Rc)

> 允许一个变量有多个所有者,通过引用计数实现. RcBox
> 非线程安全 多线程用Arc
> Rc clone变量不可变 ,内部可变使用 RefCell

## 源码解析
> 通过阅读源码是学习编程的方法之一。最近在向公司介绍Rust，所以为了更好地分享，就读读源码。而引用计数是一个常见常用的功能，并且Rust的Rc还是单线程的，应该不会太复杂。所以本文记录阅读Rc过程的摘要。  
> 如果有错误，欢迎指正！

Rc是用于单线程的引用计数，是Reference Counted的缩写。你如果想用于多线程，Rust会直接甩给你编译错误。如果对Rust的多线程编程感兴趣，请看[多线程编程的秘密：Sync, Send, and 'Static](https://zhuanlan.zhihu.com/p/362285521)

如果想先对Rust有一些认识，可以阅读[Rust那些难理解的点(大量更新于6月16日）](https://zhuanlan.zhihu.com/p/360342782)。

下面让我们来打开[Rc的源码](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/src/alloc/rc.rs.html%23298-301)

![](https://pic4.zhimg.com/v2-ff4448281f8d8e62faa9f377691bbde7_b.jpg)

首先是Rc的数据结构

```
pub struct Rc<T: ?Sized> {
    ptr: NonNull<RcBox<T>>,
    phantom: PhantomData<RcBox<T>>,
}
```

这个数据结构就包含了两个field，但是要注意的东西却不少。phantom是什么，RcBox又是什么， ?Sized是什么。

## ?Sized

先说说这个?Sized，它表示引用计数Rc，支持编译期就可以知道大小的类型， 也支持编译期不知道大小的类型，如slice \[T\]。比如下面的用法

```
use std::rc::Rc;
fn main() {
  let x = Rc::new(5);// 肯定在编译期知道5的大小
  let y:Rc<[i32]>  ;  //在编译期，我们不知道[i32]的大小   
  y = Rc::new([3, 4,5 ]);
}
```

![](https://pic3.zhimg.com/v2-4aab7c07d41ad03055310fdb5d1d1b7a_b.jpg)

## PhantomData

phantom的类型PhantomData<RcBox<T>>。这个是用来干什么的呢？实际上这个比想象中的具有要多信息。Rust很多标准库的类型到用到了这个，如Vec。搜索一下得到下面的说明

[use std::marker::PhantomData;](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/std/marker/struct.PhantomData.html)

```
Zero-sized type used to mark things that “act like” they own a T.

Adding a PhantomData<T> field to your type tells the compiler 
that your type acts as though it stores a value of type T, 
even though it doesn’t really. 
This information is used when computing certain safety properties.

For a more in-depth explanation of how to use PhantomData<T>, 
please see the Nomicon.
```

至此，大致明白了这个field是一个marker field，不占空间，用来做标识的，告诉编译器，Rc拥有这个RcBox<T>，明示编译器我可能在drop函数里面也drop掉RcBox<T>（这不就是当强引用等于零的时候嘛？见下文分析）。更多关于PhantomData请看[Rust: PhantomData，Drop Check 虚虚实实](https://zhuanlan.zhihu.com/p/383004091)

因此，我们也就知道NonNull是不会拥有RcBox<T>，不然为什么要有这个phantom field。那么让我们看看什么是NonNull.

## NonNull

![](https://pic2.zhimg.com/v2-a5f4894867155f06a75acdc15bb2a8f1_b.jpg)

在Rust的官网文档库搜索一下NonNull，得到

```
*mut T but non-zero and covariant.


This is often the correct thing to use when building data structures 
using raw pointers, but is ultimately more dangerous to use 
because of its additional properties. 
If you’re not sure if you should use NonNull<T>, just use *mut T!

Unlike *mut T, the pointer must always be non-null, 
even if the pointer is never dereferenced. 
This is so that enums may use this forbidden value as a discriminant 
– Option<NonNull<T>> has the same size as *mut T. 
However the pointer may still dangle if it isn’t dereferenced.

Unlike *mut T, NonNull<T> was chosen to be covariant over T. 
This makes it possible to use NonNull<T> when building covariant types, 
but introduces the risk of unsoundness if used in a type 
that shouldn’t actually be covariant. 
(The opposite choice was made for *mut T 
even though technically the unsoundness could only be 
caused by calling unsafe functions.)
```

哇~信息好像也挺多的~

首先NonNull就是\*mut T，但是不会等于零，且是协变。

不等于零，很容易理解，就是说不会是空指针的意思。这估计就是NonNull的来源！从中我们也可以知道为什么NonNull<T>为什么不会拥有T了，因为它就是一个指针\*mut T，没有拥有的语义。

而另外一个NonNull神奇的地方是（由

指出）：NonNUll可以做空指针优化，Option<Rc<T>>跟Rc<T>占用相同的大小。这个叫[discriminant elision](https://link.zhihu.com/?target=https%3A//rust-lang.github.io/unsafe-code-guidelines/layout/enums.html%23discriminant-elision-on-option-like-enums)。因为enum通常需要一个标志（discriminant）来区分究竟是哪一个variant。但是Option一样的只有两个variant的enum。其中一个variant，有一些非法的值（叫niches），这些非法的值可以充当None一样的variant。所以就不用标志了，从而enum与variant占用一样的大小。

协变是啥？幸好我事先了解过一点，如果是新手，直接跳过这个。协变可以理解为：有一个子类型SubType和父类型Type，如果NonNull<SubType> 也是NonNull<Type>的子类型，那么我们就说NonNull是协变的。

实际上NonNull的源代码很简单

```
pub struct NonNull<T: ?Sized> {
    pointer: *const T,
}
```

接下来重头戏应该就是RcBox<T>了！

## RcBox<T>

RcBox<T>的数据结构为

```
.#[repr(C)]
struct RcBox<T: ?Sized> {
    strong: Cell<usize>,
    weak: Cell<usize>,
    value: T,
}
```

这里我们就看到了跟引用计数熟悉的数据结构了——strong 用来记录有所有Rc（强引用）的个数，weak用来记录有多少弱引用。value就是实际的数据。其中Cell是支持内部可变性的类型——因为肯定有多个RcBox的共享引用，并且修改计数，这时我们就需要使用内部可变性Cell来实现这个功能。更多请看[内部可变性/Interior Mutability in Rust](https://zhuanlan.zhihu.com/p/380419980)

它们的用途就是：strong为零的时候value就会被析构（调用value的析构函数），weak为零的时候这个RcBox就会被删除。

因此让我们看看Rc的drop（drop等于C++里面的[析构函数](https://zhuanlan.zhihu.com/p/369349887)。），估计它里面肯定就会判断strong的个数是否为零。

```
    fn drop(&mut self) {
        unsafe {
            self.inner().dec_strong();
            if self.inner().strong() == 0 {
                // destroy the contained object
                ptr::drop_in_place(Self::get_mut_unchecked(self));

                // remove the implicit "strong weak" pointer now that we've
                // destroyed the contents.
                self.inner().dec_weak();

                if self.inner().weak() == 0 {
                    Global.dealloc(self.ptr.cast(), Layout::for_value(self.ptr.as_ref()));
                }
            }
        }
    }
```

首先映入眼帘的是unsafe。什么"不安全”，rust不是号称安全的嘛！实际上，Rust的安全是在对特定的问题是安全的（更多内容请看[Rust那些难理解的点(大量更新于6月16日）](https://zhuanlan.zhihu.com/p/360342782)）。这里的unsafe主要是告诉编译器——[花括号里面的代码我很有自信是对的](https://link.zhihu.com/?target=https%3A//manishearth.github.io/blog/2017/12/24/undefined-vs-unsafe-in-rust/)，你（编译器）不用禁止我做一些事情（比如解指针，调用别人写的像我一样自信的代码）。所以用了unsafe以后，你就可以做更有特权的事情。

明白了unsafe，那么我们就看看花括号里面做了哪些事情。

首先对strong计数减一，这是常规操作，毕竟现在有一个Rc被析构了。

接着看看强引用计数是不是零，如果是零，那么就调用value的析构函数，释放这个引用计数管理的数据。这里释放数据，使用了`ptr::drop_in_place` ，而get\_mut\_unchecked，就是获取这个数据

```
    pub unsafe fn get_mut_unchecked(this: &mut Self) -> &mut T {
        // We are careful to *not* create a reference covering the "count" fields, as
        // this would conflict with accesses to the reference counts (e.g. by `Weak`).
        unsafe { &mut (*this.ptr.as_ptr()).value }
    }
```

接着对弱引用计数减一，如果弱引用计数为零，所有的Rc/Weak也不存在了，那么就将这个RcBox给销毁了。

综上所述，Rc的数据结构在内存中可以描绘如下，

-   当Rc都不存在的时候，value:T的析构函数被调用
-   当Weak都不存在的时候，RcBox就会被销毁

![](https://pic3.zhimg.com/v2-397dbb1d906741793e2312c29251edf2_b.jpg)
