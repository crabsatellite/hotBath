# Test Checklist - Bath Water Bottles

## 测试环境要求

- [ ] 游戏版本：NeoForge 1.21.1
- [ ] Hot Bath mod 已加载

## 功能测试清单

### 1. 浴水瓶效果测试

#### 1.1 热水瓶 (Hot Water Bottle)

- [ ] 饮用后获得速度 I 效果（5 秒）
- [ ] [ToughAsNails] 饮用后温度变为 WARM（10 秒）
- [ ] [ToughAsNails] 温度效果持续 10 秒后消失
- [ ] [Cold Sweat] 饮用后体温升至 36°C（5 秒）
- [ ] [Cold Sweat] 如果环境温度高于 36°C，无温度变化
- [ ] [Cold Sweat] 温度效果持续 5 秒后消失

#### 1.2 蜂蜜浴水瓶 (Honey Bath Water Bottle)

- [ ] 饮用后恢复 2 心生命值
- [ ] 饮用后获得伤害吸收 I 效果（5 秒）
- [ ] [ToughAsNails] 饮用后温度变为 WARM（10 秒）
- [ ] [Cold Sweat] 饮用后体温升至 36°C（5 秒）

#### 1.3 牛奶浴水瓶 (Milk Bath Water Bottle)

- [ ] 饮用后随机移除 1 个负面效果
- [ ] [ToughAsNails] 饮用后温度变为 WARM（10 秒）
- [ ] [Cold Sweat] 饮用后体温升至 36°C（5 秒）

#### 1.4 草药浴水瓶 (Herbal Bath Water Bottle)

- [ ] 饮用后获得抗性提升 I 效果（5 秒）
- [ ] [ToughAsNails] 饮用后温度变为 WARM（10 秒）
- [ ] [Cold Sweat] 饮用后体温升至 36°C（5 秒）

#### 1.5 牡丹浴水瓶 (Peony Bath Water Bottle)

- [ ] 饮用后恢复 2 心生命值
- [ ] [ToughAsNails] 饮用后温度变为 WARM（10 秒）
- [ ] [Cold Sweat] 饮用后体温升至 36°C（5 秒）

#### 1.6 玫瑰浴水瓶 (Rose Bath Water Bottle)

- [ ] 饮用后恢复 2 心生命值
- [ ] 饮用后获得力量 I 效果（5 秒）
- [ ] [ToughAsNails] 饮用后温度变为 WARM（10 秒）
- [ ] [Cold Sweat] 饮用后体温升至 36°C（5 秒）

### 2. 浴水瓶温度效果机制测试

_测试浴水瓶饮用后的温度调节功能_

#### 2.1 ToughAsNails 温度效果

_仅在安装 ToughAsNails mod 时测试_

- [ ] [ToughAsNails] 温度效果准确持续 10 秒
- [ ] [ToughAsNails] 10 秒后温度恢复到正常状态
- [ ] [ToughAsNails] 在温度效果激活期间再次饮用浴水瓶
- [ ] [ToughAsNails] 温度效果时间重置为 10 秒（从当前时间重新计算）
- [ ] [ToughAsNails] 多次连续饮用，每次都重置为 10 秒

#### 2.2 Cold Sweat 温度效果

_仅在安装 Cold Sweat mod 时测试_

- [ ] [Cold Sweat] 温度效果准确持续 5 秒
- [ ] [Cold Sweat] 5 秒后体温恢复到正常状态
- [ ] [Cold Sweat] 在寒冷环境中饮用，体温升至 36°C
- [ ] [Cold Sweat] 在温暖环境（>36°C）中饮用，体温不受影响
- [ ] [Cold Sweat] 在温度效果激活期间再次饮用浴水瓶
- [ ] [Cold Sweat] 温度效果时间重置为 5 秒（从当前时间重新计算）

#### 2.3 与浴缸温度效果的独立性

- [ ] [ToughAsNails] 在热水浴缸中时，温度为 WARM（来自 HotBathTANPlayerModifier）
- [ ] [ToughAsNails] 离开浴缸后饮用浴水瓶，温度仍为 WARM（来自 BathWaterBottleTANModifier）
- [ ] [ToughAsNails] 两个效果互不干扰
- [ ] [Cold Sweat] 在浴缸中饮用浴水瓶，两种温度效果可以叠加
- [ ] [Cold Sweat] 离开浴缸后，浴水瓶效果继续生效 5 秒

### 3. Mod 兼容性测试

#### 3.1 无额外 Mod

- [ ] 所有浴水瓶的药水效果正常工作
- [ ] 游戏正常运行，无崩溃

#### 3.2 ToughAsNails 集成

- [ ] [ToughAsNails] 游戏启动时日志显示成功注册温度修改器
- [ ] [ToughAsNails] 所有浴水瓶饮用后正确应用温度效果
- [ ] [ToughAsNails] 温度效果正确应用
- [ ] [ToughAsNails] 在浴缸中使用右键喝水功能，获得温暖效果（10 秒）
- [ ] [ToughAsNails] 右键喝水时不会获得浴水瓶的药水效果（只有温度）
- [ ] [ToughAsNails] 移除 ToughAsNails mod 后游戏仍能正常运行

#### 3.3 Cold Sweat 集成

- [ ] [Cold Sweat] 游戏启动时日志显示成功注册温度修改器
- [ ] [Cold Sweat] 所有浴水瓶饮用后正确应用温度效果
- [ ] [Cold Sweat] 浴水瓶温度效果持续 5 秒
- [ ] [Cold Sweat] 环境温度高于 36°C 时无温度变化
- [ ] [Cold Sweat] 玩家进入热水浴缸时体温升至约 37°C
- [ ] [Cold Sweat] 在寒冷环境中，浴缸提供温暖效果
- [ ] [Cold Sweat] 浴缸温度效果随距离衰减（7 格范围内）
- [ ] [Cold Sweat] 在热带环境中，浴缸温度不会降低玩家体温
- [ ] [Cold Sweat] 移除 Cold Sweat mod 后游戏仍能正常运行

### 4. ToughAsNails 右键喝水集成测试

_测试 ToughAsNails 的手动喝水功能在浴缸中的表现_

#### 4.1 基础右键喝水功能

- [ ] [ToughAsNails] 在热水浴缸中潜行 + 主手空着 + 右键，可以喝水
- [ ] [ToughAsNails] 喝水后恢复口渴值
- [ ] [ToughAsNails] 喝水后获得温暖效果（TAN: 10 秒 WARM）
- [ ] [ToughAsNails + Cold Sweat] 喝水后同时获得两种温度效果（TAN: 10 秒, Cold Sweat: 5 秒 36°C）

#### 4.2 不同浴缸类型测试

- [ ] [ToughAsNails] 在蜂蜜浴缸中右键喝水，获得温度效果
- [ ] [ToughAsNails] 在牛奶浴缸中右键喝水，获得温度效果
- [ ] [ToughAsNails] 在草药浴缸中右键喝水，获得温度效果
- [ ] [ToughAsNails] 在牡丹浴缸中右键喝水，获得温度效果
- [ ] [ToughAsNails] 在玫瑰浴缸中右键喝水，获得温度效果
- [ ] [ToughAsNails] 所有浴缸类型右键喝水效果一致（只有温度，无药水效果）

#### 4.3 与浴水瓶效果对比

- [ ] [ToughAsNails] 右键喝水：只有温度效果
- [ ] [ToughAsNails] 喝浴水瓶：温度效果 + 药水效果
- [ ] [ToughAsNails] 右键喝水后不会获得速度、力量等药水效果
- [ ] [ToughAsNails] 右键喝水后不会恢复生命值

### 5. 热水浴缸温度效果测试

_测试玩家在热水浴缸中的温度调节功能_

#### 4.1 基础温度效果

- [ ] [ToughAsNails] 玩家在热水浴缸中时，温度显示为 WARM
- [ ] [Cold Sweat] 玩家在热水浴缸中时，体温升至约 37°C（体温舒适区）
- [ ] [Cold Sweat] 离开浴缸后，体温逐渐恢复到环境温度

#### 4.2 环境交互测试

- [ ] [Cold Sweat] 在寒冷生物群系（如雪原）中，浴缸有效提供温暖
- [ ] [Cold Sweat] 在极寒天气中，浴缸可以防止玩家冻伤
- [ ] [Cold Sweat] 在炎热生物群系（如沙漠）中，浴缸温度不会让玩家过热
- [ ] [Cold Sweat] 浴缸周围 7 格范围内都能感受到温暖效果
- [ ] [Cold Sweat] 距离越远，温暖效果越弱

#### 4.3 浴缸与浴水瓶效果叠加

- [ ] [ToughAsNails] 在浴缸中饮用浴水瓶，两种温度效果可以共存
- [ ] [ToughAsNails] 离开浴缸后，浴水瓶的温度效果继续生效 10 秒
- [ ] [Cold Sweat] 在浴缸中饮用浴水瓶，浴水瓶的 36°C 效果可以覆盖浴缸的 37°C
- [ ] [Cold Sweat] 离开浴缸后，浴水瓶的温度效果继续生效 5 秒

### 5. 边界情况测试

#### 4.1 多玩家测试

- [ ] 多个玩家同时饮用浴水瓶
- [ ] 每个玩家的药水效果独立
- [ ] [ToughAsNails] 每个玩家的温度效果独立计时
- [ ] [ToughAsNails] 不会互相干扰
- [ ] [Cold Sweat] 多个玩家同时在浴缸中，各自体温独立调节

#### 4.2 玩家重新加入

- [ ] [ToughAsNails] 玩家在温度效果激活期间退出游戏
- [ ] [ToughAsNails] 重新加入后，温度效果自动清除（预期行为）

#### 4.3 死亡测试

- [ ] [ToughAsNails] 玩家在温度效果激活期间死亡
- [ ] [ToughAsNails] 重生后温度效果正常清除

#### 4.4 不同浴缸类型测试

- [ ] [Cold Sweat] 所有类型浴缸（热水、蜂蜜、牛奶、草药、牡丹、玫瑰）都提供相同的温度效果
- [ ] [ToughAsNails] 所有类型浴缸都提供 WARM 温度效果

## 测试结果记录

| 测试项                    | 通过 | 失败 | 备注 |
| ------------------------- | ---- | ---- | ---- |
| 热水瓶效果                | ⬜   | ⬜   |      |
| 蜂蜜浴水瓶效果            | ⬜   | ⬜   |      |
| 牛奶浴水瓶效果            | ⬜   | ⬜   |      |
| 草药浴水瓶效果            | ⬜   | ⬜   |      |
| 牡丹浴水瓶效果            | ⬜   | ⬜   |      |
| 玫瑰浴水瓶效果            | ⬜   | ⬜   |      |
| TAN 浴水瓶温度持续时间    | ⬜   | ⬜   |      |
| TAN 浴水瓶效果不叠加      | ⬜   | ⬜   |      |
| TAN 浴缸温度              | ⬜   | ⬜   |      |
| Cold Sweat 浴水瓶温度     | ⬜   | ⬜   |      |
| Cold Sweat 浴水瓶环境响应 | ⬜   | ⬜   |      |
| Cold Sweat 浴缸温度       | ⬜   | ⬜   |      |
| Cold Sweat 浴缸范围效果   | ⬜   | ⬜   |      |
| 多玩家独立性              | ⬜   | ⬜   |      |

## 已知问题

- 无

## 测试日期

- 测试人员：
- 测试日期：
- ToughAsNails 版本：
- Cold Sweat 版本：
- Hot Bath mod 版本：
