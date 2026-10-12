[English](README.md) | **日本語**

# Tinkers21 — Minecraft 1.21.1 / NeoForge

**Tinkers' Construct** を **Minecraft 1.21.1 / NeoForge** に対応させる非公式移植プロジェクトです。

このプロジェクトでは、オリジナルのTinkers' Constructが持つシステム、API、データ形式、アドオンとの互換性を可能な限り維持しながら、現在のMinecraft環境でTinkers' Constructを動作させることを目標としています。

> [!IMPORTANT]
> このプロジェクトは**非公式のコミュニティ移植版**です。
> オリジナルのTinkers' Construct開発者およびSlimeKnightsによって開発・保守されているものではありません。
>
> この移植版は**バイブコード（AIを活用した開発）**で開発しています。記録された検証に成功していても、安全性や不具合がないことを保証するものではありません。
> **公式のTinkers' ConstructのMinecraft 1.21.1 / NeoForge版が公開されたら、安全性・信頼性・継続的な保守の観点から公式版へ乗り換えてください。** 移行前にワールドをバックアップし、移行互換性を確認してください。この非公式版の既存ワールドやアドオンデータを、そのまま引き継げることは保証していません。

## 目標

このプロジェクトの主な目的は、Tinkers' Constructの再実装や作り直しではなく、可能な限り忠実に移植することです。

可能な範囲で、以下の機能や仕組みを維持することを目標としています。

- 素材（Material）と素材ステータス
- ツールパーツとツール作成
- Tool Definition
- ModifierとModifier Hook
- Tinkers' Stationおよび関連するクラフトシステム
- SmelteryおよびFoundry
- 溶解・鋳造システム
- Fluid関連システム
- SlimeなどのTinkers' Construct独自コンテンツ
- 既存のResource/Data形式
- アドオンから利用されるTinkers' Construct API

Minecraft 1.20.1から1.21.1の間には、Minecraft本体およびMod LoaderのAPIに大きな変更があります。

そのため、アイテムデータ、ネットワーク、レンダリング、Registryなど、一部の内部実装については変更が避けられません。

APIの変更が必要になった場合でも、不必要な独自実装を追加するのではなく、既存のTinkers' Constructの設計やアドオンとの互換性を可能な限り維持することを優先します。

## 対象環境

| 項目 | バージョン |
| --- | --- |
| Minecraft | 1.21.1 |
| Mod Loader | NeoForge |
| Java | 21 |
| 移植元 Tinkers' Construct | 1.20.1 |

## 状態と導入方法

現在の実験的プレビューは **3.12.1-port.9** です。JEI不要のEMI単体対応を追加しました（検証済みEMI：1.1.24）。[GitHub Releases](https://github.com/muscat-yummy/Tinkers21/releases/tag/v3.12.1-port.9) から取得し、クライアント・サーバーの `mods/` に配置してください。Tinkers21 **3.12.1-port.9** とMantle21 **1.11-port.5** を使用します。Mantle21 1.11-port.5は既存のJARをそのまま使えます。Minecraft **1.21.1**、NeoForge **21.1.249以上の21.1系列**、**Java 21** が必要です。249で実クライアント描画とGameTest全28件、249・251の通常サーバーで同一の配布JARを検証しています。同じMODのJARは1個だけ置き、公式Tinkers' ConstructやMantleのJARと同時に導入しないでください。

Phase 1〜8の定義された範囲の検証が完了し、フルdatagen、両JARビルド、実Minecraft GameTest全28件（蛇口から鉢への鉄・金ブロック鋳造を含む）、実クライアントの厳格な描画・アドオン検証が成功しています。TiCEX本体・外部連携modの組み合わせ・一般的な旧ワールド移行は未検証のため、検証時には別ワールドかバックアップしたコピーを使用してください。

[リリースノート](docs/releases/3.12.1-port.9.md)、[CIの検証記録](PORTING.md)、[アドオン移植ガイド](docs/ADDON_PORTING.md) を参照してください。SHA-256はリリース本文に、ソースとCIの検証記録はPORTING.mdに記載しています。独立した検証用アドオンは配布JARから除外しています。

## アドオン互換性

既存アドオンとの互換性は、このプロジェクトにおける重要な目標の一つです。

可能な限りオリジナルのTinkers' Construct API構造を維持し、既存のアドオンをMinecraft 1.21.1へ移植する際に、完全な作り直しを必要としない環境を目指します。

ただし、開発中の段階ではAPIの変更が発生する可能性があり、互換性は保証されません。

アドオン移植のAPI差分と独立した検証用アドオンは [移植ガイド](docs/ADDON_PORTING.md) に記録しています。検証したコミット・Actions結果・残る制限は [PORTING.md](PORTING.md) を確認してください。

本体の移植が安定した後、既存のTinkers' Constructアドオンについても移植・対応を進める場合があります。

## コントリビューション

コードの提供、テスト、バグ報告、互換性修正などを歓迎します。

単にコンパイルを通すことを目的として、既存のTinkers' Constructのシステムを無関係な独自実装へ置き換えることは、可能な限り避けてください。

Minecraft 1.21.1またはNeoForgeの仕様上困難な場合を除き、既存の動作やAPIを維持することを優先してください。

大きな設計変更を行う場合は、既存の実装を維持できない理由を説明してください。

## Issueについて

不具合を報告する場合は、可能な範囲で以下の情報を記載してください。

- Minecraftのバージョン
- NeoForgeのバージョン
- この移植版のバージョンまたはCommit
- 導入しているMod
- 関連するログまたはクラッシュレポート
- 不具合を再現する手順

同じ問題がすでに報告されていないか確認してからIssueを作成してください。

## オリジナルプロジェクト

このプロジェクトは、**SlimeKnights**によるオリジナルの**Tinkers' Construct**をベースにしています。

オリジナルのTinkers' Constructおよびソースコードについては、GitHubの以下のリポジトリを参照してください。

`SlimeKnights/TinkersConstruct`

この非公式移植版固有の不具合について、オリジナルのTinkers' Construct開発者へ問い合わせたり、Issueを作成したりしないでください。

この移植版に関する問題は、このプロジェクトのIssueへ報告してください。

## ライセンス

このプロジェクトは**MIT License**のもとで公開されています。

このプロジェクトにはTinkers' Constructを基にしたコードが含まれています。

オリジナルの著作権表示およびライセンス情報は、MIT Licenseの条件に従って維持されます。

詳細については`LICENSE`を参照してください。

## 免責事項

このプロジェクトは独立した非公式移植プロジェクトです。

Mojang Studios、Microsoft、SlimeKnights、およびオリジナルのTinkers' Construct開発者によって公式に提供、承認、または運営されているものではありません。

## バックストーリー

友達とTinCoをしたかったけど、1.21.1にはないことに気づいたから、規約とかそこら辺全部AIに任せることにして出来上がった変な作品
