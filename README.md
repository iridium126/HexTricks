# HexTricks

**HexCasting + Trickster 联动模组**

支持 HexCasting 和 Trickster 的法术相互调用，实现两个魔法模组的完美联动。

---

## 功能特性

### 核心功能

#### **双向法术互调用**

**HexCasting → Trickster**
- 在 HexCasting 法术中直接调用并执行 Trickster 法术片段
- 支持完整的参数传递和返回值接收
- 完整支持 Trickster 的异步/延迟法术执行机制

**Trickster → HexCasting**
- 在 Trickster 法术中执行 HexCasting 法术
- 从副手读取 HexCasting Iota 数据
- 支持单个/列表形式的 Hex 图案执行
- 法术组构台方块原生施法环境

#### **双向类型转换**
- **HexCasting Iota ↔ Trickster Fragment**
  - 数字、布尔值、三维向量、实体引用
  - 列表嵌套转换、空值映射
  - 未知类型自动包装，保证类型安全

#### **法术组构台施法环境**
- Trickster 法术构造方块原生支持 HexCasting
- 自动魔力抽取与消耗
- 32 格施法范围，与玩家一致
- 完整的粒子效果与声音播放

---

## HexCasting 法术操作

### 新增的 Hex 图案

| 图案名 | 图案 (SOUTH_EAST 起点) | 栈操作 | 描述 |
|---------|-----------------------|--------|------|
| **Execute Trick** | `wdwewawqwqw` | `TrickIota [List?] → [Result?]` | 执行 Trickster 法术 |
| **Read Trick** | `wawqwqwqwqwq` | `→ TrickIota` | 从副手物品读取法术 |

---

## Trickster 法术操作

### 新增的 Trickster 图案

| 图案名 | 签名 | 描述 |
|--------|------|------|
| **read_iota_offhand** | `-> any` | 从副手读取 HexCasting Iota 数据 |
| **run_list_pattern_iota_string** | `string/list, ... -> any` | 执行 HexCasting 法术 |

---

## 安装

1. 下载对应版本的 `.jar` 文件
2. 放入 Minecraft 的 `mods` 文件夹
3. 确保已安装所有必需依赖
4. 启动游戏即可使用

---

## 构建说明

### 环境要求
- JDK 21
- Git

### 构建步骤

```bash
# 克隆仓库
git clone https://github.com/iridium126/HexTricks.git
cd HexTricks

# 构建
./gradlew build

# 构建产物位置
# build/libs/HexTricks-*.jar
```

## 许可证

本项目采用 **MIT License** - 详见 [LICENSE](LICENSE) 文件。

---

<p align="center">
  <sub>用魔法驱动魔法 ✨</sub>
</p>
