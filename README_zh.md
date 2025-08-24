<p align="center">
  <img width="256" height="256" src="src\main\resources\assets\builder\icon.png">
</p>

[English](README.md) | 中文

# Builder

## 简介

一个通过特定格式的 JSON 文件批量导入建筑的 Fabric 模组

初衷是为 **三维模型转 Minecraft 工具 —— [Minecraftify2.0](https://github.com/Ivans-11/Minecraftify2/)** 而开发的数据导入模组，故常与其配合使用。

当然，本模组也可以单独使用。只要能生成符合格式的 JSON 文件，就可以使用本模组导入建筑。

## 功能
- 从 `config/mybuilds/` 目录下的 JSON 文件中批量导入方块。
- 玩家可以使用 `builder:anchor_block`（锚点方块）设置相对坐标系原点和朝向，从而精确地控制导入方块的位置与朝向。
- 支持撤回功能，避免误操作。最多可以撤回最近 3 次建造操作。

## 使用方法

1. 在 `config/mybuilds/` 目录下创建特定格式的 JSON 文件，例如 [`example.json`](./example.json)

   ```json
    {
        "minecraft:stone": [[1, 0, 0],[2, 0, 0],[3, 0, 0]],
        "minecraft:oak_planks": [[0, 1, 1],[0, 1, 2]]
    }
   ```
   - 其中，键为方块 ID，值为相对坐标数组。
   - 可使用工具 [**Minecraftify2.0**](https://github.com/Ivans-11/Minecraftify2/releases) 从三维模型文件生成，具体使用方法见其[仓库说明](https://github.com/Ivans-11/Minecraftify2)
2. 在游戏中放置一个 `builder:anchor_block`（锚点方块） ，用于确定参考坐标系原点和朝向。

    ![](image/anchor.png)
3. 输入命令：

   ```
   /builder place example
   ```

   即可在距离玩家最近的锚点处生成建筑。

   ![](image/build.png)
4. 如果需要撤回操作，输入：

   ```
   /builder undo
   ```

   将撤回最近一次生成操作（最多支持 3 步）。

## 命令列表

- `/builder place <name>`
  从 `config/mybuilds/<name>.json` 导入建筑
- `/builder list`
  列出 `config/mybuilds/` 目录下的所有 JSON 文件
- `/builder anchors`
  列出所有锚点方块的坐标
- `/builder undo`
  撤回最近一次生成操作

## 注意

- 必须先放置锚点方块才能生成建筑，生成建筑时会自动寻找距离玩家最近的锚点方块。
- 锚点方块可在建筑方块物品栏中找到。

    ![](image/item_zh.png)
- 撤回仅能恢复由 `/build place` 命令生成的方块，不影响手动放置的方块。

## 致谢

本项目基于 [FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod) 开发。