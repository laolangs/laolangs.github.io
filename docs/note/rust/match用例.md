# match 常用写法

@ 绑定语法

```rust
let s = 1;
match s{
    1 | 2 =>(), // s=1 或 2
    s @ 1 | s @ 2 =>(),  // s=1 或 2
    s @ 4..=10 => // s 在 4到10之间
    _=>(),
}
```

条件判断：

```rust
let s = 1;
match s{
    s if s>10 =>(),  // s>10
    _=>(),
}
```

结构体：

```rust
struct A{
    am:i32,
    te:String,
}
let s = A{am:1,te:"abc".to_owned()};
match s{
    A{am,..} if am>10 =>(),  // s>10
    A{am,te} if &te == "abc"=>()  // s 在 4到10之间
    _=>(),
}
```

```rust
// Topic: Match guards & binding
//
// Summary:
// * A tile-based game requires different logic for different kinds
//   of tiles. Print different messages depending on the kind of
//   tile selected.
//
// Requirements:
// * Bricks:
//   * Colored bricks should print "The brick color is [color]"
//   * Other bricks should print "[Bricktype] brick"
// * Water:
//   * Pressure levels 10 and over should print "High water pressure!"
//   * Pressure levels under 10 should print "Water pressure level: [Pressure]"
// * Grass, Dirt, and Sand should all print "Ground tile"
// * Treasure Chests:
//   * If the treasure is Gold and the amount is at least 100, print "Lots of gold!"
// * Everything else shoud not print any messages
//
// Notes:
// * Use a single match expression utilizing guards to implement the program
// * Run the program and print the messages with at least 4 different tiles

#[derive(Debug, PartialEq)]
enum TreasureItem {
    Gold,
    SuperPower,
}

#[derive(Debug)]
struct TreasureChest {
    content: TreasureItem,
    amount: usize,
}

#[derive(Debug)]
struct Pressure(u16);

#[derive(Debug)]
enum BrickStyle {
    Dungeon,
    Gray,
    Red,
}

#[derive(Debug)]
enum Tile {
    Brick(BrickStyle),
    Dirt,
    Grass,
    Sand,
    Treasure(TreasureChest),
    Water(Pressure),
    Wood,
}
fn tile_print(tile: Tile) {
    match tile {
        Tile::Grass | Tile::Dirt | Tile::Sand => println!("Ground tile"),
        // @ 绑定语法
        Tile::Brick(brick @ BrickStyle::Gray | brick @ BrickStyle::Red) => {
            println!("The brick color is Gray")
        }

        Tile::Brick(other) => println!("{:?} brick", other),
        // 条件判断
        Tile::Water(pressure) if pressure.0 >= 10 => println!("High water pressure!"),

        Tile::Water(pressure) if pressure.0 < 10 => {
            println!("Water pressure level: {}", pressure.0)
        }
        // 结构体判断
        Tile::Treasure(TreasureChest {
            amount,
            content: TreasureItem::Gold,
        }) if amount >= 100 => println!("Lots of gold!"),
        Tile::Treasure(TreasureChest {
            amount,
            ..
        }) if amount >= 100 => println!("Lots of amount!"),
        _ => (),
    }
}
fn main() {
    let tile1 = Tile::Dirt;
    tile_print(tile1);
    let tile1 = Tile::Brick(BrickStyle::Dungeon);
    tile_print(tile1);
    let tile1 = Tile::Treasure(TreasureChest {
        content: TreasureItem::Gold,
        amount: 100,
    });
    tile_print(tile1);
    let tile1 = Tile::Water(Pressure(10));
    tile_print(tile1);
}

```
