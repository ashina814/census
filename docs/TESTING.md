# Census — 動作確認メモ（in-world テスト手順）

開発クライアント起動: `./gradlew runClient`

## 最初に1回だけ
ワールドに入ったら **Esc →「LANに公開」→「チートの許可: オン」→ 公開**。
これでホストが op レベル4になり、`/tick` など全コマンドが使える。
（シングルプレイ既定はレベル2で、`/tick` だけ "Unknown command" になるため）

## 準備コマンド
```
/gamemode creative
/give @s census:census_book
/difficulty peaceful
/summon minecraft:villager ~ ~ ~      # 数回
```

## 2大デバッグ武器
- `/tick warp 6000` … 時間依存機能を一気に進める（reflection=6000t毎, 繁殖CD=24000t）
  - 注意: `/time add` は効かない（サーバーtickカウンタは進まない）
- `/data get entity @e[type=villager,limit=1,sort=nearest] neoforge:attachments`
  … persona/memory/emotion/reputation/lineage/reflections を生NBTで確認

## 確認項目
- **コア**: 村人を見る→HUD（名前/気分/性格）。戸籍書を右クリック→情報カード。名前が全員バラバラ。
- **M3 心境**: 村人を5〜6回殴る → `/tick warp 6000` → 戸籍書 → 「心境: …を恨んでいる/心に傷を負っている」
- **M2 遠隔復讐**: 大人2体を近接召喚 → `/summon minecraft:villager ~ ~ ~ {Age:-24000}`（=2体の子）
  → 子に `/census family`（gen1・親ID）→ 親を殴り殺す → `/tick warp 400`
  → 子に `/census memory` で RELATIVE_KILLED 確認
- **M1 軽さ**: F3 の ms/tick グラフ。村人を増やして安定するか。
  厳密計測: `/tick`権限後に `/debug start` →20〜30秒プレイ→ `/debug stop` → `run/debug/profile-results-*.txt`
- **M0 ネームプレートoff**: `run/config/census-server.toml` の `nameTagsAlwaysVisible=false` → 再起動

## ログ / 設定
- ログ: `run/logs/latest.log`
- 設定: `run/config/census-server.toml`
- プロファイル: `run/debug/`（`/debug stop` で生成）
