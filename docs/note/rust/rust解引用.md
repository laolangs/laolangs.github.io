这一章的主要内容是想对rust中的解引用操作本身，以及各种存在的自动解引用机制进行一个比较详细的记载。需要参考的资料有：

[std::ops::Deref Trait文档](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/std/ops/trait.Deref.html)

[关于在函数调用时自动解引用的回答](https://link.zhihu.com/?target=https%3A//stackoverflow.com/questions/28519997/what-are-rusts-exact-auto-dereferencing-rules)

[rust book中的章节](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/book/ch15-02-deref.html)

[dereference operator](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/reference/expressions/operator-expr.html%23the-dereference-operator)

[method call expressions](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/reference/expressions/method-call-expr.html)

[type coercions](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/reference/type-coercions.html)

### 1\. 从操作符说起

```
Syntax
DereferenceExpression :
   * Expression
```

 rust中预置了一种解引用操作符`*`，将其与一个类型为“**指针类型**”的表达式结合，就代表指针类型指向的**内存位置**，这一点和C语言很相似。

 rust的文档指出，如果被解引用的表达式具有类型`&mut T`（可变引用），`*mut T`（可变裸指针），并且是局部变量（**注意，这里排除了全局变量，也就是rust里的static变量，它们的可变性要单独讨论//TODO**)，结构体的field，或者mutable [place expression](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/reference/expressions.html%23place-expressions-and-value-expressions)，那么返回的**内存位置**就可以被赋值。

 特别地，一个可变引用`&mut`真正的含义应该在于：你可以隔着这个引用**修改一个绑定**，那么这暗含着，这个引用指向的绑定，一定是一个可变绑定

 仔细考虑一下上面的话，就可以察觉到，一个可变引用能不能通过解引用赋值，关键不在于绑定(bind)这个引用的变量(variable)是不是可变绑定，而是这个变量绑定到的引用是不是可变引用，比如你想修改一个变量`nyaruko_age`的值：

```
let mut nyaruko_age:32 = u32::MAX;
```

 你只需要创建一个可变引用，但是绑定这个引用的变量不需要是可变的！

```
//不需要let mut nyaruko_mut_ref:&mut u32 = &mut nyaruko;
let nyaruko_mut_ref:&mut u32 = &mut nyaruko;
*nyaruko_mut_ref = 16;
```

 此外，注意使用星号`*`操作符解引用时，实际执行的动作有两种情况：

-   1.直接解引用：被解引用的表达式具有引用类型，那么就直接去掉一层indirection
-   2.调用`.deref()`（来自Deref Trait）：被解引用的表达式不具有引用类型

### 2\. 什么是指针类型

解引用操作符的相关文档中指出了解引用可以被应用在指针类型上，那么指针类型是什么？

### 2.1. 引用

```
Syntax
ReferenceType :
   & Lifetime? mut? TypeNoBounds
```

-   **共享引用（不可变引用, shared reference）**：由`&type`或者`&'a type`创建，rust的所有权机制允许一个变量具有多个共享引用。特别注意，类型`T`和他的引用`&T`不是同一种类型！  
    
-   **可变引用（mutable reference）**：由`&mut type`或者`&mut'a type`创建，rust只允许一个变量有一个可变引用。可变引用可以被转换(coerce)成不可变引用。同样注意，类型`T`和`&mut T`不是同一种类型！注意一个问题是，**可变引用只能由一个`mut`的变量创建出来**。  
    

 结合“指针类型”的命名，以及上面指出的类型`T`和他的引用`&T`不是同一种类型的问题，额外注意一下：尽管名字相同，rust中的引用和C++中的引用完全不是同一种东西，rust的引用更像是C/C++中的指针，但区别在于rust的所有权机制强迫每一个引用一定是合法的，对应到C/C++中就是说指针不能是空指针。不过，rust的core文档也特别说明，一个`Option<&T>`具有和C语言指针完全相同的内存布局，因此可以在FFI中使用。下面是一个内存布局示意图：

![](https://pic1.zhimg.com/v2-9e78651639512a6b71dd0258b0f3f688_b.jpg)

-   除了上面的内容之外，再额外记录一下rust中创建引用的语法：

```
let nyaruko:u32 = 888;
let nyaruko_ref:&u32 = &nyaruko;

let mut nyaruko_mut:u32 = 77;
let nyaruko_mut_ref:&mut u32 = &mut nyaruko_mut;
//怎么通过操作nyaruko_mut_ref来改变nyaruko_mut的值？
//答案：
*nyaruko_mut_ref = 4;

//错误的情况：
let mut nyaruko_mut_ref:&mut u32 = &mut nyaruko_mut;
nyaruko_mut_ref = &mut 4;
//这样实际上相当于使得nyaruko_mut_ref引用了一个4的常量
```

这里的问题和**可变性(mutability)**的问题有关！ //TODO [mutability](https://link.zhihu.com/?target=https%3A//web.mit.edu/rust-lang_v1.25/arch/amd64_ubuntu1404/share/doc/rust/html/book/first-edition/mutability.html)

-   关于指针类型还有一个很好的文档（图片就取自那里）：[Effective Rust](https://link.zhihu.com/?target=https%3A//www.lurklurk.org/effective-rust/references.html)

### 2.2. 裸指针

（[参见std文档pointer章节](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/std/primitive.pointer.html%23method.offset)）

```
Syntax
RawPointerType :
   * ( mut | const ) TypeNoBounds
```

 裸指针是没有safety和liveness保证的指针，其基本相当于C/C++中的指针。rust中可以随意地创建裸指针，但只能在unsafe块中对它们做解引用。上面的语法中，`*const type`表示指向不可变的，类型为`type`的指针，`*mut type`表示指向可变的，类型为`type`的指针。

 这里值得额外注意的内容是，`* mut|const type`是一个**类型**，但没法向它添加mut修饰符，并且一般的运算符都没有针对这种类型的重载，如：

```
let mut_pointer:*mut u32 = 0x100 as *mut u32;
let mut_pointer_offset_4 = mut_pointer+4; //不合法
```

 也就是说rust中不能很“方便”地做pointer arithmetic. 不过rust提供了`.offset`方法，可以[参见std文档pointer章节](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/std/primitive.pointer.html%23method.offset). 另外，std文档中还**特别提到**，尽管获取某个东西的指针不会获取它的所有权（简单来说，对原始的数据没有影响），但**解引用并向指针位置写入值时，rust会自动调用指针原来指向的对象的drop方法**！

 另外，简单记录一下rust中创建裸指针的方法：（同样来自std的pointer章节）

-   从引用转换（coerce）而来：

```
let my_num: i32 = 10;
let my_num_ptr: *const i32 = &my_num;
let mut my_speed: i32 = 88;
let my_speed_ptr: *mut i32 = &mut my_speed;
let mut nyaruko: i32 = 11;
let nyaruko_ptr = &mut nyaruko as *mut i32;
```

-   消耗(consume)一个Box

Box的into\_raw方法吃掉自己的所有权，返回一个裸指针

```
let my_speed: Box<i32> = Box::new(88);
let my_speed: *mut i32 = Box::into_raw(my_speed);

// By taking ownership of the original `Box<T>` though
// we are obligated to put it together later to be destroyed.
unsafe {
    drop(Box::from_raw(my_speed));
}
```

-   使用`ptr::addr_of!`宏

`ptr::addr_of`和`ptr::addr_of_mut`都可以直接得到某个对象的裸指针。std文档中给了一个例子：

```
#[derive(Debug, Default, Copy, Clone)]
#[repr(C, packed)]
struct S {
    aligned: u8,
    unaligned: u32,
}
let s = S::default();
let p = std::ptr::addr_of!(s.unaligned); // not allowed with coercion
```

文档中记录，给出这个例子的主要原因是，`addr_of!`宏可以得到任意一个对象的地址，但是转换(coerce)是不行的，这里的原因是结构体`S`被标记为`packed`，因此`unaligned` field是非对齐的，rust中不允许对非对齐field取引用。

-   从libc库获取

```
extern crate libc;

use std::mem;

unsafe {
    let my_num: *mut i32 = libc::malloc(mem::size_of::<i32>()) as *mut i32;
    if my_num.is_null() {
        panic!("failed to allocate memory");
    }
    libc::free(my_num as *mut libc::c_void);
}
```

### 3\. Deref Trait

除了上文中介绍的原生的解引用`*`操作符可以对引用以及裸指针（即所谓指针类型）的表达式解引用之外，rust还允许我们通过`Deref`Trait完成“重载\*操作符”的功能：

```
pub trait Deref {
    type Target: ?Sized;
    fn deref(&self)->&Self::Target; //需要impl deref,返回一个类型为Target的引用
}
```

对于一个实现了Deref Trait的类型为`T`的表达式`x`来说，如果`Target=U`，那么：

-   `*x`等价于`*(x.deref())`：你从一个`T`得到一个`U` （x不是引用或者裸指针）  
    
-   允许`&T`类型，或者`&mut T`的表达式被强转为`&U`类型  
    
-   因为`&T`可以被转换(coerce)到`&U`，`T`类型会自动实现所有`U`类型的不可变方法  
    

rust中的Deref Trait只可以在不可变引用的情形下生效，就如同上面所说，它只能将`&T`，`&mut T`变成`&U`. 相对地，rust中还为我们准备了在可变引用的场景下使用的DerefMut Trait：

```
pub trait DerefMut: Deref {
    fn deref_mut(&mut self) -> &mut Self::Target;
}
```

同理，对于一个实现了Deref Trait的类型为`T`的表达式`x`来说，如果`Target=U`，那么：

-   在“mutable context”下，`*x`等价于`*(x.deref_mut())`：你从一个`T`得到一个`U` （x不是引用或者裸指针）
-   允许`&mut T`类型的表达式被强转为`&mut U`类型
-   因为`&mut T`可以被转换(coerce)到`&mut U`，`T`类型会自动实现所有`U`类型的可变方法

总结来看，rust中的Deref和DerefMut Trait所带来的解引用机制是非常独特且实用的：

（1）它首先提供给程序员了一种重载`*`操作符的方式，使得智能指针类型，如`Box<T>`可以被直观地实现

（2）更重要的，它另外通过允许实现了trait的**类型的引用**之间的转换，使得不同的类型关联起来，这一个特性极大地增强了rust中泛型系统的威力。

### 4.rust中方法解析时发生的自动借用和自动解引用机制

在差不多记录了rust中的解引用操作符后，这里还想额外记录一个很有趣的话题，也就是rust中存在的一种自动借用和自动解引用机制，官方文档参考[此处](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/reference/expressions/method-call-expr.html)

对这个机制的记录源自于rust中的一种关于引用的现象。我们知道，rust中引用既像指针，又不是那么的像指针：

-   一方面，我们已经在上面介绍过，rust中具有引用类型的变量的内存布局和C语言中的指针几乎是一样的
-   而另一方面，rust中将“创建一个变量的引用”这种动作称呼为“借用这个变量”，同时我们的确可以隔着若干层变量的引用对一个变量进行操作，比如：

```
struct S {
    ok: u32
}
impl S{
    fn are_you_ok(&self) -> (){
        if self.ok!=0 {
            println!("yes!");
        }
    }
}

fn main(){
    let struct_s =
    S{
        ok:1
    };
    let ref1 = &struct_s; //ref1 : &S
    let ref2 = &ref1; //ref2 : & &S
    let ref3 = &ref2; //ref2 : & & &S
    let ref4 = &ref3; //ref4 : & & & &S 
    ref4.are_you_ok(); //say "yes!"
}
```

好吧...这是怎么回事？显然`ref4`应该具有类型`&&&&S`，如果拿C语言的概念类比，这里`ref4`应该是一个四级指针，想在C语言中隔着一个四级指针操作对象，哈哈，那你有的忙活了。而在rust中我们却好像就把这个四级引用当作了原始的对象直接使用了。但问题在于：第一点，我们的`are_you_ok`明明白白地写着它**是给S实现的，不是&&&&S**，而且我们之前很清楚地介绍过，rust中`&T`和`T`不是一种类型；第二点，我们的`are_you_ok`接收的参数**明明是一个&self**，它怎么就能被应用到`&&&&S`了？

事实上，这是rust的方法解析时发生的自动借用/自动解引用机制在帮忙。rust中，将一个方法调用(method call)的点号左侧的值称为"方法的receiver"，而rust规定，在进行方法调用解析时，可以对receiver做以下的操作，来寻找合法的方法调用：

假设receiver具有类型`T`，重复执行以下操作直到`T`不再改变：

（1）使`U=T`

（2）将`U`，`&U`，`&mut U`加入解析列表

（3）对`U`解引用，使`T=*U`

上述循环结束后，执行一次[unsized coercion](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/reference/type-coercions.html%23unsized-coercions)，并使得`T`等于unsized coercion的得到的结果类型再次执行一次（2）和（3），最终得到一个完整的解析列表；最后，按顺序尝试将解析列表中的类型匹配到方法上，且最终的解析结果不能有冲突。

上面的内容有点抽象，用刚才的例子，当我做`ref4.are_you_ok()`，构造的解析列表是：

`&&&&S`，`&&&&&S`，`&mut &&&&S`，`&&&S`，`&&&&S`，`&mut &&&S`.......，`&S`，`&&S`，`&mut &S`，`S`，`&S`，`&mut S`

规律应该已经很明显了，简单来说就是，每一步先分别试着加引用，以及加可变引用；如果不行，就对原来的类型解引用，反复尝试，直到解析成功。官方文档给出的例子是：

> For instance, if the receiver has type `Box<[i32;2]>`, then the candidate types will be `Box<[i32;2]>`, `&Box<[i32;2]>`, `&mut Box<[i32;2]>`, `[i32; 2]` (by dereferencing), `&[i32; 2]`, `&mut [i32; 2]`, `[i32]` (by unsized coercion), `&[i32]`, and finally `&mut [i32]`.

也很清晰。

### Coercions?

上文中说了这么多coercion，难免疑惑到底什么是coercion？事实上我并不太确定这个词究竟该怎么翻译，因此就用rust文档中给出的定义代替：

-   coercion是需要**隐式地**改变值的类型时进行的操作
-   coercion在程序的特定位置（这些位置就称[corercion sites](https://link.zhihu.com/?target=https%3A//doc.rust-lang.org/reference/type-coercions.html)）**自动地**发生，并在值的类型上有严格的约束
-   任何可以发生的coercion都可以通过`as`操作符显式执行