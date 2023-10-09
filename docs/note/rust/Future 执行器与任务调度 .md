### Future 定义

Future 是 Rust 异步编程的核心， [Future](https://link.juejin.cn/?target=https%3A%2F%2Fdoc.rust-lang.org%2Fstd%2Ffuture%2Ftrait.Future.html "https://doc.rust-lang.org/std/future/trait.Future.html") trait 的定义：

```rust
#[must_use = "futures do nothing unless you `.await` or poll them"] #[lang = "future_trait"]
pub trait Future {
    type Output;
    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output>;
}

#[must_use = "this `Poll` may be a `Pending` variant, which should be handled"]
pub enum Poll<T> {
    Ready(T),
    Pending,
}
```

Future 有一个关联类型 Output；还有一个 `poll()` 方法，它返回 `Poll<Self::Output>`。Poll 是个枚举，有 `Ready` 和 `Pending` 两个状态。通过调用 `poll()` 方法可以推进 Future 的进一步执行，直到任务完成被切走为止。

> 在当前 poll 中，若 Future 完成了，则返回 `Poll::Ready(result)`，即得到 Future 的值并返回；若Future 还没完成，则返回 `Poll::Pending()`，此时 Future 会被挂起，需要等某个事件将其唤醒（wake唤醒函数）

### 执行调度器 executor

executor 是一个 Future 的调度器。操作系统负责调度线程，但它不会去调度用户态的协程（比如 Future），所以任何使用了协程来处理并发的程序，都需要有一个 executor 来负责协程的调度。

Rust 的 Future 是惰性的：只有在被 poll 轮询时才会运行。其中一个推动它的方式就是在 async 函数中使用 `.await` 来调用另一个 async 函数，但是这个只能解决 async 内部的问题，那些最外层的 async 函数，需要靠执行器 executor 来推动 。

#### executor 运行时

Rust 虽然提供 Future 这样的协程，但它在语言层面并不提供 executor，当不需要使用协程时，不需要引入任何运行时；而需要使用协程时，可以在生态系统中选择最合适的 executor。

Rust 有如下4中常见的 executor ：

-   [futures](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Frust-lang%2Ffutures-rs "https://github.com/rust-lang/futures-rs")：这个库自带了很简单的 executor
-   [tokio](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Ftokio-rs%2Ftokio "https://github.com/tokio-rs/tokio")：提供 executor，当使用 #\[tokio::main\] 时，就隐含引入了 tokio 的 executor
-   [async-std](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fasync-rs%2Fasync-std "https://github.com/async-rs/async-std") ：提供 executor，和 tokio 类似
-   [smol](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fsmol-rs%2Fsmol "https://github.com/smol-rs/smol") ：提供 async-executor，主要提供了 block\_on

#### wake 通知机制

executor 会管理一批 Future (最外层的 async 函数)，然后通过不停地 `poll` 推动它们直到完成。 最开始，执行器会先 `poll` 一次 Future ，后面就不会主动去 `poll` 了，如果 `poll` 方法返回 `Poll::Pending`，就挂起 Future，直到收到某个事件后，通过 `wake()`函数去唤醒被挂起 Future，Future 就可以去主动通知执行器，它才会继续去 `poll`，执行器就可以执行该 Future。这种 wake 通知然后 `poll` 的方式会不断重复，直到 Future 完成。

Waker 提供了 `wake()` 方法：其作用是可以告诉执行器，相关的任务可以被唤醒了，此时执行器就可以对相应的 Future 再次进行 `poll` 操作。

`Context` 是 Waker 的一个封装，先看下 `poll` 方法里的 `Context`：

```rust
pub struct Context<'a> {
    waker: &'a Waker,
    _marker: PhantomData<fn(&'a ()) -> &'a ()>,
}
```

Waker 的定义和相关的代码非常抽象，内部使用了一个 vtable 来允许各种各样的 waker 的行为：

```rust
pub struct RawWakerVTable {
    clone: unsafe fn(*const ()) -> RawWaker,
    wake: unsafe fn(*const ()),
    wake_by_ref: unsafe fn(*const ()),
    drop: unsafe fn(*const ()),
}

```

Rust 自身不提供异步运行时，它只在标准库里规定了一些基本的接口，可以由各个运行时自行决定怎么实现。所以在标准库中，只能看到这些接口的定义，以及“高层”接口的实现，比如 Waker 下的 wake 方法，只是调用了 vtable 里的 wake() 而已 。

```rust
impl Waker {
    /// Wake up the task associated with this `Waker`.
    #[inline]
    pub fn wake(self) {
        // The actual wakeup call is delegated through a virtual function call
        // to the implementation which is defined by the executor.
        let wake = self.waker.vtable.wake;
        let data = self.waker.data;

        // Don't call `drop` -- the waker will be consumed by `wake`.
        crate::mem::forget(self);

        // SAFETY: This is safe because `Waker::from_raw` is the only way
        // to initialize `wake` and `data` requiring the user to acknowledge
        // that the contract of `RawWaker` is upheld.
        unsafe { (wake)(data) };
    }
    ...
}
```

vtable 具体的实现并不在标准库中，而是在第三方的异步运行时里，比如 futures 库的 `waker vtable` [定义](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Frust-lang%2Ffutures-rs%2Fblob%2Fmaster%2Ffutures-task%2Fsrc%2Fwaker.rs "https://github.com/rust-lang/futures-rs/blob/master/futures-task/src/waker.rs")。

### 构建一个计时器

用一个计时器例子，帮助理解 Future 调度机制，目标是： 在创建计时器时创建新线程，休眠特定时间，然后过了时间窗口时通知（signal） 计时器 future。

注：需要用到 `futures` 包的 `ArcWake` 特征，它可以提供一个方便的途径去构建一个 `Waker` 。编辑 `Cargo.toml` ，添加下面依赖:

```rust
[dependencies]
futures = "0.3"
```

计时器 Future 完整代码：

```rust
// future_timer.rs
use futures;
use std::{
    future::Future,
    pin::Pin,
    sync::{Arc, Mutex},
    task::{Context, Poll, Waker},
    thread,
    time::Duration,
};

pub struct TimerFuture {
    shared_state: Arc<Mutex<SharedState>>,
}

/// 在Future和等待的线程间共享状态
struct SharedState {
    /// 定时(睡眠)是否结束
    completed: bool,

    /// 当睡眠结束后，线程可以用`waker`通知`TimerFuture`来唤醒任务
    waker: Option<Waker>,
}

impl Future for TimerFuture {
    type Output = ();
    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        // 通过检查共享状态，来确定定时器是否已经完成
        let mut shared_state = self.shared_state.lock().unwrap();
        if shared_state.completed {
            println!("future ready. execute poll to return.");
            Poll::Ready(())
        } else {
            println!("future not ready, tell the future task how to wakeup to executor");
            // 设置`waker`，这样新线程在睡眠(计时)结束后可以唤醒当前的任务，接着再次对`Future`进行`poll`操作,
            // 下面的`clone`每次被`poll`时都会发生一次，实际上，应该是只`clone`一次更加合理。
            // 选择每次都`clone`的原因是： `TimerFuture`可以在执行器的不同任务间移动，如果只克隆一次，
            // 那么获取到的`waker`可能已经被篡改并指向了其它任务，最终导致执行器运行了错误的任务
            shared_state.waker = Some(cx.waker().clone());
            Poll::Pending
        }
    }
}

impl TimerFuture {
    /// 创建一个新的`TimerFuture`，在指定的时间结束后，该`Future`可以完成
    pub fn new(duration: Duration) -> Self {
        let shared_state = Arc::new(Mutex::new(SharedState {
            completed: false,
            waker: None,
        }));

        // 创建新线程
        let thread_shared_state = shared_state.clone();
        thread::spawn(move || {
            // 睡眠指定时间实现计时功能
            thread::sleep(duration);
            let mut shared_state = thread_shared_state.lock().unwrap();
            // 通知执行器定时器已经完成，可以继续`poll`对应的`Future`了
            shared_state.completed = true;
            if let Some(waker) = shared_state.waker.take() {
                println!("detect future is ready, wakeup the future task to executor.");
                waker.wake()
            }
        });

        TimerFuture { shared_state }
    }
}

fn main() {
    // 我们现在还没有实现调度器，所以要用一下futues库里的一个调度器。
    futures::executor::block_on(TimerFuture::new(Duration::new(10, 0)));    
}

```

执行结果如下：

```
future not ready, tell the future task how to wakeup to executor
detect future is ready, wakeup the future task to executor.
future ready. execute poll to return.
```

可以看到，刚开始的时候，定时10s事件还未完成，处在`Pending`状态，这时要告诉这个任务后面就绪后怎么唤醒去调度执行。等10s后，定时事件完成了，通过前面的设置的`Waker`，唤醒这个`Future`任务去调度执行。

### 构建一个执行器

上面的代码，我们并没有实现调度器，而是使用的`futures`库中提供的一个调度器去执行，下面自己实现一个调度器，看一下它的原理。而在Rust中，真正要用的话，还是要学习`tokio`库，这里我们只是为了讲述一下实现原理，以便于理解异步是怎么一回事。关键代码如下：

```rust
// future_executor.rs
use {
    futures::{
        future::{BoxFuture, FutureExt},
        task::{waker_ref, ArcWake},
    },
    std::{
        future::Future,
        sync::mpsc::{sync_channel, Receiver, SyncSender},
        sync::{Arc, Mutex},
        task::Context,
        time::Duration,
    },
   
};

mod future_timer;
 // 引入之前实现的定时器模块
use future_timer::TimerFuture;

/// 任务执行器，负责从通道中接收任务然后执行
struct Executor {
    ready_queue: Receiver<Arc<Task>>,
}

/// `Spawner`负责创建新的`Future`然后将它发送到任务通道中
#[derive(Clone)]
struct Spawner {
    task_sender: SyncSender<Arc<Task>>,
}

/// 一个 Future，它可以调度自己(将自己放入任务通道中)，然后等待执行器去`poll`
struct Task {
    /// 进行中的Future，在未来的某个时间点会被完成
    ///
    /// 按理来说`Mutex`在这里是多余的，因为我们只有一个线程来执行任务。但是由于
    /// Rust并不聪明，它无法知道`Future`只会在一个线程内被修改，并不会被跨线程修改。因此
    /// 我们需要使用`Mutex`来满足这个笨笨的编译器对线程安全的执着。
    ///
    /// 如果是生产级的执行器实现，不会使用`Mutex`，因为会带来性能上的开销，取而代之的是使用`UnsafeCell`
    future: Mutex<Option<BoxFuture<'static, ()>>>,

    /// 可以将该任务自身放回到任务通道中，等待执行器的poll
    task_sender: SyncSender<Arc<Task>>,
}

fn new_executor_and_spawner() -> (Executor, Spawner) {
    // 任务通道允许的最大缓冲数(任务队列的最大长度)
    // 当前的实现仅仅是为了简单，在实际的执行中，并不会这么使用
    const MAX_QUEUED_TASKS: usize = 10_000;
    let (task_sender, ready_queue) = sync_channel(MAX_QUEUED_TASKS);
    (Executor { ready_queue }, Spawner { task_sender })
}

impl Spawner {
    fn spawn(&self, future: impl Future<Output = ()> + 'static + Send) {
        let future = future.boxed();
        let task = Arc::new(Task {
            future: Mutex::new(Some(future)),
            task_sender: self.task_sender.clone(),
        });
        println!("first dispatch the future task to executor.");
        self.task_sender.send(task).expect("too many tasks queued.");
    }
}

/// 实现ArcWake，表明怎么去唤醒任务去调度执行。
impl ArcWake for Task {
    fn wake_by_ref(arc_self: &Arc<Self>) {
        // 通过发送任务到任务管道的方式来实现`wake`，这样`wake`后，任务就能被执行器`poll`
        let cloned = arc_self.clone();
        arc_self
            .task_sender
            .send(cloned)
            .expect("too many tasks queued");
    }
}

impl Executor {
     // 实际运行具体的Future任务，不断的接收Future task执行。
    fn run(&self) {
        let mut count = 0;
        while let Ok(task) = self.ready_queue.recv() {
            count = count + 1;
            println!("received task. {}", count);
            // 获取一个future，若它还没有完成(仍然是Some，不是None)，则对它进行一次poll并尝试完成它
            let mut future_slot = task.future.lock().unwrap();
            if let Some(mut future) = future_slot.take() {
                // 基于任务自身创建一个 `LocalWaker`
                let waker = waker_ref(&task);
                let context = &mut Context::from_waker(&*waker);
                // `BoxFuture<T>`是`Pin<Box<dyn Future<Output = T> + Send + 'static>>`的类型别名
                // 通过调用`as_mut`方法，可以将上面的类型转换成`Pin<&mut dyn Future + Send + 'static>`
                if future.as_mut().poll(context).is_pending() {
                    println!("executor run the future task, but is not ready, create a future again.");
                    // Future还没执行完，因此将它放回任务中，等待下次被poll
                    *future_slot = Some(future);
                } else {
                    println!("executor run the future task, is ready. the future task is done.");
                }
            }
        }
    }
}


fn main() {
    let (executor, spawner) = new_executor_and_spawner();

   // 将 TimerFuture 封装成一个任务，分发到调度器去执行
    spawner.spawn(async {
        println!("TimerFuture await");
        // 创建定时器Future，并等待它完成
        TimerFuture::new(Duration::new(10, 0)).await;
        println!("TimerFuture Done");
    });

    // drop掉任务，这样执行器就知道任务已经完成，不会再有新的任务进来
    drop(spawner);

    // 运行执行器直到任务队列为空
    // 任务运行后，会先打印`howdy!`, 暂停2秒，接着打印 `done!`
    executor.run();
}
```

运行结果如下：

```rust
first dispatch the future task to executor.
received task. 1
TimerFuture await
future not ready, tell the future task how to wakeup to executor
executor run the future task, but is not ready, create a future again.
detect future is ready, wakeup the future task to executor.
received task. 2
future ready. execute poll to return.
TimerFuture Done
executor run the future task, is ready. the future task is done.
```

第一次调度的时候，因为还没有就绪，在`Pending`状态，告诉这个任务，后面就绪是怎么唤醒`wake`该任务。然后当事件就绪的时候，因为前面告诉了如何唤醒，按方法唤醒了该任务去调度执行。

### 异步处理流程

Reactor Pattern 是构建高性能事件驱动系统的一个很典型模式，executor 和 reactor 是 Reactor Pattern 的组成部分。Reactor pattern 包含三部分：

-   task：待处理的任务。任务可以被打断，并且把控制权交给 executor，等待之后的调度
-   executor：一个调度器。维护等待运行的任务（ready queue），以及被阻塞的任务（wait queue）
-   reactor：维护事件队列。当事件来临时，通知 executor 唤醒某个任务等待运行

executor 会调度执行待处理的任务，当任务无法继续进行却又没有完成时，它会挂起任务，并设置好合适的唤醒条件。之后，如果 reactor 得到了满足条件的事件，它会唤醒之前挂起的任务，然后 executor 就有机会继续执行这个任务。这样一直循环下去，直到任务执行完毕。

Rust 使用 Future 做异步处理就是一个典型的 Reactor Pattern 模式。

> 以 tokio 为例：async/await 提供语法层面的支持，Future 是异步任务的数据结构，当 .await 时，executor 就会调度并执行它

下图为 tokio 上 Future 异步处理整个流程： ![future.jpeg](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/986088b35a0b4efea6657f9d2263b663~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp?)

引用自《陈天 · Rust 编程第一课》

> 注：tokio 的调度器会运行在多个线程上，运行线程上自己的 ready queue 上的任务（Future），如果没有，就去别的线程的调度器上偷一些过来运行（work-stealing 调度机制）。当某个任务无法再继续取得进展，此时 Future 运行的结果是 `Poll::Pending`，那么调度器会挂起任务，并设置好合适的唤醒条件（Waker），等待被 reactor 唤醒。而reactor 会利用操作系统提供的异步 I/O（如epoll / kqueue / IOCP），来监听操作系统提供的 IO 事件，当遇到满足条件的事件时，就会调用 Waker.wake() 唤醒被挂起的 Future，这个 Future 会回到 ready queue 等待执行。

### 总结

Future 是 Rust 异步编程的核心，代表一些将在未来完成的操作。 Rust 的 Future 是惰性的，需要执行器 executor 调度执行，这种调度执行实现基于轮询，在当前轮询 `poll` 中，若 Future 完成了，则返回 `Poll::Ready(result)`，即得到 Future 的值并返回；若 Future 还没完成，则返回 `Poll::Pending()`，此时 Future 会被挂起，需要等某个事件发生 Waker 将其唤醒，Waker 提供`wake()`方法来告诉执行器哪个关联任务应该要唤醒。当`wake()`函数被调用时， 执行器知道 Waker 关联的任务已经准备好继续了，该 future 会被再轮询一遍。这种 `wake` 通知然后 `poll` 的方式会不断重复，直到  Future 完成。

每个异步任务分成三个阶段:

1.  **轮询阶段**(The Poll)： 调度执行器(executor)触发一个`Future`被轮询后，开始执行，遇阻塞（`Pending`）则挂起进入等待阶段。
2.  **等待阶段**：事件源(通常称为reactor)注册 `Waker` 等待一个事件发生，当该事件准备好时唤醒 wake 相应的`Future` ，进入唤醒阶段。
3.  **唤醒阶段**：事件发生，相应的`Future`被 Waker 唤醒。 执行器(executor)调度`Future`再次被轮询，并向前走一步，直到它完成或达到一个`Pending`点，不能再向前走, 如此往复，直到最终完成。

### 参考

-   [Rust 圣经 - 异步编程](https://link.juejin.cn/?target=https%3A%2F%2Fcourse.rs%2Fadvance%2Fasync%2Ffuture-excuting.html "https://course.rs/advance/async/future-excuting.html")
    
-   [Rust 异步编程](https://link.juejin.cn/?target=https%3A%2F%2Fhuangjj27.github.io%2Fasync-book%2F02_execution%2F02_future.html "https://huangjj27.github.io/async-book/02_execution/02_future.html")
    
-   [200 行代码讲透 RUST FUTURES](https://link.juejin.cn/?target=https%3A%2F%2Fstevenbai.top%2Frust%2Ffutures_explained_in_200_lines_of_rust "https://stevenbai.top/rust/futures_explained_in_200_lines_of_rust")
    
-   [Futures Explained in 200 Lines of Rust](https://link.juejin.cn/?target=https%3A%2F%2Fcfsamson.github.io%2Fbooks-futures-explained%2Fintroduction.html "https://cfsamson.github.io/books-futures-explained/introduction.html")
    
-   [陈天 · Rust 编程第一课 - 异步处理](https://link.juejin.cn/?target=https%3A%2F%2Ftime.geekbang.org%2Fcolumn%2Farticle%2F455413 "https://time.geekbang.org/column/article/455413")