# リリース説明の方針

GitHub Releaseの説明は、日本語・英語の両方で、それぞれ変更点を1〜2行にまとめる。「軽微なバグを修正 / Fixed minor bugs」など、利用者に必要な内容だけを書く。必要なら両MODの更新案内とREADMEへのリンクを添える。

Releaseタイトルは `Tinkers21 <version>` のみにする。ダッシュ区切りの副題や、ノーブレークスペース・HTML空白文字参照による装飾は使わない。

公開添付はTinkers21・Mantle21のJARとセットZIPに限定する。SHA256はRelease本文に記載し、release-notes.md・provenance.json・SHA256SUMS.txtを別添付しない。ビルド元・CIの詳細はPORTING.mdとCI用の内部記録に残す。

公開用の短い説明は `concise/<version>.md` を使用する。詳細な原因・CI・GameTest・検証範囲はPORTING.mdに記録し、長い技術記録をRelease本文へ貼らない。既存の長い版別ファイルは過去の検証記録として保持する。
