# City Builder (Fabric 1.19.2)

設計書(WebUI → サーバーMod → クライアント)に沿った、クリエイティブ向け「都市計画CAD」です。
建物は**現代建築のみ**(マンション・オフィス・駅・病院・商業施設など 42 種)。

```
WebUI (webui/citybuilder-planner.html)  ──city.json──▶  サーバー (Mod + templates + catalog.json)
                                                           │  カタログ情報だけを同期
                                                           ▼
                                                    クライアント(専用Mod)
                                                    GUIで建物を選ぶ → プレビュー → 左クリック
                                                           │ 配置指示(ID・座標・回転)だけ送信
                                                           ▼
                                                    サーバーが検証してブロックを配置
```

## 同梱物

| パス | 内容 |
|---|---|
| `mod/` | Fabric 1.19.2 Mod のソース一式(Gradleプロジェクト) |
| `server-data/citybuilder/` | 建物テンプレート `templates/*.schem`(42個)、`catalog.json`、サンプル `plans/city01.json` |
| `webui/citybuilder-planner.html` | 都市計画エディタ(単体HTML。ブラウザで開くだけ) |
| `tools/` | テンプレート生成スクリプト(Node.js)。建物の調整・追加用 |

## 重要: このソースは未コンパイルです

作成環境では Maven(Fabric/Mojang)に接続できず、**Mod は一度もビルド・起動していません**。
ソースは Fabric 1.19.2 の API に合わせて書いてありますが、初回ビルド時に小さなコンパイルエラー(API名の違い等)が出る可能性があります。
WebUI とテンプレート(.schem)はこの環境で実際に動作確認済みです。

特に怪しい箇所(エラーが出たらここを疑ってください):
- `client/PlacementMode.java` … クリック処理(`KeyBinding.wasPressed` で消費する方式)
- `client/PreviewRenderer.java` … 描画 API(`Matrix3f/4f`、`getRenderTypeLinesShader` など)
- `CityCommands.java` … `CommandRegistrationCallback`(v2, 引数3つ)

## インストール

### 1. ビルド
JDK 17 と Gradle 7.5 以上が必要です。
```
cd mod
gradle wrapper --gradle-version 7.6   # 初回のみ
./gradlew build
```
`mod/build/libs/citybuilder-1.0.0.jar` ができます。(IntelliJ IDEA で `mod/` を開いて Gradle の `build` を実行でも可)

### 2. サーバー
1. Fabric Loader 0.14.x + Fabric API 0.77.0+1.19.2 を入れた 1.19.2 サーバーに jar を入れる
2. `server-data/citybuilder/` の中身を **`<サーバー>/config/citybuilder/`** にコピー
   (`catalog.json`, `templates/`, `plans/`)
3. 起動。`config/citybuilder/config.json` が自動生成されます

### 3. クライアント
同じ jar と Fabric API を `mods/` に入れます(建物データはサーバーから受け取るので、テンプレートのコピーは不要)。

## 使い方

### Minecraft 内
- 配置モード中、左クリックは(誤ってブロックを壊さないよう)無効になります
- **B** … メニューを開く(建物 / 道路 / 区画 の3タブ)
- 建物を選ぶと配置モード: 見ている地面に半透明のボリューム+枠線+正面マーカー(黄矢印)が追従
  - **右クリック** … ここに建てる(連続で置けます) / **Shift+右クリック** … やめる
  - **R** … 90°回転 / **矢印キー** … 東西南北に1ブロック / **PageUp・PageDown** … 上下
- 道路ツール: 右クリックで点を追加、**同じ点をもう一度右クリック**で確定(2点以上)。Shift+右クリックで1点戻す(点が無ければ終了)
- 区画ツール: 右クリックで角を2回指定(Shift+右クリックで取り消し)(芝生・広場・駐車場・水面など)
- 権限が無いとプレビューは出ますが配置されません(HUDに表示)
- 配置基準: **見ている地表ブロックが建物の1段目(地面層)を置き換えます**。建物の最小コーナーは枠線の左上(北西)です

### コマンド(OPレベル2、`config.json` で変更可)
```
/citybuilder reload                       設定とカタログの再読み込み(接続中のクライアントにも再送)
/citybuilder list [カテゴリ]              建物一覧
/citybuilder info <id>                    寸法などの確認
/citybuilder place <id> <x y z> [0|90|180|270]
/citybuilder road <from> <to> [幅] [路面]   直線の道路
/citybuilder area <from> <to> <種類>       矩形区画
/citybuilder plan list
/citybuilder plan load <名前> [x y z]       WebUIで作った計画を施工
/citybuilder undo                          直前の作業を元に戻す(プレイヤーごとに履歴)
/citybuilder cancel                        自分の作業キューを中止
/citybuilder status
```
`plan load` の原点は、座標を省略すると**立っている足元のブロック(地表)**です。計画内の座標は原点からのオフセットです。
大きな作業は tick ごとに分割して実行されるので、サーバーは止まりません(`blocksPerTick`)。

### WebUI(都市計画)
`webui/citybuilder-planner.html` をブラウザで開きます。
1. 左のパレットから建物を選んで配置(R で回転)。道路(L)・区画(A)ツールで道路網と緑地を描く
2. 建物同士の重なり・道路との干渉は赤枠と警告で表示
3. 「city.json をダウンロード」→ `config/citybuilder/plans/<名前>.json` に置く
4. ゲーム内で `/citybuilder plan load <名前>`
5. 現地で見て、ズレた建物は GUI から置き直す・`undo` する、といった微調整

サンプル: `server-data/citybuilder/plans/city01.json`(大通り+マンション・オフィス街+駅・商業)。WebUI の「読み込み…」で開けます。

## 自作の建物を増やす(jar の更新は不要)
1. WorldEdit で建物を `//schem save 名前` で保存する
   - **建物の最小コーナー(北西・最下段)が原点、正面は南(+Z)向き**になるよう範囲を取る。1段目(y=0)を地面層にする
   - ファイル名は英数字・`_`・`-` のみ(日本語名は WebUI が `custom_1` などに読み替えます)
2. **WebUI に `.schem` をドラッグ&ドロップ**(複数可。「＋ 自作の建物を取り込む」ボタンでも可)
   - 寸法と真上から見た色を自動で読み取り、「自作」カテゴリに追加されます
   - 右側の「自作の建物」で、名前・カテゴリ・余白(外周の歩道の幅)を編集できます。内容はブラウザに保存されます
3. 同じ `.schem` を **`config/citybuilder/templates/` に置く**(これだけでゲーム内のメニューに出ます)
4. `/citybuilder reload`
5. ゲーム内の名前・カテゴリも WebUI と揃えたい場合は、WebUI の「catalog.json を書き出す」を `config/citybuilder/` に置いて `reload`(JSON は手書き不要)

`.schem` は Sponge Schematic v2/v3 に対応(古い `.schematic` は未対応)。`catalog.json` の `margin` は WebUI 用で、Mod 側は使いません。

## config.json
| 項目 | 既定 | 意味 |
|---|---|---|
| `permissionLevel` | 2 | 使用に必要なOPレベル。0 で全員。シングルプレイで「チートなし」の場合は 0 にしてください |
| `allowedPlayers` | [] | OP でなくても許可するプレイヤー名 |
| `requireCreative` | true | GUIからの配置にクリエイティブを要求 |
| `maxPlaceDistance` | 160 | プレイヤーから配置先までの最大距離 |
| `blocksPerTick` | 4000 | 1tickで処理するブロック数 |
| `cooldownTicks` | 10 | 連打防止 |
| `maxVolume` / `maxPendingBlocks` | 2,000,000 / 5,000,000 | 1回の最大体積 / キュー上限 |
| `historyDepth` | 10 | undo できる回数 |
| `roadClearAbove` / `roadFoundation` / `templateFoundation` / `foundationDepth` | 12 / true / true / 8 | 上方の整地・下の空洞埋め |
| `allowedDimensions` | ["minecraft:overworld"] | 建築を許すディメンション |
| `buildAreas` | [] | 空でなければ、この範囲内のみ建築可。`{"x1":0,"z1":0,"x2":999,"z2":999}` の配列 |
| `protectedAreas` | [] | 建築禁止範囲(同形式) |

## サーバー側の検証(クライアントを信用しない)
権限 / ゲームモード / 連打 / テンプレートID(カタログにあるものだけ) / 回転値 / ディメンション / 高さ範囲 / 体積 / 距離 / 建築可能範囲 / 保護区域 / キュー上限。
クライアントが送るのは「ID・位置・回転」だけで、ブロックの配置は常にサーバーが行います。

## 仕様と制限(正直なところ)
- **プレビューはブロックそのものではなく、半透明のボリューム+枠線です**。設計どおり、クライアントは建物のブロックデータを持たないためです。建物の見た目の確認は WebUI(真上のサムネイル)と、置いた後の `undo` で行います
- 道路の幅は**奇数**で左右対称になります(WebUIは奇数のみ)。歩道は両側に幅2。中央線は幅5以上
- 鉄道(レール)の敷設は未対応です。駅舎・ホームはテンプレートとして置けます
- 建物のブロックエンティティ(チェスト等)は復元しません(同梱の建物には含まれません)
- undo は変更前のブロック状態を戻します。サーバー再起動で履歴は消えます
- 建物の `.schem` は Minecraft 1.19.2(DataVersion 3120)のブロックで作られています

## テンプレートの再生成(任意)
```
cd tools && node gen_main.js     # server-data/citybuilder/ と tools/catalog.web.json を再生成
cd ../webui && python3 build.py  # WebUI にカタログを埋め込み直す
```
