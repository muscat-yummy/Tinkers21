[English](README.md) | **日本語**

# Tinkers' Construct for Minecraft 1.21.1

**Tinkers' Construct** を **Minecraft 1.21.1 / NeoForge** に対応させる非公式移植プロジェクトです。

このプロジェクトでは、オリジナルのTinkers' Constructが持つシステム、API、データ形式、アドオンとの互換性を可能な限り維持しながら、現在のMinecraft環境でTinkers' Constructを動作させることを目標としています。

> [!IMPORTANT]
> このプロジェクトは**非公式のコミュニティ移植版**です。
> オリジナルのTinkers' Construct開発者およびSlimeKnightsによって開発・保守されているものではありません。

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

## 開発状況

🚧 **開発中**

現在、この移植版は開発途中です。

すべての機能が正常に動作するとは限りません。

開発途中のビルドには、未実装の機能、クラッシュ、描画の問題、データの非互換性、その他の不具合が含まれている可能性があります。

大まかに以下の順序で開発を進めます。

1. ビルド環境とNeoForgeへの移行
2. Registryおよび基本システム
3. Material・Tool・Modifier
4. ツール作成とTinkers' Station
5. ツール・防具のレンダリング
6. Smeltery・Melting・Casting・Fluid
7. ワールド関連コンテンツと残りの機能
8. アドオン互換性

## アドオン互換性

既存アドオンとの互換性は、このプロジェクトにおける重要な目標の一つです。

可能な限りオリジナルのTinkers' Construct API構造を維持し、既存のアドオンをMinecraft 1.21.1へ移植する際に、完全な作り直しを必要としない環境を目指します。

ただし、開発中の段階ではAPIの変更が発生する可能性があり、互換性は保証されません。

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
